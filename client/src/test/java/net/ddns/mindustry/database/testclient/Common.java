package net.ddns.mindustry.database.testclient;

import org.sqids.Sqids;
import java.security.SecureRandom;

public final class Common {

    public static final Sqids SQIDS = Sqids.builder()
            .alphabet("abcdefghijklmnopqrstuvwxyz0123456789") // Lowercase for usernames.
            .build();
    public static final SecureRandom RANDOM = new SecureRandom();
}
