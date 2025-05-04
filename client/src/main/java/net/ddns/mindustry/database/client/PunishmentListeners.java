package net.ddns.mindustry.database.client;

import org.jspecify.annotations.NullMarked;
import java.util.function.IntConsumer;

@NullMarked
public interface PunishmentListeners {

    void register(Type type, IntConsumer listener);

    void unregister(Type type, IntConsumer listener);

    enum Type {
        BAN,
        KICK,
        WARN
    }
}
