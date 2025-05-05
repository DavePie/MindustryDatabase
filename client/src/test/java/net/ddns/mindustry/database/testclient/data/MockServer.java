package net.ddns.mindustry.database.testclient.data;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.junit.jupiter.api.Assertions;
import java.util.Objects;

public record MockServer(String ip, int port, String name) {

    public static MockServer instance() {
        return new MockServer("127.0.0.1", 8312, "some_server");
    }

    /// Retrieves the server row using the instance data.\
    /// If the instance data is not saved on the db, this method will fail.
    public static Server fromDb(Database con) {
        Objects.requireNonNull(con);
        final var server  = MockServer.instance();
        final var oServer = con.server().find(server.ip(), server.port());
        if (oServer.isEmpty()) Assertions.fail("The server did not get added.");
        return oServer.orElseThrow();
    }
}
