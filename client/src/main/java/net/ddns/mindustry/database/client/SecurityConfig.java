package net.ddns.mindustry.database.client;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/// Class containing the hash configuration for sessions and passwords.
public final class SecurityConfig {

    // TODO Include algorithms like SHA2-256, if not available SHA3-256?
    private final MessageDigest sessionDigest;
    private final Argon2 argon2;
    private final int argon2Iteration;
    private final int argon2Memory;
    private final int argon2Parallelism;

    public SecurityConfig(String sessionAlgorithm,
                          int saltLength,
                          int hashLength,
                          int argon2Iteration,
                          int argon2Memory,
                          int argon2Parallelism) throws NoSuchAlgorithmException {
        Objects.requireNonNull(sessionAlgorithm);
        this.sessionDigest = MessageDigest.getInstance(sessionAlgorithm);
        // ARGON2id by default.
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, saltLength, hashLength);
        this.argon2Iteration = argon2Iteration;
        this.argon2Memory = argon2Memory;
        this.argon2Parallelism = argon2Parallelism;
    }

    @Deprecated
    /// TEST ONLY!
    public SecurityConfig() throws NoSuchAlgorithmException {
        this("SHA-256", 32, 255, 10, 69_000, 8);
    }

    public MessageDigest sessionHash() {
        return sessionDigest;
    }

    public Argon2 passHash() {
        return argon2;
    }

    public String hashPass(char[] password) {
        return passHash().hash(argon2Iteration(), argon2Memory(), argon2Parallelism(), password);
    }

    public int argon2Iteration() {
        return argon2Iteration;
    }

    public int argon2Memory() {
        return argon2Memory;
    }

    public int argon2Parallelism() {
        return argon2Parallelism;
    }
}
