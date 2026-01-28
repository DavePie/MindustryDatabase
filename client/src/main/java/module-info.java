import org.jspecify.annotations.NullMarked;

@NullMarked
module net.ddns.mindustry.database.client {
    requires de.mkammerer.argon2.nolibs;
    requires org.postgresql.jdbc;
    requires org.jooq.postgres.extensions;
    requires org.jspecify;
    requires sqids;
    requires com.zaxxer.hikari;
    exports net.ddns.mindustry.database.client;
    exports net.ddns.mindustry.database.schema.tables.pojos;
    exports net.ddns.mindustry.database.schema.enums;
}