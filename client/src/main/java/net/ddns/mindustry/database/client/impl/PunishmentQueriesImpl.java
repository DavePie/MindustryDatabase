package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.enums.PunishmentIssuerType;
import net.ddns.mindustry.database.schema.enums.PunishmentType;
import net.ddns.mindustry.database.schema.tables.pojos.*;
import org.jooq.DSLContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import static net.ddns.mindustry.database.schema.Tables.*;

@NullMarked
public record PunishmentQueriesImpl(DatabaseImpl database) implements PunishmentQueries {

    /// Requires a transaction.
    private PunishmentIssuer retrieveIssuer(DSLContext tDsl, Issuer issuer) {

        Objects.requireNonNull(tDsl);
        Objects.requireNonNull(issuer);

        // I try to insert the issuer, in case it is already inserted, I do nothing and select it.
        final var insert = tDsl.insertInto(PUNISHMENT_ISSUER);
        final var oIssuer = (switch (issuer) {

            case Issuer.Console(var server) -> insert.set(PUNISHMENT_ISSUER.TYPE, PunishmentIssuerType.server)
                    .set(PUNISHMENT_ISSUER.SERVER_ID, server.id());

            case Issuer.Player (var user  ) -> insert.set(PUNISHMENT_ISSUER.TYPE, PunishmentIssuerType.account)
                    .set(PUNISHMENT_ISSUER.ACCOUNT_ID, user.id());

        }).onConflictDoNothing()
                .returningResult(PUNISHMENT_ISSUER)
                .fetchOptionalInto(PunishmentIssuer.class);
        // The issuer has been inserted, so I return the inserted value.
        if (oIssuer.isPresent()) return oIssuer.orElseThrow();

        // The issuer already exists, so I select it.
        final var select = tDsl.selectFrom(PUNISHMENT_ISSUER);
        return (switch (issuer) {
            case Issuer.Console(var server) -> select.where(PUNISHMENT_ISSUER.SERVER_ID.eq(server.id()));
            case Issuer.Player (var user  ) -> select.where(PUNISHMENT_ISSUER.ACCOUNT_ID.eq(user.id()));
        }).fetchOptionalInto(PunishmentIssuer.class).orElseThrow(); // Always present.
    }

    private Ban banQuery(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime creation, @Nullable OffsetDateTime expiration) {

        Objects.requireNonNull(punished);
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);
        Objects.requireNonNull(creation);
        if (expiration != null && expiration.isBefore(creation)) throw new IllegalArgumentException("The expiration date is before the creation date.");

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            final Integer issuerId = retrieveIssuer(tDsl, issuer).id();

            return tDsl.insertInto(BAN)
                    .set(BAN.ACCOUNT_ID,      punished.id())
                    .set(BAN.ISSUER_ID,       issuerId)
                    .set(BAN.REASON,          reason)
                    .set(BAN.SERVER_ID,       server.id())
                    .set(BAN.CREATION_DATE,   creation)
                    .set(BAN.EXPIRATION_DATE, expiration)
                    .returningResult(BAN)
                    .fetchOptionalInto(Ban.class)
                    .orElseThrow();
        });
    }

    private Mute muteQuery(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime creation, OffsetDateTime expiration) {

        Objects.requireNonNull(punished);
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);
        Objects.requireNonNull(creation);
        Objects.requireNonNull(expiration);
        if (expiration.isBefore(creation)) throw new IllegalArgumentException("The expiration date is before the creation date.");

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            final Integer issuerId = retrieveIssuer(tDsl, issuer).id();

            return tDsl.insertInto(MUTE)
                    .set(MUTE.ACCOUNT_ID,      punished.id())
                    .set(MUTE.ISSUER_ID,       issuerId)
                    .set(MUTE.REASON,          reason)
                    .set(MUTE.SERVER_ID,       server.id())
                    .set(MUTE.CREATION_DATE,   creation)
                    .set(MUTE.EXPIRATION_DATE, expiration)
                    .returningResult(MUTE)
                    .fetchOptionalInto(Mute.class)
                    .orElseThrow();
        });
    }

    @Override
    public Optional<Issuer> findIssuer(int id) {

        return database().dsl().transactionResult(ctx -> {
            final DSLContext tDsl = ctx.dsl();
            return tDsl.selectFrom(PUNISHMENT_ISSUER)
                    .where(PUNISHMENT_ISSUER.ID.eq(id))
                    .fetchOptionalInto(PunishmentIssuer.class)
                    .map(result -> switch (result.type()) {
                        case account -> Issuer.of(database().account().find(tDsl, result.accountId()).orElseThrow());
                        case server  -> Issuer.of(database().server() .find(tDsl, result.serverId()) .orElseThrow());
                    });
        });
    }

    @Override
    public Optional<Ban> latestBan(Account account) {
        Objects.requireNonNull(account);
        return database().dsl()
                .select()
                .from(BAN)
                .where(BAN.ACCOUNT_ID.eq(account.id()))
                .orderBy(BAN.CREATION_DATE.desc())
                .limit(1)
                .fetchOptionalInto(Ban.class);
    }

    @Override
    public Optional<Mute> latestMute(Account account) {
        Objects.requireNonNull(account);
        return database().dsl()
                .select()
                .from(MUTE)
                .where(MUTE.ACCOUNT_ID.eq(account.id()))
                .orderBy(MUTE.CREATION_DATE.desc())
                .limit(1)
                .fetchOptionalInto(Mute.class);
    }

    @Override
    public Optional<Warn> latestUnseenWarn(Account account) {
        Objects.requireNonNull(account);
        return database().dsl()
                .select()
                .from(WARN)
                .where(WARN.ACCOUNT_ID.eq(account.id()).and(WARN.SEEN.eq(false)))
                .orderBy(WARN.CREATION_DATE.desc())
                .limit(1)
                .fetchOptionalInto(Warn.class);
    }

    @Override
    public List<Warn> unseenWarns(Account account) {
        Objects.requireNonNull(account);
        return database().dsl()
                .select()
                .from(WARN)
                .where(WARN.ACCOUNT_ID.eq(account.id()).and(WARN.SEEN.eq(false)))
                .fetchInto(Warn.class);
    }

    @Override
    public void markWarnSeen(Warn warn) {
        database().dsl()
                .update(WARN)
                .set(WARN.SEEN, true)
                .where(WARN.ID.eq(warn.id()))
                .execute();
    }

    @Override
    public List<Ban> activeBans(Account account) {
        final var now = OffsetDateTime.now();
        return database().dsl()
                .select()
                .from(BAN)
                .leftAntiJoin(UNBAN).on(UNBAN.BAN_ID.eq(BAN.ID))
                .where(BAN.ACCOUNT_ID.eq(account.id()).and(BAN.EXPIRATION_DATE.isNull().or(BAN.EXPIRATION_DATE.greaterThan(now))))
                .fetchInto(Ban.class);
    }

    @Override
    public String uidFrom(Ban ban) {
        return database().sqidsEncode(Objects.requireNonNull(ban).id());
    }

    @Override
    public Optional<Ban> findBan(String uid) {

        Objects.requireNonNull(uid);
        final var oId = database().sqidsDecodeInt(uid);
        if (oId.isEmpty()) return Optional.empty();

        return database().dsl()
                .selectFrom(BAN)
                .where(BAN.ID.eq(oId.orElseThrow()))
                .fetchOptionalInto(Ban.class);
    }

    @Override
    public Optional<Ban> findBan(int id) {
        return database().dsl()
                .selectFrom(BAN)
                .where(BAN.ID.eq(id))
                .fetchOptionalInto(Ban.class);
    }

    @Override
    public Optional<Ban> findBan(Account account) {
        return database().dsl()
                .selectFrom(BAN)
                .where(ACCOUNT.ID.eq(account.id()))
                .fetchOptionalInto(Ban.class);
    }

    @Override
    public Optional<Kick> findKick(int id) {
        return database().dsl()
                .selectFrom(KICK)
                .where(KICK.ID.eq(id))
                .fetchOptionalInto(Kick.class);
    }

    @Override
    public Optional<Warn> findWarn(int id) {
        return database().dsl()
                .selectFrom(WARN)
                .where(WARN.ID.eq(id))
                .fetchOptionalInto(Warn.class);
    }

    @Override
    public Optional<Mute> findMute(int id) {
        return database().dsl()
                .selectFrom(MUTE)
                .where(MUTE.ID.eq(id))
                .fetchOptionalInto(Mute.class);
    }

    @Override
    public Ban infiteBan(Account punished, Issuer issuer, String reason, Server server) {
        return banQuery(punished, issuer, reason, server, OffsetDateTime.now(), null);
    }

    @Override
    public Ban ban(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime creation, OffsetDateTime expiration) {
        Objects.requireNonNull(expiration);
        return banQuery(punished, issuer, reason, server, creation, expiration);
    }

    @Override
    public Ban ban(Account punished, Issuer issuer, String reason, Server server, Duration duration) {
        Objects.requireNonNull(duration);
        final var now = OffsetDateTime.now();
        return banQuery(punished, issuer, reason, server, now, now.plus(duration));
    }

    @Override
    public Kick kick(Account punished, Issuer issuer, String reason, Server server) {

        Objects.requireNonNull(punished);
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            final Integer issuerId = retrieveIssuer(tDsl, issuer).id();

            return tDsl.insertInto(KICK)
                    .set(KICK.ACCOUNT_ID, punished.id())
                    .set(KICK.ISSUER_ID,  issuerId)
                    .set(KICK.REASON,     reason)
                    .set(KICK.SERVER_ID,  server.id())
                    .returningResult(KICK)
                    .fetchOptionalInto(Kick.class)
                    .orElseThrow();
        });
    }

    @Override
    public Warn warn(Account punished, Issuer issuer, String reason, Server server) {

        Objects.requireNonNull(punished);
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            final Integer issuerId = retrieveIssuer(tDsl, issuer).id();

            return tDsl.insertInto(WARN)
                    .set(WARN.ACCOUNT_ID, punished.id())
                    .set(WARN.ISSUER_ID,  issuerId)
                    .set(WARN.REASON,     reason)
                    .set(WARN.SERVER_ID,  server.id())
                    .returningResult(WARN)
                    .fetchOptionalInto(Warn.class)
                    .orElseThrow();
        });
    }

    @Override
    public UnbanStatus unban(Ban ban, Issuer issuer) {

        Objects.requireNonNull(ban);
        Objects.requireNonNull(issuer);

        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            final Integer issuerId = retrieveIssuer(tDsl, issuer).id();

            // I insert first, so I'm sure a row is always present,
            // and I avoid the collision in case I do it after the select.
            final var oUnban = tDsl.insertInto(UNBAN)
                    .set(UNBAN.BAN_ID, ban.id())
                    .set(UNBAN.ISSUER_ID, issuerId)
                    .onConflictDoNothing()
                    .returningResult(UNBAN)
                    .fetchOptionalInto(Unban.class)
                    .map(result -> new UnbanStatus(result, false));
            if (oUnban.isPresent()) return oUnban.orElseThrow();

            return tDsl.selectOne()
                    .where(UNBAN.BAN_ID.eq(ban.id()))
                    .fetchOptionalInto(Unban.class)
                    .map(result -> new UnbanStatus(result, true))
                    .orElseThrow();
        });
    }

    @Override
    public Mute mute(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime creation, OffsetDateTime expiration) {
        return muteQuery(punished, issuer, reason, server, creation, expiration);
    }

    @Override
    public Mute mute(Account punished, Issuer issuer, String reason, Server server, Duration duration) {
        Objects.requireNonNull(duration);
        final var now = OffsetDateTime.now();
        return muteQuery(punished, issuer, reason, server, now, now.plus(duration));
    }

    @Override
    public List<Punishment> recentPunishments(PunishmentType type, Server server) {
        return database().dsl().transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();

            // I clean up old queue elements that by this point all servers have handled.
            final var deleteRange = OffsetDateTime.now().minusHours(4);
            tDsl.delete(PUNISHMENT_QUEUE)
                    .where(PUNISHMENT_QUEUE.CREATION_DATE.lessOrEqual(deleteRange))
                    .execute();

            final var results = tDsl.select(PUNISHMENT_QUEUE)
                    .from(PUNISHMENT_QUEUE)
                    .leftOuterJoin(PUNISHMENT_ACKNOWLEDGE).on(PUNISHMENT_QUEUE.ID.eq(PUNISHMENT_ACKNOWLEDGE.QUEUE_ID))
                    .where(PUNISHMENT_QUEUE.TYPE.eq(type).and(PUNISHMENT_ACKNOWLEDGE.QUEUE_ID.isNull()))
                    .fetchInto(PunishmentQueue.class);
            if (results.isEmpty()) return List.of(); // Nothing to acknowledge.

            final var punishments = new ArrayList<Punishment>();
            var query = tDsl.insertInto(PUNISHMENT_ACKNOWLEDGE, PUNISHMENT_ACKNOWLEDGE.QUEUE_ID, PUNISHMENT_ACKNOWLEDGE.SERVER_ID);

            for (var result : results) {
                final int id = switch (result.type()) {
                    case ban  -> result.banId();
                    case kick -> result.kickId();
                    case warn -> result.warnId();
                    case mute -> result.muteId();
                };
                punishments.add(new Punishment(result.type(), id));
                query = query.values(result.id(), server.id());
            }
            // I make this server acknowledge the queue to avoid retrieving already handled elements.
            query.execute();
            return punishments;
        });
    }
}
