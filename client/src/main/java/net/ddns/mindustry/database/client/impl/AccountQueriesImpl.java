package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.client.SecurityConfig;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import static net.ddns.mindustry.database.schema.Tables.*;

@NullMarked
public record AccountQueriesImpl(DSLContext dsl, SecurityConfig security) implements AccountQueries {

    private byte[] createSessionHash(String ip, String uuid) {
        Objects.requireNonNull(ip);
        Objects.requireNonNull(uuid);
        return security().sessionHash().digest((ip + uuid).getBytes(StandardCharsets.UTF_8));
    }

    /// @return the account id, if the session is present and not expired, or else, the empty optional.
    private OptionalInt sessionAccountId(DSLContext tDsl, byte[] session) {
        return tDsl.select(ACCOUNT_SESSION.ACCOUNT_ID, ACCOUNT_SESSION.EXPIRATION_DATE)
                .from(ACCOUNT_SESSION)
                .where(ACCOUNT_SESSION.SESSION_COOKIE.eq(session))
                .fetchStream()
                .limit(1) // Always only one element.
                .flatMapToInt(result -> {
                    final OffsetDateTime expiration = result.get(ACCOUNT_SESSION.EXPIRATION_DATE);
                    if (expiration.isBefore(OffsetDateTime.now())) return null; // The session expired.
                    return IntStream.of(result.get(ACCOUNT_SESSION.ACCOUNT_ID));
                }).findFirst();
    }

    /// Creates a new session if not present, else updates the previous one with the new information.
    private void updateSession(DSLContext tDsl, int accountId, byte[] session, int durationHours) throws DataAccessException {

        final var expiration = OffsetDateTime.now().plusHours(durationHours);
        tDsl.insertInto(ACCOUNT_SESSION)
                .set(ACCOUNT_SESSION.ACCOUNT_ID, accountId)
                .set(ACCOUNT_SESSION.SESSION_COOKIE, session)
                .set(ACCOUNT_SESSION.EXPIRATION_DATE, expiration)
                .onConflict()
                .doUpdate()
                .set(ACCOUNT_SESSION.SESSION_COOKIE, session)
                .set(ACCOUNT_SESSION.EXPIRATION_DATE, expiration)
                .execute();
    }

    public Optional<Account> find(DSLContext tDsl, String username) {
        Objects.requireNonNull(username);
        return tDsl.selectFrom(ACCOUNT)
                .where(ACCOUNT.USERNAME.eq(username.strip().toLowerCase()))
                .fetchOptionalInto(Account.class);
    }

    public Optional<Account> find(DSLContext tDsl, int id) {
        return tDsl.selectFrom(ACCOUNT)
                .where(ACCOUNT.ID.eq(id))
                .fetchOptionalInto(Account.class);
    }

    @Override
    public boolean isUsernameValid(@Nullable String username) {

        if (username == null) return false;

        // Check 1: Length must be greater than 2
        if (username.length() <= 2) return false;

        // Check 2: The username must match the regex pattern "^[a-z0-9_.]+$"
        // This ensures that only lowercase letters, digits, underscores, and dots are present.
        if (!username.matches("^[a-z0-9_.]+$")) return false;

        // Check 3: The username must not contain two consecutive dots ("..")
        return !username.contains("..");
    }

    @Override
    public Optional<Account> find(String username) {
        return find(dsl, username);
    }

    @Override
    public Optional<Account> find(String ip, String uuid) {

        Objects.requireNonNull(ip);
        Objects.requireNonNull(uuid);

        return sessionAccountId(dsl, createSessionHash(ip, uuid))
                .stream()
                .mapToObj(accountId -> find(dsl, accountId).orElse(null))
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    public LoginStatus login(String username, char[] password, String ip, String uuid, int durationHours) {

        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        if (durationHours <= 0) throw new IllegalArgumentException("The hours cannot be negative or 0.");

        final byte[] session = createSessionHash(ip, uuid);

        try {
            return dsl.transactionResult(ctx -> {

                final DSLContext tDsl = ctx.dsl();
                // I return if the user is already authenticated.
                if (sessionAccountId(tDsl, session).isPresent()) return new LoginStatus.AlreadyLoggedIn();

                // Computationally expensive hash.
                // I hash and update it on the database, because if the password is correct,
                // I want to update it using the latest security configuration.
                // This is not done outside the transaction because if a user is already authenticated, I want to be fast.
                // I also don't do it after the verifying to take the same amount of time in case the username is not valid.
                final byte[] hashedPassword = security().hashPass(password).getBytes(StandardCharsets.UTF_8);

                final Account account = find(tDsl, username).orElse(null);
                if (account == null) {
                    security().hashPass(password); // Wasteful operation to avoid username scanning.
                    return new LoginStatus.WrongCredentials();
                }

                final String dbHash = new String(account.password(), StandardCharsets.UTF_8);
                if (!security().passHash().verify(dbHash, password)) return new LoginStatus.WrongCredentials();

                tDsl.update(ACCOUNT)
                        // I update the password with the latest argon2 configuration.
                        .set(ACCOUNT.PASSWORD, hashedPassword)
                        .where(ACCOUNT.ID.eq(account.id()))
                        .execute();

                // I create a new session, or update the previous one for the user.
                updateSession(tDsl, account.id(), session, durationHours);

                return new LoginStatus.LoggedIn(account);
            });
        } finally {
            security().passHash().wipeArray(password);
        }
    }

    @Override
    public void logout(Account account) {
        Objects.requireNonNull(account);
        dsl.deleteFrom(ACCOUNT_SESSION)
                .where(ACCOUNT_SESSION.ACCOUNT_ID.eq(account.id()))
                .execute();
    }

    @Override
    public SignupStatus signup(String username, char[] password, String ip, String uuid) {

        if (!isUsernameValid(Objects.requireNonNull(username))) return new SignupStatus.InvalidName();
        Objects.requireNonNull(password);

        // I do this here to make the signup operation slow in every case.
        final byte[] hashedPass = security.hashPass(password).getBytes(StandardCharsets.UTF_8);

        // TODO Account checking.

        try {
            final Account account = dsl.insertInto(ACCOUNT)
                    .set(ACCOUNT.USERNAME, username)
                    .set(ACCOUNT.PASSWORD, hashedPass)
                    .onConflict(ACCOUNT.USERNAME)
                    .doNothing()
                    .returningResult(ACCOUNT)
                    .fetchOneInto(Account.class);

            return account == null ? new SignupStatus.UsernameInUse() : new SignupStatus.Created(account);
        } finally {
            security.passHash().wipeArray(password);
        }
    }

    @Override
    public JoinStatus joinsServer(Server server, String displayName, String ip, String uuid) throws DataAccessException {

        Objects.requireNonNull(server);
        Objects.requireNonNull(displayName);
        final byte[] session = createSessionHash(ip, uuid);

        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            // The session is not present or is expired.
            final OptionalInt oAccountId = sessionAccountId(tDsl, session);
            if (oAccountId.isEmpty()) return new JoinStatus.NotAuthenticated();
            final int accountId = oAccountId.orElseThrow();

            // I check if the account is already inside a server.
            if (tDsl.selectOne()
                    .from(SERVER_JOIN)
                    .where(SERVER_JOIN.ACCOUNT_ID.eq(accountId).and(SERVER_JOIN.LEAVE_DATE.isNull()))
                    .fetchOptional()
                    .isPresent()) return new JoinStatus.AlreadyInServer();

            tDsl.insertInto(SERVER_JOIN)
                    .set(SERVER_JOIN.DISPLAY_NAME, displayName)
                    .set(SERVER_JOIN.ACCOUNT_ID, accountId)
                    .set(SERVER_JOIN.SERVER_ID, server.id())
                    .execute();

            // TODO Server authorization.

            return new JoinStatus.Joined(find(tDsl, accountId).orElseThrow());
        });
    }

    @Override
    public void leavesServer(Account account) throws DataAccessException {
        Objects.requireNonNull(account);
        dsl.update(SERVER_JOIN)
                .set(SERVER_JOIN.LEAVE_DATE, OffsetDateTime.now())
                .where(SERVER_JOIN.ACCOUNT_ID.eq(account.id()).and(SERVER_JOIN.LEAVE_DATE.isNull()))
                .execute();
    }

    @Override
    public PasswordUpdateStatus updatePassword(Account account, char[] oldPassword, char[] newPassword) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(newPassword);

        final var dbPassword = new String(account.password(), StandardCharsets.UTF_8);

        try {

            // I do this here to take the same amount of time if the old password is wrong.
            final byte[] hashed = security().hashPass(newPassword).getBytes(StandardCharsets.UTF_8);
            if (!security().passHash().verify(dbPassword, oldPassword)) return new PasswordUpdateStatus.WrongPassword();

            dsl.update(ACCOUNT)
                    .set(ACCOUNT.PASSWORD, hashed)
                    .where(ACCOUNT.ID.eq(account.id()))
                    .execute();

            return new PasswordUpdateStatus.Updated();
        } finally {
            security.passHash().wipeArray(oldPassword);
            security.passHash().wipeArray(newPassword);
        }
    }
}
