package dslab.monitoring;

public class LogMessage {

    final String host;
    final Integer port;
    final String emailAddress;

    public LogMessage(String host, Integer port, String emailAddress) {
        this.host = host;
        this.port = port;
        this.emailAddress = emailAddress;
    }

    @Override
    public String toString() {
        return "LogMessage{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", emailAddress='" + emailAddress + '\'' +
                '}';
    }
}
