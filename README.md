# Mindustry Database
A Mindustry plugin to store and modify player artifacts in a centralized database system.

[![](https://www.jitpack.io/v/mindustry-ddns-net/MindustryDatabase.svg)](https://www.jitpack.io/#mindustry-ddns-net/MindustryDatabase)


# Installation
## Supports
[ ] Client \
[ ] Client server \
[x] Headless server

## Headless Server
1. Download the latest `.jar`
2. Copy the `.jar`
3. Navigate to your server root directory
4. Navigate to `[server root]/config/mods`
5. Paste the `.jar`
6. Start or restart the server
7. Type in the respective config values for `ip`, `url`, `user`, and `password` via `config CONFIG PARAMETER`
   (Refer to the [Server Commands](#server) section for additional information about the commands (such as parameters))
8. Run the `reload-configurations` command.
9. Register the server to the database via the `register-server` command.

# Configs
- `server-ip` - The IP of the Mindustry server. This is optional and should only be changed when necessary. Default is
`127.0.0.1`. This shouldn't be changed directly unless you know what you're doing. Change this via the `update-ip`
command.
- `url` - The URL of the database.
- `user` - The database user to authenticate as.
- `password` - The password of the database user.
- `session-duration` - The maximum duration of a player session. Defaults to 12 hours. If the session exceeds the
maximum duration, then the user's session will become invalid when the user joins.

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

## Client
Refer to the `/help` command in-game.

# Database Schema
For the database schema, see [this file](client/tables.sql).
