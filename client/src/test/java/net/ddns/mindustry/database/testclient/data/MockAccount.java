package net.ddns.mindustry.database.testclient.data;

import org.jooq.postgres.extensions.types.Inet;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import static net.ddns.mindustry.database.testclient.Common.*;

public record MockAccount(String username, char[] password, String ip, String uuid) {

    private static final HashSet<String> GENERATED = new HashSet<>();

    private static boolean collision(String username) {
        synchronized (GENERATED) {
            final boolean collision = GENERATED.contains(username);
            GENERATED.add(username);
            return collision;
        }
    }

    public static MockAccount random() {

        final String username = SQIDS.encode(List.of(RANDOM.nextLong(0, Long.MAX_VALUE)));
        if (collision(username)) {
            System.out.println("Woah, an username collision: " + username);
            return random();
        }
        final char[] password = SQIDS.encode(List.of(RANDOM.nextLong(0, Long.MAX_VALUE))).toCharArray();
        final byte[] ipv4Raw  = new byte[4]; RANDOM.nextBytes(ipv4Raw);
        final String ipv4;
        try { ipv4 = Inet.inet(Inet4Address.getByAddress(ipv4Raw)).toString();
        } catch (UnknownHostException e) { throw new IllegalStateException(e);}
        final String uuid = UUID.randomUUID().toString();
        return new MockAccount(username, password, ipv4, uuid);
    }

    public MockAccount withIp(String ip) {
        return new MockAccount(username, password, ip, uuid);
    }
}