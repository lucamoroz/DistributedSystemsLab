package dslab.transfer;

import dslab.protocols.dmpt.DMTPException;
import dslab.protocols.dmpt.server.DMTPServerHandler;
import dslab.protocols.dmpt.Email;
import dslab.protocols.dmpt.server.IDMTPServerHandler;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class EmailProducer implements Runnable {

    private final Socket socket;
    private final BlockingQueue<Email> blockingQueue;
    private final Config transferConfig;

    public EmailProducer(Config transferConfig, Socket socket, BlockingQueue<Email> blockingQueue) {
        this.socket = socket;
        this.blockingQueue = blockingQueue;
        this.transferConfig = transferConfig;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            IDMTPServerHandler DMTPHandler = new DMTPServerHandler(socket, reader, writer);

            DMTPHandler.init();
            DMTPHandler.receiveEmails(new IDMTPServerHandler.Callback() {
                @Override
                public boolean consumeEmail(Email email) {
                    // In case of interruption, emails will be lost.
                    // A better approach could confirm the user that an email has been sent after storing some persistent
                    // information about the email, so even after a shutdown I could retry sending it.
                    // For the sake of simplicity, I decide to assume there wont be a failure after the email has been
                    // queued.
                    try {
                        blockingQueue.put(email);
                        System.out.println("\n" + email.toString() + "\n");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    }

                    sendLogMessage(socket.getLocalAddress(), socket.getLocalPort(), email);
                    return true;
                }

                @Override
                public boolean validateRecipient(String recipient) {
                    // Accept any recipient
                    return true;
                }
            });


        } catch (DMTPException e) {
            // Ignore dmtp exceptions caused by clients (users)
        } catch (IOException e) {
            System.out.println("IOException receiving email from client: " + e.getMessage());
        }
        finally {
            closeResources(socket, reader, writer);
        }
    }

    private void closeResources(Socket socket, BufferedReader reader, PrintWriter writer) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException exception) { }
        }

        if (writer != null) {
            writer.close();
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException exception) { }
        }
    }

    private void sendLogMessage(InetAddress localAddress, int port, Email email) {
        try {
            DatagramSocket socket = new DatagramSocket();
            String log = localAddress.getHostAddress() + ":" + port + " " + email.sender;

            DatagramPacket packet = new DatagramPacket(log.getBytes(), log.getBytes().length,
                    InetAddress.getByName(transferConfig.getString("monitoring.host")),
                    transferConfig.getInt("monitoring.port"));

            socket.send(packet);

        } catch (IOException e) {
            System.out.println("Couldn't send log message: " + e.getMessage());
        }
    }

}
