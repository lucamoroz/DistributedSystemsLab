package dslab.monitoring;

import dslab.protocols.dmpt.Email;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;

public class ListenerThread extends Thread {

    private final DatagramSocket datagramSocket;
    final Map<String, Integer> nEmailsPerAddress;
    final Map<String, Integer> nEmailsPerServer;

    public ListenerThread(DatagramSocket datagramSocket, Map<String, Integer> nEmailsPerAddress, Map<String, Integer> nEmailsPerServer) {
        this.datagramSocket = datagramSocket;
        this.nEmailsPerAddress = nEmailsPerAddress;
        this.nEmailsPerServer = nEmailsPerServer;
    }

    @Override
    public void run() {

        DatagramPacket packet;
        byte[] buffer;

        try {

            while (!Thread.currentThread().isInterrupted()) {
                buffer = new byte[1024];
                packet = new DatagramPacket(buffer, buffer.length);

                datagramSocket.receive(packet);

                String message = new String(packet.getData());

                if (!isLogValid(message)) continue;

                LogMessage log = parseLogMessage(message);

                Integer prev = nEmailsPerAddress.getOrDefault(log.emailAddress, 0);
                nEmailsPerAddress.put(log.emailAddress, prev + 1);

                String hostKey = String.join(":", log.host, log.port.toString());
                prev = nEmailsPerServer.getOrDefault(hostKey, 0);
                nEmailsPerServer.put(hostKey, prev + 1);
            }

        } catch (SocketException e) {
            if (!Thread.currentThread().isInterrupted())
                System.out.println("SocketException while waiting for/handling packets: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }
    }

    /**
     * @param message candidate log message
     * @return true if the message follows the pattern <host>:<port> <email-address>
     */
    private boolean isLogValid(String message) {
        String[] tokens = message.split(" ");

        if (tokens.length != 2) return false;

        if (!Email.isValidAddress(tokens[1])) return false;

        String[] hostPort = tokens[0].split(":");
        if (hostPort.length != 2) return false;

        try {
            Integer.parseInt(hostPort[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    /**
     * @param message String log message following the pattern <host>:<port> <email-address>
     * @return LogMessage
     */
    private LogMessage parseLogMessage(String message) {
        String[] addrEmail = message.split(" ");
        String email = addrEmail[1];
        String[] hostPort = addrEmail[0].split(":");
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        return new LogMessage(host, port, email);
    }
}
