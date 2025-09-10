package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Inet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static net.ddns.mindustry.database.client.impl.DatabaseImpl.inet;
import static net.ddns.mindustry.database.schema.Tables.*;

@NullMarked
public record AccountQueriesImpl(DatabaseImpl database, MessageDigest digest) implements AccountQueries {

    public AccountQueriesImpl(DatabaseImpl database) {
        this(database, newDigest(database));
    }

    private static MessageDigest newDigest(DatabaseImpl database) {

        final String algorithm = database
                .securityConfig()
                .hashAlgorithm();

        try { return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid algorithm: " + algorithm, e);
        }
    }

    /// Adds a new Login entry and creates a new session if not present,
    /// else updates the previous one with the new information.
    private void authenticate(DSLContext tDsl, int accountId, Inet ip, byte[] session, Duration duration) throws DataAccessException {

        // I insert the account login.
        tDsl.insertInto(LOGIN)
                .set(LOGIN.ACCOUNT_ID, accountId)
                .set(LOGIN.IP_ADDRESS, ip)
                .execute();

        // I update the session cookie to allow the user to not authenticate everytime they join.
        final var expiration = OffsetDateTime.now().plus(duration);
        tDsl.insertInto(ACCOUNT_SESSION)
                .set(ACCOUNT_SESSION.ACCOUNT_ID, accountId)
                .set(ACCOUNT_SESSION.SESSION_COOKIE, session)
                .set(ACCOUNT_SESSION.EXPIRATION_DATE, expiration)
                .onConflict(ACCOUNT_SESSION.SESSION_COOKIE)
                .doUpdate()
                .set(ACCOUNT_SESSION.SESSION_COOKIE, session)
                .set(ACCOUNT_SESSION.EXPIRATION_DATE, expiration)
                .execute();
    }

    private <T> T trackAccountsFromInet(DSLContext tDsl, Inet ip, BiFunction<Table<?>, WithStep, T> query) {

        Objects.requireNonNull(tDsl);
        Objects.requireNonNull(ip);
        Objects.requireNonNull(query);

        // I create the cte table that will hold the recursive data.
        final var cteTable = DSL.name("account_tree")
                .fields(LOGIN.ACCOUNT_ID.getName(), LOGIN.IP_ADDRESS.getName())
                .as(tDsl.select(LOGIN.ACCOUNT_ID, LOGIN.IP_ADDRESS));
        // Commodity fields of the cte table, used for joins.
        final var cteAccount = Objects.requireNonNull(cteTable.field(LOGIN.ACCOUNT_ID));
        final var cteAddress = Objects.requireNonNull(cteTable.field(LOGIN.IP_ADDRESS));

        // The base query that selects the first batch of data used for the recursion.
        // The data will be retrieved by using the ip address provided.
        final var baseQuery = tDsl.select(LOGIN.ACCOUNT_ID, LOGIN.IP_ADDRESS)
                .from(LOGIN)
                .where(LOGIN.IP_ADDRESS.eq(ip));
        // The recursive query that will find account/ip links until no new record is found.
        final var recursiveQuery = tDsl.select(LOGIN.ACCOUNT_ID, LOGIN.IP_ADDRESS)
                .from(LOGIN)
                .innerJoin(cteTable)
                .on(LOGIN.ACCOUNT_ID.eq(cteAccount).or(LOGIN.IP_ADDRESS.eq(cteAddress)))
                .where(LOGIN.ACCOUNT_ID.notEqual(cteAccount).or(LOGIN.IP_ADDRESS.notEqual(cteAddress)));
        // I apply the recursive cte, then the function will select the wanted data.
        return query.apply(cteTable, tDsl.withRecursive(cteTable.getName())
                .as(baseQuery.union(recursiveQuery)));
    }

    private Set<Account> accountsFromIp(DSLContext tDsl, Inet ip) {
        return trackAccountsFromInet(tDsl, ip, (cteTable, query) -> {
            final var cteAccount = Objects.requireNonNull(cteTable.field(LOGIN.ACCOUNT_ID));
            return query.select(ACCOUNT)
                    .distinctOn(cteAccount)
                    .from(cteTable)
                    .innerJoin(ACCOUNT)
                    .on(ACCOUNT.ID.eq(cteAccount))
                    .fetchStreamInto(Account.class)
                    .collect(Collectors.toSet());
        });
    }

    private int countAccountsFromIp(DSLContext tDsl, Inet userIp) {
        return trackAccountsFromInet(tDsl, userIp, (cteTable, query) -> {
            final var cteAccount = Objects.requireNonNull(cteTable.field(LOGIN.ACCOUNT_ID));
            return query.select(DSL.countDistinct(cteAccount))
                    .from(cteTable)
                    .fetchOptionalInto(Integer.class)
                    .orElseThrow(IllegalStateException::new);
        });
    }

    private LoginStatus login(DSLContext tDsl, String username, char[] password, Inet ip, byte[] session, Duration duration) {

        final var security = database().securityConfig();

        // Computationally expensive hash.
        // I hash and update it on the database, because if the password is correct,
        // I want to update it using the latest security configuration.
        // This is not done outside the transaction because if a user is already authenticated, I want to be fast.
        // I also don't do it after the verifying to take the same amount of time in case the username is not valid.
        final byte[] hashedPassword = security.hashPass(password).getBytes(StandardCharsets.UTF_8);

        final Account account = find(tDsl, username).orElse(null);
        if (account == null) {
            security.hashPass(password); // Wasteful operation to avoid username scanning.
            return new LoginStatus.WrongCredentials();
        }

        final String dbHash = new String(account.password(), StandardCharsets.UTF_8);
        if (!security.argon2().verify(dbHash, password)) return new LoginStatus.WrongCredentials();

        tDsl.update(ACCOUNT)
                // I update the password with the latest argon2 configuration.
                .set(ACCOUNT.PASSWORD, hashedPassword)
                .where(ACCOUNT.ID.eq(account.id()))
                .execute();

        // I create a new session, or update the previous one for the user.
        authenticate(tDsl, account.id(), ip, session, duration);

        return new LoginStatus.LoggedIn(account);
    }

    private SignupStatus signup(DSLContext tDsl, String username, byte[] password) {
        return tDsl.insertInto(ACCOUNT)
                .set(ACCOUNT.USERNAME, username)
                .set(ACCOUNT.PASSWORD, password)
                .onConflict(ACCOUNT.USERNAME)
                .doNothing()
                .returningResult(ACCOUNT)
                .fetchOptionalInto(Account.class)
                .map(SignupStatus.Created::new)
                // I do this to be able to provide a different class in the orElse.
                .map(status -> (SignupStatus) status)
                // The row has not been added, meaning the query when on conflict, meaning the username already exists.
                .orElseGet(() -> new SignupStatus.UsernameInUse(username));
    }

    public byte[] createSessionHash(String ip, String uuid) {
        Objects.requireNonNull(ip);
        Objects.requireNonNull(uuid);
        // The Message digest cannot be re-used within different threads.
        final MessageDigest digest = this.digest;
        synchronized (digest) {
            return digest.digest((ip + uuid).getBytes(StandardCharsets.UTF_8));
        }
    }

    /// @return the account id, if the session is present and not expired, or else, the empty optional.
    public OptionalInt sessionAccountId(DSLContext tDsl, byte[] session) {
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
        return find(database().dsl(), username);
    }

    @Override
    public Optional<Account> find(String ip, String uuid) {

        Objects.requireNonNull(ip);
        Objects.requireNonNull(uuid);

        return database().dsl().transactionResult(ctx -> {
            final DSLContext tDsl = ctx.dsl();
            return sessionAccountId(tDsl, createSessionHash(ip, uuid))
                    .stream()
                    .mapToObj(accountId -> find(tDsl, accountId).orElse(null))
                    .filter(Objects::nonNull)
                    .findFirst();
        });
    }

    @Override
    public Set<Account> findAccounts(String ip) {
        final Inet inet = inet(ip);
        return database().dsl().transactionResult(ctx -> accountsFromIp(ctx.dsl(), inet));
    }

    @Override
    public LoginStatus login(String username, char[] password, String ip, String uuid, Duration sessionDuration) {

        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        Objects.requireNonNull(sessionDuration);

        try {
            final byte[] session = createSessionHash(ip, uuid);
            final Inet inet = inet(ip);
            return database().dsl().transactionResult(ctx -> login(ctx.dsl(), username, password, inet, session, sessionDuration));
        } finally {
            database().securityConfig()
                    .argon2()
                    .wipeArray(password);
        }
    }

    @Override
    public void logout(Account account) {
        Objects.requireNonNull(account);
        database().dsl()
                .deleteFrom(ACCOUNT_SESSION)
                .where(ACCOUNT_SESSION.ACCOUNT_ID.eq(account.id()))
                .execute();
    }

    @Override
    public SignupStatus signup(String username, char[] password, String ip, String uuid, Duration sessionDuration) {

        if (!isUsernameValid(Objects.requireNonNull(username))) return new SignupStatus.InvalidUsername(username);
        Objects.requireNonNull(password);

        final var security = database().securityConfig();
        final Inet inet = inet(ip);
        final byte[] session = createSessionHash(ip, uuid);
        // I do this here to make the signup operation slow in every case.
        final byte[] hashedPass = security
                .hashPass(password)
                .getBytes(StandardCharsets.UTF_8);

        // The password is too short.
        if (password.length < security.minimumPasswordLength()) return new SignupStatus.InvalidPassword();

        try {
            return database().dsl().transactionResult(ctx -> {

                final DSLContext tDsl = ctx.dsl();
                final int accounts = countAccountsFromIp(tDsl, inet);
                if (security.accountLimit() <= accounts) return new SignupStatus.LimitReached(security.accountLimit());

                final SignupStatus status = signup(tDsl, username, hashedPass);
                if (status instanceof SignupStatus.Created(Account account)) {
                    authenticate(tDsl, account.id(), inet, session, sessionDuration);
                }
                return status;
            });
        } finally {
            security.argon2().wipeArray(password);
        }
    }

    @Override
    public PasswordUpdateStatus updatePassword(Account account, char[] oldPassword, char[] newPassword) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(newPassword);

        final var security = database().securityConfig();
        final var dbPassword = new String(account.password(), StandardCharsets.UTF_8);

        try {

            // I do this here to take the same amount of time if the old password is wrong.
            final byte[] hashed = security
                    .hashPass(newPassword)
                    .getBytes(StandardCharsets.UTF_8);

            if (!security.argon2().verify(dbPassword, oldPassword)) return new PasswordUpdateStatus.WrongPassword();

            database().dsl().update(ACCOUNT)
                    .set(ACCOUNT.PASSWORD, hashed)
                    .where(ACCOUNT.ID.eq(account.id()))
                    .execute();

            return new PasswordUpdateStatus.Updated();
        } finally {
            security.argon2().wipeArray(oldPassword);
            security.argon2().wipeArray(newPassword);
        }
    }
}
