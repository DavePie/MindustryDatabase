package net.ddns.mindustry.database.testclient.data;

public record MockAccount(String username, char[] password, String ip, String uuid) {

    public static MockAccount instance() {
        return new MockAccount("username",
                new char[] {'H', 'I', '!', ':', 'D'},
                "127.0.0.1",
                "f82ebecb-ec7f-4da6-bf25-ab2948cb3377");
    }
}