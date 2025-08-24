package net.ddns.mindustry.database.testclient.tests;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.testclient.DbInitialization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public final class ServerTest {

    private final Database db;

    public ServerTest() {
        this.db = DbInitialization.newConnection();
    }

    @Test
    public void addServerIpv4() {

        final String ip = "127.29.0.1";
        final int port = 5678;
        final String name = "AddServerIpv4";

        final boolean added = db.server().add(ip, port, name);
        Assertions.assertTrue(added);

        final boolean conflict = db.server().add(ip, port, name);
        Assertions.assertFalse(conflict);
    }

    @Test
    public void addServerIpv6() {

        final String ip = "2001:db8:3333:4444:5555:6666:7777:8888";
        final int port = 5678;
        final String name = "AddServerIpv6";

        final boolean added = db.server().add(ip, port, name);
        Assertions.assertTrue(added);

        final boolean conflict = db.server().add(ip, port, name);
        Assertions.assertFalse(conflict);
    }
}
