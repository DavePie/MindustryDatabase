package net.ddns.mindustry.database.client;

import java.util.function.IntConsumer;

public interface PunishmentListeners {

    void register(Type type, IntConsumer listener);

    void unregister(Type type, IntConsumer listener);

    enum Type {
        BAN,
        KICK,
        WARN
    }
}
