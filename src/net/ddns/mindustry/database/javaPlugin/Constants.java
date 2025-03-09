package net.ddns.mindustry.database.javaPlugin;

import net.ddns.mindustry.segment.MainKt;
import net.ddns.mindustry.segment.TextInputHandler;

public class Constants {
    public static final String SERVER_IP_PORT_ERROR = "The server configuration is invalid. Ensure that the server" +
            " configuration in both the database and the config are valid.";

    public static final TextInputHandler textInputHandler = MainKt.getTextInputHandler();
}
