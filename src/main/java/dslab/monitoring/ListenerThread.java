package dslab.monitoring;

import dslab.protocols.dmtp.Email;

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

                LogMessage log = null;
                try {
                    log = parseLogMessage(message);
                } catch (Exception e) {
                    System.out.println("Failed parsing: " + message + " for reason: " + e.getMessage());
                    continue;
                }

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
     * @param message String log message following the pattern <host>:<port> <email-address>
     * @return LogMessage
     */
    private LogMessage parseLogMessage(String message) {

        String[] addrEmail = message.split(" ");
        String address = addrEmail[0];
        String email = addrEmail[1];

        String host = address.substring(0, address.lastIndexOf(":"));
        int port = Integer.parseInt(address.substring(address.lastIndexOf(":") + 1));

        return new LogMessage(host, port, email);
    }
}
