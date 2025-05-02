module net.ddns.mindustry.database.client {
    requires de.mkammerer.argon2.nolibs;
    requires org.postgresql.jdbc;
    requires org.jooq.postgres.extensions;
    requires com.fasterxml.jackson.databind;
    exports net.ddns.mindustry.database.client;
    exports net.ddns.mindustry.database.schema.tables.pojos;
}