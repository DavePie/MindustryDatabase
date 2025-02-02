# Mindustry Database
A Mindustry plugin for saving and handling data about players.

[![](https://www.jitpack.io/v/mindustry-ddns-net/MindustryDatabase.svg)](https://www.jitpack.io/#mindustry-ddns-net/MindustryDatabase)



# Keep in mind
This doesn't provide any moderation commands itself. However, the database does provide methods for moderation.

# Installation
## Supports
[ ] Client <br>
[ ] Client server <br>
[x] Headless server

## Headless Server
1. Download the latest `.jar`
2. Copy the `.jar`
3. Navigate to your server root directory
4. Navigate to `[server root]/config/mods`
5. Paste the `.jar`
6. Start or restart the server
7. Type in the respective config values for `url`, `user`, and `password` via `config CONFIG PARAMETER`
8. Run the `reconnect` command.
9. Register the server to the database via the `register` command (if you haven't run the `update-ip` command).

# Configs
- `server-ip` - The IP of the Mindustry server. This is optional and should only be changed when necessary. Default is
`127.0.0.1`. This shouldn't be changed directly unless you know what you're doing. Change this via the `update-ip`
command.
- `url` - The URL of the database.
- `user` - The database user to authenticate as.
- `password` - The password of the database user.
- `session-duration` - The maximum duration of a player session. Defaults to 12 hours. If the session exceeds the
maximum duration, then the user's session will become invalid when the user joins.

# Database Schema
For the database schema, see [this file](client/tables.sql).
