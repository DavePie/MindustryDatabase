package net.ddns.mindustry.database.testclient;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.SecurityConfig;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public final class DbInitialization {

    private static Properties dbConfig() {
        final Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("jooq.properties"))) {
            properties.load(reader);
        } catch (IOException e) {
            Assertions.fail("Could not load the jooq.properties file.", e);
        }
        return properties;
    }

    private static void clearDatabase(String url, String user, String pass) {

        // I read the SQL initialization file.
        final String sql;
        try { sql = String.join("\n", Files.readAllLines(Path.of("tables.sql")));
        } catch (IOException e) {
            Assertions.fail("Could not read the tables.sql file.", e);
            return;
        }

        // TODO Retrieve the database separately from the url
        // I drop and create the database.
        try (var dsl = DSL.using(url.replaceFirst("mindustry_database", ""), user, pass)) {
            dsl.dropDatabaseIfExists("mindustry_database").execute();
            dsl.createDatabase("mindustry_database").execute();
        }
        // I re-create all tables.
        try (var dsl = DSL.using(url, user, pass)) {
            dsl.execute(sql);
        }
    }

    public static void clearDatabase() {
        final Properties dbConfig = dbConfig();
        final var url  = dbConfig.getProperty("url");
        final var user = dbConfig.getProperty("user");
        final var pass = dbConfig.getProperty("password");
        clearDatabase(url, user, pass);
    }

    public static Database newConnection(int accountLimit) {

        final Properties dbConfig = dbConfig();
        final var url  = dbConfig.getProperty("url");
        final var user = dbConfig.getProperty("user");
        final var pass = dbConfig.getProperty("password");

        final var builder = SecurityConfig.Builder.create()
                .saltLength(16)
                .hashLength(128)
                .argon2Iteration(10)
                .argon2Memory(20)
                .argon2Parallelism(2)
                .accountLimit(accountLimit);

        final SecurityConfig securityConfig;
        try { securityConfig = builder.build();
        } catch (NoSuchAlgorithmException e) {
            Assertions.fail("Missing default hash algorithm", e);
            throw new IllegalStateException();
        }
        return Database.newConnection(url, user, pass, securityConfig);
    }

    public static Database clearAndConnect(int accountLimit) {
        clearDatabase();
        return newConnection(accountLimit);
    }

    private DbInitialization() {}
}
