package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Ban;
import net.ddns.mindustry.database.schema.tables.pojos.Kick;
import net.ddns.mindustry.database.schema.tables.pojos.Warn;
import java.util.function.Consumer;

public interface PunishmentListeners {

    void registerBanListener(Consumer<Ban> listener);

    void unregisterBanListener(Consumer<Ban> listener);

    void registerKickListener(Consumer<Kick> listener);

    void unregisterKickListener(Consumer<Kick> listener);

    void registerWarnListener(Consumer<Warn> listener);

    void unregisterWarnListener(Consumer<Warn> listener);
}
