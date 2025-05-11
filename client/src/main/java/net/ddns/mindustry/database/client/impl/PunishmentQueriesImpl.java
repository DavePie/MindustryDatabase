package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.enums.PunishmentIssuerType;
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
public record PunishmentQueriesImpl(DSLContext dsl, AccountQueriesImpl accountImpl, ServerQueriesImpl serverImpl) implements PunishmentQueries {

    private static final SecureRandom RANDOM = new SecureRandom();

    public Optional<Ban> findBan(DSLContext tDsl, long uuid) {
        return tDsl.selectFrom(BAN)
                .where(BAN.UUID.eq(uuid))
                .fetchOptionalInto(Ban.class);
    }

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

    private Ban banQuery(Account punished, Issuer issuer, String reason, Server server, @Nullable OffsetDateTime expiration) {

        Objects.requireNonNull(punished);
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);
        // Expiration can be nullable.

        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            final Integer issuerId = retrieveIssuer(tDsl, issuer).id();

            // I generate an unique uuid.
            long randomUuid = RANDOM.nextLong();
            while (findBan(tDsl, randomUuid).isPresent()) randomUuid = RANDOM.nextLong();

            return tDsl.insertInto(BAN)
                    .set(BAN.ACCOUNT_ID,      punished.id())
                    .set(BAN.ISSUER_ID,       issuerId)
                    .set(BAN.REASON,          reason)
                    .set(BAN.SERVER_ID,       server.id())
                    .set(BAN.UUID,            randomUuid)
                    .set(BAN.EXPIRATION_DATE, expiration)
                    .returningResult(BAN)
                    .fetchOptionalInto(Ban.class)
                    .orElseThrow();
        });
    }

    @Override
    public Optional<Issuer> findIssuer(int id) {

        return dsl.transactionResult(ctx -> {
            final DSLContext tDsl = ctx.dsl();
            return tDsl.selectFrom(PUNISHMENT_ISSUER)
                    .where(PUNISHMENT_ISSUER.ID.eq(id))
                    .fetchOptionalInto(PunishmentIssuer.class)
                    .map(result -> switch (result.type()) {
                        case account -> Issuer.of(accountImpl().find(tDsl, result.accountId()).orElseThrow());
                        case server -> Issuer.of(serverImpl().find(tDsl, result.serverId()).orElseThrow());
                    });
        });
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
    public Ban infiteBan(Account punished, Issuer issuer, String reason, Server server) {
        return banQuery(punished, issuer, reason, server, null);
    }

    @Override
    public Ban ban(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime expiration) {
        Objects.requireNonNull(expiration);
        return banQuery(punished, issuer, reason, server, expiration);
    }

    @Override
    public Ban ban(Account punished, Issuer issuer, String reason, Server server, Duration duration) {
        Objects.requireNonNull(duration);
        return ban(punished, issuer, reason, server, OffsetDateTime.now().plus(duration));
    }

    @Override
    public Kick kick(Account punished, Issuer issuer, String reason, Server server) {

        Objects.requireNonNull(punished);
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(server);

        return dsl.transactionResult(ctx -> {

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

        return dsl.transactionResult(ctx -> {

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

        return dsl.transactionResult(ctx -> {

            final DSLContext tDsl = ctx.dsl();
            final Integer issuerId = retrieveIssuer(tDsl, issuer).id();

            // I insert first, so I'm sure a row is always present,
            // and I avoid the collision in case I do it after the select.
            final var oUnban = dsl.insertInto(UNBAN)
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
}
