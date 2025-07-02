package net.ddns.mindustry.database.client;

import org.jspecify.annotations.NullMarked;
import java.util.Objects;
import java.util.function.Consumer;

@NullMarked
public interface DatabaseEvents {

    void register(Type type, Consumer<Event> listener);

    void unregister(Type type, Consumer<Event> listener);

    sealed interface Event {

        record Value(int id) implements Event {}

        record Failure(Exception exception) implements Event {
            public Failure {
                Objects.requireNonNull(exception);
            }
        }
    }

    enum Type {
        BAN,
        KICK,
        WARN,
        MUTE
    }
}
