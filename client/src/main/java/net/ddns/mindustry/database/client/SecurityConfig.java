package net.ddns.mindustry.database.client;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.jspecify.annotations.NullMarked;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/// Class containing the hash configuration for sessions and passwords.
@NullMarked
public record SecurityConfig(MessageDigest sessionDigest,
                             Argon2 argon2,
                             int argon2Iteration,
                             int argon2Memory,
                             int argon2Parallelism,
                             int minimumPasswordLength,
                             int accountLimit) {

    public SecurityConfig {
        Objects.requireNonNull(sessionDigest);
        Objects.requireNonNull(argon2);
        if (minimumPasswordLength <= 0) throw new IllegalArgumentException("The minimum password length cannot be 0 or negative.");
        if (accountLimit <= 0) throw new IllegalArgumentException("The account limit cannot be 0 or negative.");
    }

    public SecurityConfig(String sessionAlgorithm,
                          int saltLength,
                          int hashLength,
                          int argon2Iteration,
                          int argon2Memory,
                          int argon2Parallelism,
                          int minimumPasswordLength,
                          int accountsLimit) throws NoSuchAlgorithmException {
        this(MessageDigest.getInstance(Objects.requireNonNull(sessionAlgorithm)),
                // ARGON2id by default.
                Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, saltLength, hashLength),
                argon2Iteration,
                argon2Memory,
                argon2Parallelism,
                minimumPasswordLength,
                accountsLimit);
    }

    public String hashPass(char[] password) {
        return argon2().hash(argon2Iteration(), argon2Memory(), argon2Parallelism(), password);
    }

    public static final class Builder {

        private String sessionAlgorithm = "SHA-256";
        private int saltLength;
        private int hashLength;
        private int argon2Iteration;
        private int argon2Memory;
        private int argon2Parallelism;
        private int minimumPasswordLength = 5;
        private int accountLimit = 5;

        private Builder() {}

        public static Builder create() {
            return new Builder();
        }

        public Builder sessionAlgorithm(String algorithm) {
            this.sessionAlgorithm = Objects.requireNonNull(algorithm);
            return this;
        }

        public Builder saltLength(int length) {
            this.saltLength = length;
            return this;
        }

        public Builder hashLength(int length) {
            this.hashLength = length;
            return this;
        }

        public Builder argon2Iteration(int iterations) {
            this.argon2Iteration = iterations;
            return this;
        }

        public Builder argon2Memory(int memory) {
            this.argon2Memory = memory;
            return this;
        }

        public Builder argon2Parallelism(int parallelism) {
            this.argon2Parallelism = parallelism;
            return this;
        }

        public Builder minimumPasswordLength(int length) {
            this.minimumPasswordLength = length;
            return this;
        }

        /// @param limit the number of accounts allowed for a single user, the recommended amount is 5.
        public Builder accountLimit(int limit) {
            this.accountLimit = limit;
            return this;
        }

        public SecurityConfig build() throws NoSuchAlgorithmException {
            return new SecurityConfig(sessionAlgorithm,
                    saltLength,
                    hashLength,
                    argon2Iteration,
                    argon2Memory,
                    argon2Parallelism,
                    minimumPasswordLength,
                    accountLimit);
        }
    }
}
