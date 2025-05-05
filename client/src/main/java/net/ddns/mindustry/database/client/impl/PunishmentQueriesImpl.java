package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.tables.pojos.*;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static net.ddns.mindustry.database.schema.Tables.*;

@NullMarked
public record PunishmentQueriesImpl(DSLContext dsl, AccountQueriesImpl account) implements PunishmentQueries {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Optional<Ban> findBan(DSLContext tDsl, long uuid) {
        return tDsl.selectFrom(BAN)
                .where(BAN.UUID.eq(uuid))
                .fetchOptionalInto(Ban.class);
    }

    @Override
    public List<Ban> activeBans(Account account) {
        final var now = OffsetDateTime.now();
        return dsl.select()
                .from(BAN)
                .leftAntiJoin(UNBAN).on(UNBAN.BAN_ID.eq(BAN.ID))
                .where(BAN.EXPIRATION_DATE.isNull().or(BAN.EXPIRATION_DATE.greaterThan(now)))
                .fetchInto(Ban.class);
    }

    @Override
    public Optional<Ban> findBan(long uuid) {
        return findBan(dsl, uuid);
    }

    @Override
    public Optional<Ban> findBan(int id) {
        return dsl.selectFrom(BAN)
                .where(BAN.ID.eq(id))
                .fetchOptionalInto(Ban.class);
    }

    @Override
    public Optional<Kick> findKick(int id) {
        return dsl.selectFrom(KICK)
                .where(KICK.ID.eq(id))
                .fetchOptionalInto(Kick.class);
    }

    @Override
    public Optional<Warn> findWarn(int id) {
        return dsl.selectFrom(WARN)
                .where(WARN.ID.eq(id))
                .fetchOptionalInto(Warn.class);
    }

    @Override
    public Status<Ban> ban(String punishedUsername, String staffUsername, String reason, Server server, @Nullable OffsetDateTime expiration) {

        Objects.requireNonNull(punishedUsername);
        Objects.requireNonNull(staffUsername);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);
        // Expiration can be nullable.

        return dsl.transactionResult(ctx -> {
            final DSLContext tDsl = ctx.dsl();

            final Account punished = account.find(tDsl, punishedUsername).orElse(null);
            if (punished == null) return new Status.PunishedNotFound<>();

            final Account staff = account.find(tDsl, staffUsername).orElse(null);
            if (staff == null) return new Status.StaffNotFound<>();

            // I generate an unique uuid.
            long randomUuid = RANDOM.nextLong();
            while (findBan(tDsl, randomUuid).isPresent()) randomUuid = RANDOM.nextLong();

            final Ban ban = tDsl.insertInto(BAN)
                    .set(BAN.ACCOUNT_ID,      punished.id())
                    .set(BAN.STAFF_ID,        staff.id())
                    .set(BAN.REASON,          reason)
                    .set(BAN.SERVER_ID,       server.id())
                    .set(BAN.UUID,            randomUuid)
                    .set(BAN.EXPIRATION_DATE, expiration)
                    .returningResult(BAN)
                    .fetchOptionalInto(Ban.class)
                    .orElseThrow();
            return new PunishmentQueries.Status.Ok<>(ban);
        });
    }

    @Override
    public Status<Ban> ban(String punishedUsername, String staffUsername, String reason, Server server, Duration duration) {
        Objects.requireNonNull(duration);
        return ban(punishedUsername, staffUsername, reason, server, OffsetDateTime.now().plus(duration));
    }

    @Override
    public Status<Kick> kick(String punishedUsername, String staffUsername, String reason, Server server) {

        Objects.requireNonNull(punishedUsername);
        Objects.requireNonNull(staffUsername);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);

        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            final Account punished = account.find(tDsl, punishedUsername).orElse(null);
            if (punished == null) return new Status.PunishedNotFound<>();

            final Account staff = account.find(tDsl, staffUsername).orElse(null);
            if (staff == null) return new Status.StaffNotFound<>();

            final Kick kick = tDsl.insertInto(KICK)
                    .set(KICK.ACCOUNT_ID, punished.id())
                    .set(KICK.STAFF_ID,   staff.id())
                    .set(KICK.REASON,     reason)
                    .set(KICK.SERVER_ID,  server.id())
                    .returningResult(KICK)
                    .fetchOptionalInto(Kick.class)
                    .orElseThrow();
            return new Status.Ok<>(kick);
        });
    }

    @Override
    public Status<Warn> warn(String punishedUsername, String staffUsername, String reason, Server server) {

        Objects.requireNonNull(punishedUsername);
        Objects.requireNonNull(staffUsername);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);

        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            final Account punished = account.find(tDsl, punishedUsername).orElse(null);
            if (punished == null) return new Status.PunishedNotFound<>();

            final Account staff = account.find(tDsl, staffUsername).orElse(null);
            if (staff == null) return new Status.StaffNotFound<>();

            final Warn warn = tDsl.insertInto(WARN)
                    .set(WARN.ACCOUNT_ID, punished.id())
                    .set(WARN.STAFF_ID,   staff.id())
                    .set(WARN.REASON,     reason)
                    .set(WARN.SERVER_ID,  server.id())
                    .returningResult(WARN)
                    .fetchOptionalInto(Warn.class)
                    .orElseThrow();
            return new Status.Ok<>(warn);
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
