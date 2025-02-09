module net.ddns.mindustry.database.client {
    requires de.mkammerer.argon2.nolibs;
    requires org.jooq.postgres.extensions;
    exports net.ddns.mindustry.database.client;
    exports net.ddns.mindustry.database.schema.tables.pojos;
}