# MindustryDatabase client
This is the module containing **only** the database queries; it helps provide simple bindings. \
The separation from Mindustry code helps for concurrent work from different teams, and to only focus at one module at a time.

# SQL Schema
The schema can be found [here](tables.sql).

# Dependencies (TODO FatJar?)
```groovy
dependencies {
    implementation("org.mariadb.jdbc:mariadb-java-client:${mariadb}")
    implementation("org.jooq:jooq:${jooq}")
}
```

# JOOQ Code Generation
### Setting up the database.
The file `jooq.properties` must be created in the module root having the following properties:
```properties
url=jdbc:mariadb://[your-jdbc-host]:[port]/mindustry_database
user=database_user
password=database_password
```
*The database creation, and user permissions, must be done manually.*

### Generating the classes.
Once the database is ready, to generate the JOOQ schema classes, run `gradle client:jooqCodegen`.


