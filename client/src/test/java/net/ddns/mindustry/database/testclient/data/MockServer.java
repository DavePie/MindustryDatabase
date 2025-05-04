package net.ddns.mindustry.database.testclient.data;

public record MockServer(String ip, int port, String name) {

    public static MockServer instance() {
        return new MockServer("127.0.0.1", 8312, "some_server");
    }
}
