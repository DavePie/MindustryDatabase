module net.ddns.mindustry.database.client {
    requires org.jooq;
    requires de.mkammerer.argon2.nolibs;
    requires org.mariadb.jdbc;
    exports net.ddns.mindustry.database.client;
    exports net.ddns.mindustry.database.schema.tables.pojos;
}