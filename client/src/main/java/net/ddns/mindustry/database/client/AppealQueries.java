package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Appeal;
import net.ddns.mindustry.database.schema.tables.pojos.AppealReply;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface AppealQueries {

    String uidFrom(Appeal appeal);

    String uidFrom(AppealReply appealReply);

    Optional<Appeal> findAppeal(String uid);

    Optional<AppealReply> findAppealReply(String uid);

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

    sealed interface Status {
        record Ok(Appeal appeal) implements Status {
            public Ok {
                Objects.requireNonNull(appeal);
            }
        }
        record EmptyMessage() implements Status {}
        record RateLimited() implements Status {}
    }
}
