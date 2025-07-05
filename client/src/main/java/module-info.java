module net.ddns.mindustry.database.client {
    requires de.mkammerer.argon2.nolibs;
    requires org.postgresql.jdbc;
    requires org.jooq.postgres.extensions;
    requires org.jspecify;
    requires sqids;
    exports net.ddns.mindustry.database.client;
    exports net.ddns.mindustry.database.schema.tables.pojos;
}