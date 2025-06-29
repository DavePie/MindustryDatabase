package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Appeal;
import net.ddns.mindustry.database.schema.tables.pojos.AppealReply;
import org.jspecify.annotations.NullMarked;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public interface AppealQueries {

    Optional<Appeal> findAppeal(long uid);

    Optional<AppealReply> findAppealReply(long uid);

    Status appeal(Account account, String message);

    AppealReply replyToAppeal(Appeal appeal, Account staff, String message, boolean acceptAppeal);

    void deleteAppeal(Appeal appeal);

    void deleteAppealReply(AppealReply appealReply);

    List<Appeal> accountAppeals(Account account);

    int accountAppealsCount(Account account);

    Optional<Appeal> latestAccountAppeal(Account account);

    List<AppealReply> appealReplies(Appeal appeal);

    int appealRepliesCount(Appeal appeal);

    List<Appeal> openAppeals(int limit);

    int openAppealsCount();

    List<Appeal> openAccountAppeals(Account account);

    int openAccountAppealsCount(Account account);

    interface Status {
        record Ok(Appeal appeal) implements Status {
            public Ok {
                Objects.requireNonNull(appeal);
            }
        }
        record EmptyMessage() implements Status {}
        record RateLimited() implements Status {}
    }
}
