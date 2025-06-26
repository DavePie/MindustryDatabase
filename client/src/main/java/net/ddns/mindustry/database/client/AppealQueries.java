package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Appeal;
import net.ddns.mindustry.database.schema.tables.pojos.AppealReply;
import java.util.List;
import java.util.Optional;

public interface AppealQueries {

    Optional<Appeal> findAppeal(long uid);

    Optional<AppealReply> findAppealReply(long uid);

    Status appeal(Account account, String message);

    void replyToAppeal(Appeal appeal, Account account, String message, boolean acceptAppeal);

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

    enum Status { OK, EMPTY_MESSAGE, RATE_LIMITED }
}
