package dslab.client;

import dslab.protocols.dmtp.DMTPException;
import dslab.protocols.dmtp.Email;
import dslab.protocols.dmtp.client.DMTPClientHandler;

import java.io.*;
import java.net.Socket;

public class DMTPHandlerWrapper {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private DMTPClientHandler handler;
    private final String host;
    private final int port;

    public DMTPHandlerWrapper(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private boolean init() {
        // create socket for DMTP connection
        try {
            socket = new Socket(host, port);
        } catch (IOException e) {
            System.out.println("error while creating client DMTP socket: " + e.getMessage());
            closeResources();
            return false;
        }

        // Create resources for DMTP handler
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("error not create reader and writer for client DMTP handler thread: " + e.getMessage());
            closeResources();
            return false;
        }

        handler = new DMTPClientHandler(socket, reader, writer);

        return true;
    }

    public void send(Email email, PrintStream printStream) {
        if (!init()) {
            return;
        }

        // Initialize handler and clean up on error
        try {
            handler.init();
        } catch (IOException | DMTPException e) {
            printStream.printf("error sending '%s': %s%n", email.subject, e.getMessage());
            closeResources();
            return;
        }

        // Try to send email taken from blocking queue
        try {
            // We only need to print out the first recipient since there can only be one defined in the msg command
            handler.sendEmail(email, recipients -> System.out.println("error unknown recipient: " + recipients.get(0)));
        } catch (IOException | DMTPException e) {
            printStream.printf("error sending '%s': %s%n", email.subject, e.getMessage());
            closeResources();
            return;
        }

        try {
            handler.close();
        } catch (IOException | DMTPException e) {
            printStream.println("error closing DMTP connection: " + e.getMessage());
            closeResources();
            return;
        }

        closeResources();
        printStream.println("ok");
    }

    public void close() {
        closeResources();
    }

    private void closeResources() {
        handler = null;

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
}
