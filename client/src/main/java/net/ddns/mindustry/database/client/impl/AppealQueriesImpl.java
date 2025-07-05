package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.AppealQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Appeal;
import net.ddns.mindustry.database.schema.tables.pojos.AppealReply;
import org.jspecify.annotations.NullMarked;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static net.ddns.mindustry.database.schema.Tables.APPEAL;
import static net.ddns.mindustry.database.schema.Tables.APPEAL_REPLY;

@NullMarked
public record AppealQueriesImpl(DatabaseImpl database) implements AppealQueries {

    @Override
    public String uidFrom(Appeal appeal) {
        return database().sqidsEncode(Objects.requireNonNull(appeal).id());
    }

    @Override
    public String uidFrom(AppealReply appealReply) {
        return database().sqidsEncode(Objects.requireNonNull(appealReply).id());
    }

    @Override
    public Optional<Appeal> findAppeal(String uid) {

        final var oId = database().sqidsDecodeInt(uid);
        if (oId.isEmpty()) return Optional.empty();

        return database().dsl()
                .selectFrom(APPEAL)
                .where(APPEAL.ID.eq(Math.toIntExact(oId.orElseThrow())))
                .fetchOptionalInto(Appeal.class);
    }

    @Override
    public Optional<AppealReply> findAppealReply(String uid) {

        final var oId = database().sqidsDecodeInt(uid);
        if (oId.isEmpty()) return Optional.empty();

        return database().dsl()
                .selectFrom(APPEAL_REPLY)
                .where(APPEAL_REPLY.ID.eq(oId.orElseThrow()))
                .fetchOptionalInto(AppealReply.class);
    }

    @Override
    public Status appeal(Account account, String message) {

        Objects.requireNonNull(account);
        Objects.requireNonNull(message);
        if (message.isBlank()) return new Status.EmptyMessage();

        final boolean rateLimited = latestAccountAppeal(account)
                .map(Appeal::creationDate)
                // I transform the creation date to the duration between it and now.
                .map(latest -> Duration.between(latest, OffsetDateTime.now()))
                // I rate limit if the appeal has been issued before 6 hours since the last one.
                .map(duration -> duration.toHours() < 6)
                .orElse(false);

        if (rateLimited) return new Status.RateLimited();
        return database().dsl()
                .insertInto(APPEAL)
                .set(APPEAL.ACCOUNT_ID, account.id())
                .set(APPEAL.MESSAGE, message)
                .returningResult()
                .fetchOptionalInto(Appeal.class)
                .map(Status.Ok::new)
                .orElseThrow();
    }

    @Override
    public AppealReply replyToAppeal(Appeal appeal, Account staff, String message, boolean acceptAppeal) {
        return database().dsl()
                .insertInto(APPEAL_REPLY)
                .set(APPEAL_REPLY.APPEAL_ID, appeal.id())
                .set(APPEAL_REPLY.STAFF_ID, staff.id())
                .set(APPEAL_REPLY.MESSAGE, message)
                .set(APPEAL_REPLY.ACCEPTED, acceptAppeal)
                .returningResult()
                .fetchOptionalInto(AppealReply.class)
                .orElseThrow();
    }

    @Override
    public void deleteAppeal(Appeal appeal) {
        database().dsl()
                .deleteFrom(APPEAL)
                .where(APPEAL.ID.eq(appeal.id()))
                .execute();
    }

    @Override
    public void deleteAppealReply(AppealReply appealReply) {
        database().dsl()
                .deleteFrom(APPEAL_REPLY)
                .where(APPEAL_REPLY.ID.eq(appealReply.id()))
                .execute();
    }

    @Override
    public List<Appeal> accountAppeals(Account account) {
        return database().dsl()
                .selectFrom(APPEAL)
                .where(APPEAL.ACCOUNT_ID.eq(account.id()))
                .fetchInto(Appeal.class);
    }

    @Override
    public int accountAppealsCount(Account account) {
        return database().dsl()
                .selectCount()
                .from(APPEAL)
                .where(APPEAL.ACCOUNT_ID.eq(account.id()))
                .fetchOptionalInto(int.class)
                .orElseThrow(() -> new IllegalStateException("SELECT COUNT failed."));
    }

    @Override
    public Optional<Appeal> latestAccountAppeal(Account account) {
        Objects.requireNonNull(account);
        return database.dsl()
                .selectFrom(APPEAL)
                .where(APPEAL.ACCOUNT_ID.eq(account.id()))
                .orderBy(APPEAL.CREATION_DATE.desc())
                .limit(1)
                .fetchOptionalInto(Appeal.class);
    }

    @Override
    public List<AppealReply> appealReplies(Appeal appeal) {
        return database().dsl()
                .selectFrom(APPEAL_REPLY)
                .where(APPEAL_REPLY.APPEAL_ID.eq(appeal.id()))
                .fetchInto(AppealReply.class);
    }

    @Override
    public int appealRepliesCount(Appeal appeal) {
        return database().dsl()
                .selectCount()
                .from(APPEAL_REPLY)
                .where(APPEAL_REPLY.APPEAL_ID.eq(appeal.id()))
                .fetchOptionalInto(int.class)
                .orElseThrow(() -> new IllegalStateException("SELECT COUNT failed."));
    }

    @Override
    public List<Appeal> openAppeals(int limit) {
        return database().dsl()
                .select(APPEAL)
                .from(APPEAL)
                .leftJoin(APPEAL_REPLY).on(APPEAL.ID.eq(APPEAL_REPLY.APPEAL_ID))
                .where(APPEAL_REPLY.APPEAL_ID.isNull()) // I select only the appeals with no replies.
                .limit(limit)
                .fetchInto(Appeal.class);
    }

    @Override
    public int openAppealsCount() {
        return database().dsl()
                .selectCount()
                .from(APPEAL)
                .leftJoin(APPEAL_REPLY).on(APPEAL.ID.eq(APPEAL_REPLY.APPEAL_ID))
                .where(APPEAL_REPLY.APPEAL_ID.isNull()) // I select only the appeals with no replies.
                .fetchOptionalInto(int.class)
                .orElseThrow(() -> new IllegalStateException("SELECT COUNT failed."));
    }

    @Override
    public List<Appeal> openAccountAppeals(Account account) {
        return database().dsl()
                .select(APPEAL)
                .from(APPEAL)
                .leftJoin(APPEAL_REPLY).on(APPEAL.ID.eq(APPEAL_REPLY.APPEAL_ID))
                .where(APPEAL.ACCOUNT_ID.eq(account.id()).and(APPEAL_REPLY.APPEAL_ID.isNull())) // I select only the appeals with no replies.
                .fetchInto(Appeal.class);
    }

    @Override
    public int openAccountAppealsCount(Account account) {
        return database().dsl()
                .selectCount()
                .from(APPEAL)
                .leftJoin(APPEAL_REPLY).on(APPEAL.ID.eq(APPEAL_REPLY.APPEAL_ID))
                .where(APPEAL.ACCOUNT_ID.eq(account.id()).and(APPEAL_REPLY.APPEAL_ID.isNull())) // I select only the appeals with no replies.
                .fetchOptionalInto(int.class)
                .orElseThrow(() -> new IllegalStateException("SELECT COUNT failed."));
    }
}
