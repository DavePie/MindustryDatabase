package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Ban;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.DSLContext;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import static net.ddns.mindustry.database.schema.Tables.*;

public record PunishmentQueriesImpl(DSLContext dsl, AccountQueriesImpl account) implements PunishmentQueries {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Optional<Ban> findBan(DSLContext tDsl, long uuid) {
        return tDsl.selectFrom(BAN)
                .where(BAN.UUID.eq(uuid))
                .fetchOptionalInto(Ban.class);
    }

    @Override
    public Optional<Ban> findBan(long uuid) {
        return findBan(dsl, uuid);
    }

    @Override
    public Status ban(String punishedUsername, String staffUsername, String reason, Server server, OffsetDateTime expiration) {

        Objects.requireNonNull(punishedUsername);
        Objects.requireNonNull(staffUsername);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);
        // Expiration can be nullable.

        return dsl.transactionResult(ctx -> {
            final DSLContext tDsl = ctx.dsl();

            final Account punished = account.find(tDsl, punishedUsername).orElse(null);
            if (punished == null) return Status.USERNAME_NOT_FOUND;

            final Account staff = account.find(tDsl, staffUsername).orElse(null);
            if (staff == null) return Status.STAFF_NOT_FOUND;

            // I generate an unique uuid.
            long randomUuid = RANDOM.nextLong();
            while (findBan(tDsl, randomUuid).isPresent()) randomUuid = RANDOM.nextLong();

            tDsl.insertInto(BAN)
                    .set(BAN.ACCOUNT_ID,      punished.id())
                    .set(BAN.STAFF_ID,        staff.id())
                    .set(BAN.REASON,          reason)
                    .set(BAN.SERVER_ID,       server.id())
                    .set(BAN.UUID,            randomUuid)
                    .set(BAN.EXPIRATION_DATE, expiration)
                    .execute();

            return Status.OK;
        });
    }

    @Override
    public Status kick(String punishedUsername, String staffUsername, String reason, Server server) {
        Objects.requireNonNull(punishedUsername);
        Objects.requireNonNull(staffUsername);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);
        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            final Account punished = account.find(tDsl, punishedUsername).orElse(null);
            if (punished == null) return Status.USERNAME_NOT_FOUND;

            final Account staff = account.find(tDsl, staffUsername).orElse(null);
            if (staff == null) return Status.STAFF_NOT_FOUND;

            tDsl.insertInto(KICK)
                    .set(KICK.ACCOUNT_ID, punished.id())
                    .set(KICK.STAFF_ID,   staff.id())
                    .set(KICK.REASON,     reason)
                    .set(KICK.SERVER_ID,  server.id())
                    .execute();

            return Status.OK;
        });
    }

    @Override
    public Status warn(String punishedUsername, String staffUsername, String reason, Server server) {
        Objects.requireNonNull(punishedUsername);
        Objects.requireNonNull(staffUsername);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);
        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            final Account punished = account.find(tDsl, punishedUsername).orElse(null);
            if (punished == null) return Status.USERNAME_NOT_FOUND;

            final Account staff = account.find(tDsl, staffUsername).orElse(null);
            if (staff == null) return Status.STAFF_NOT_FOUND;

            tDsl.insertInto(WARN)
                    .set(WARN.ACCOUNT_ID, punished.id())
                    .set(WARN.STAFF_ID,   staff.id())
                    .set(WARN.REASON,     reason)
                    .set(WARN.SERVER_ID,  server.id())
                    .execute();

            return Status.OK;
        });
    }

    @Override
    public UnbanStatus unban(Ban ban, String staffUsername) {
        Objects.requireNonNull(ban);
        Objects.requireNonNull(staffUsername);
        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            final boolean isUnbanned = tDsl.selectOne()
                    .where(UNBAN.BAN_ID.eq(ban.id()))
                    .fetchOptional()
                    .isPresent();

            if (isUnbanned) return UnbanStatus.ALREADY_UNBANNED;

            final Account staff = account.find(tDsl, staffUsername).orElse(null);
            if (staff == null) return UnbanStatus.STAFF_NOT_FOUND;

            dsl.insertInto(UNBAN)
                    .set(UNBAN.BAN_ID, ban.id())
                    .set(UNBAN.STAFF_ID, staff.id())
                    .execute();

            return UnbanStatus.OK;
        });
    }
}
