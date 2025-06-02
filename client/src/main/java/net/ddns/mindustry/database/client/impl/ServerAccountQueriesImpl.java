package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.ServerAccountQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.OnlineAccount;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.OptionalInt;
import static net.ddns.mindustry.database.schema.Tables.*;

public record ServerAccountQueriesImpl(DatabaseImpl database) implements ServerAccountQueries {

    public boolean isAccountWhitelisted(DSLContext tDsl, int accountId, int serverId) {
        return tDsl.selectCount()
                .from(SERVER_WHITELIST)
                .where(SERVER_WHITELIST.ACCOUNT_ID.eq(accountId).and(SERVER_WHITELIST.SERVER_ID.eq(serverId)))
                .fetchOptionalInto(Integer.class)
                .map(count -> count == 1)
                .orElse(false);
    }

    @Override
    public JoinStatus joinsServer(Server server, String displayName, String ip, String uuid) throws DataAccessException {

        Objects.requireNonNull(server);
        Objects.requireNonNull(displayName);
        final byte[] session = database().account()
                .createSessionHash(ip, uuid);

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            // The session is not present or is expired.
            final OptionalInt oAccountId = database().account().sessionAccountId(tDsl, session);
            if (oAccountId.isEmpty()) return new JoinStatus.NotAuthenticated();
            final int accountId = oAccountId.orElseThrow();

            // I don't check the value in the server instance to avoid reading old server data.
            final boolean whitelistEnabled = database().server().isWhitelistEnabled(tDsl, server.id());
            if (whitelistEnabled && !isAccountWhitelisted(tDsl, accountId, server.id()))
                return new JoinStatus.NotWhitelisted();

            // I try to insert in the online list, if it collides, it means he is already playing.
            final boolean inserted = tDsl.insertInto(ONLINE_ACCOUNT)
                    .set(ONLINE_ACCOUNT.ACCOUNT_ID, accountId)
                    .set(ONLINE_ACCOUNT.SERVER_ID, server.id())
                    .set(ONLINE_ACCOUNT.DISPLAY_NAME, displayName)
                    .onConflictDoNothing()
                    .execute() == 1;
            return inserted ?
                    new JoinStatus.Joined(database().account().find(tDsl, accountId).orElseThrow()) :
                    new JoinStatus.AlreadyInServer();
        });
    }

    @Override
    public boolean leavesServer(Account account, Server server) throws DataAccessException {

        Objects.requireNonNull(account);
        Objects.requireNonNull(server);

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            // I remove the account from the online table.
            final var online = tDsl.deleteFrom(ONLINE_ACCOUNT)
                    .where(ONLINE_ACCOUNT.ACCOUNT_ID.eq(account.id()).and(ONLINE_ACCOUNT.SERVER_ID.eq(server.id())))
                    .returningResult(ONLINE_ACCOUNT)
                    .fetchOptionalInto(OnlineAccount.class)
                    .orElse(null);
            // In case the account provided was not online.
            if (online == null) return false;

            // I insert the player inside the server account history.
            tDsl.insertInto(SERVER_ACCOUNT_HISTORY)
                    .set(SERVER_ACCOUNT_HISTORY.DISPLAY_NAME, online.displayName())
                    .set(SERVER_ACCOUNT_HISTORY.ACCOUNT_ID, online.accountId())
                    .set(SERVER_ACCOUNT_HISTORY.SERVER_ID, online.serverId())
                    .set(SERVER_ACCOUNT_HISTORY.JOIN_DATE, online.joinDate())
                    .set(SERVER_ACCOUNT_HISTORY.LEAVE_DATE, OffsetDateTime.now())
                    .execute();
            return true;
        });
    }

    @Override
    public boolean isAccountWhitelisted(Account account, Server server) {
        return isAccountWhitelisted(database().dsl(), account.id(), server.id());
    }

    @Override
    public void whitelistAccount(Account account, Server server) {
        database().dsl()
                .insertInto(SERVER_WHITELIST)
                .set(SERVER_WHITELIST.ACCOUNT_ID, account.id())
                .set(SERVER_WHITELIST.SERVER_ID, server.id())
                .onConflictDoNothing()
                .execute();
    }

    @Override
    public void removeAccountWhitelist(Account account, Server server) {
        database().dsl()
                .deleteFrom(SERVER_WHITELIST)
                .where(SERVER_WHITELIST.ACCOUNT_ID.eq(account.id()).and(SERVER_WHITELIST.SERVER_ID.eq(server.id())))
                .execute();
    }
}
