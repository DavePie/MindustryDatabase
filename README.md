# Mindustry Database
A Mindustry plugin to store and modify player artifacts in a centralized database system. It also ships with a database.

[![](https://www.jitpack.io/v/mindustry-ddns-net/MindustryDatabase.svg)](https://www.jitpack.io/#mindustry-ddns-net/MindustryDatabase)


# Installation
## Supports
[ ] Client \
[ ] Client server \
[x] Headless server \
*there are currently **zero** plans to support the other mentioned device types*

## Headless Server
### Pre-requisites
You only have to do this once, unless you intend on having multiple different databases. This will assume that you have
a basic level of knowledge on how to set up computer systems.
1. Download postgresql. Whichever way you do it is up to you, but it must be accessible by the database.
2. Git clone this repository.
3. Navigate to the client directory, and create a new file called `jooq.properties`. Edit the file.
4. Set the username/password, an example is provided below. Note that the `/mindustry_database` at the end is necessary.
Also ensure to change the `localhost` to the appropriate IP/domain.
```properties
url=jdbc:postgresql://localhost/mindustry_database
user=lett
password=R2V0IFBob3MnZA==
```
5. Save the file, and go to the root of the Git repository. Run `./gradlew jooq`.
6. Initial setup should be complete. Refer to the [per-server configuration](#per-server-setup) section.

### Per-server setup
1. Download the latest `.jar`
2. Copy the `.jar`
3. Navigate to your server root directory
4. Navigate to `[server root]/config/mods`
5. Paste the `.jar`
6. Start or restart the server
7. Type in the respective config values for `ip`, `url`, `user`, and `password` via `config CONFIG PARAMETER`
   (Refer to the [Server Commands](#server) section for additional information about the commands (such as parameters))
8. Restart the server.
9. Register the server to the database via the `register-server` command.
10. Add roles via the `add-role` command and grant them permissions via the `grant-permission` command.
11. Grant roles to players via the `grant-role` command. *Note: the player must first be registered to the database.*

# Configurations
- `server-ip` - The IP of the Mindustry server. This is optional and should only be changed when necessary. Default is
`127.0.0.1`. This shouldn't be changed directly unless you know what you're doing. Change this via the `update-ip`
command.
- `url` - The URL of the database.
- `user` - The database user to authenticate as.
- `password` - The password of the database user. This is recommended, and it'll be best to use a randomized and secure
password.
- `account-limit` - The maximum number of accounts that an individual player can create. Defaults to 5 accounts.
- `session-duration` - The maximum duration of a player session. Defaults to 12 hours. If the session exceeds the
maximum duration, then the user's session will become invalid when the user joins.
- `command-rate-limit` - The average time (in seconds) between the configured number of command attempts required to 
kick a player.
- `command-attempts` - The number of attempts that are recorded for the command rate limit. To configure the average
amount of time (in seconds), refer to the `command-rate-limit` configuration.

# Commands
## Server
- `register-server <name>` - Registers the server into the database.
- `deregister-server [id]` - Deregisters the server from the database. If an ID is provided, then it will deregister
that specific server.
- `list-servers` - Lists all the servers in the database.
- `reload-configurations` - Reloads anything that is dependent on the configuration of the server.
- `update-ip <new-ip>` - Updates the IP of the database entry for the server.
- `update-port <new-port>` - Updates the port of the database entry for the server.
- `update-name <new-name>` - Updates the name of the server. Does **NOT** update the `serverName` configuration.
- `add-role <name> <color> <symbol> <priority>` - Adds a role to the database.
- `delete-role <id>` - Deletes a role from the database.
- `grant-role <account-username> <role-name>` - Grants a role to an account.
- `list-roles [account-username]` - Lists all roles that are in the database. Lists the roles for an account if 
provided.
- `revoke-role <account-username> <role-name>` - Revokes a role from an account.
- `revoke-role <account-username> <role-name>` - Revokes a role from an account.
- `grant-permission <role-name> <permissions...>` - Grants a permission to a role.
- `list-permissions [role]` - List all permissions in the database.
- `revoke-permission <role-name> <permissions...>` - Revokes a permission from a role.

## Client
Refer to the `/help` command in-game.

# Database Schema
For the database schema, see [this file](client/tables.sql).
