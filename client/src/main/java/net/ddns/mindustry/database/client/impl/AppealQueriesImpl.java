package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.AppealQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Appeal;
import net.ddns.mindustry.database.schema.tables.pojos.AppealReply;
import org.jspecify.annotations.NullMarked;
import java.util.List;
import java.util.Optional;
import static net.ddns.mindustry.database.schema.Tables.APPEAL;
import static net.ddns.mindustry.database.schema.Tables.APPEAL_REPLY;

@NullMarked
public record AppealQueriesImpl(DatabaseImpl database) implements AppealQueries {

    @Override
    public Optional<Appeal> findAppeal(long uid) {
        return database().dsl()
                .selectFrom(APPEAL)
                .where(APPEAL.UID.eq(uid))
                .fetchOptionalInto(Appeal.class);
    }

    @Override
    public Optional<AppealReply> findAppealReply(long uid) {
        return database().dsl()
                .selectFrom(APPEAL_REPLY)
                .where(APPEAL_REPLY.UID.eq(uid))
                .fetchOptionalInto(AppealReply.class);
    }

    @Override
    public Status appeal(Account account, String message) {
        return null;
    }

    @Override
    public void replyToAppeal(Appeal appeal, Account account, String message, boolean acceptAppeal) {
        for (int i = 0; i < 100; i++) {

            final long uid = database()
                    .securityConfig()
                    .random()
                    .nextLong();

            final boolean inserted = database().dsl()
                    .insertInto(APPEAL_REPLY)
                    .set(APPEAL_REPLY.UID, uid)
                    .set(APPEAL_REPLY.APPEAL_ID, appeal.id())
                    .set(APPEAL_REPLY.MESSAGE, message)
                    .set(APPEAL_REPLY.ACCEPTED, acceptAppeal)
                    .onConflictDoNothing()
                    .execute() == 1;
            if (inserted) return;
        }
        throw new IllegalStateException("Could not insert the appeal with an unique uid.");
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
