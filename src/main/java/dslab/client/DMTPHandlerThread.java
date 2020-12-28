package dslab.client;

import dslab.protocols.dmtp.DMTPException;
import dslab.protocols.dmtp.Email;
import dslab.protocols.dmtp.client.DMTPClientHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class DMTPHandlerThread extends Thread {
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private DMTPClientHandler handler;
    private final BlockingQueue<Email> blockingQueue;

    public DMTPHandlerThread(Socket socket, BlockingQueue<Email> blockingQueue) {
        this.socket = socket;
        this.blockingQueue = blockingQueue;
    }

    public boolean init() {
        // Create resources for DMTP handler
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Could not create reader and writer for client DMTP handler thread: " + e.getMessage());
            return false;
        }

        handler = new DMTPClientHandler(socket, reader, writer);

        // Initialize handler and clean up on error
        try {
            handler.init();
        } catch (IOException | DMTPException e) {
            System.out.println("DMTP handler init threw exception: " + e.getMessage());
            closeResources();
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            try {
                Email email = blockingQueue.take();

                // Try to send email taken from blocking queue
                try {
                    // We only need to print out the first recipient since there can only be one defined in the msg command
                    handler.sendEmail(email, recipients -> System.out.println("error unknown recipient: " + recipients.get(0)));
                } catch (IOException | DMTPException e) {
                    System.out.printf("error sending '%s': %s%n", email.subject, e.getMessage());
                    continue;
                }

                System.out.println("ok");
            } catch (InterruptedException e) {
                // In case of interruption, email will be lost.
                // A better approach could permanently store email data and remove that data only after an email has
                // been processed.
                Thread.currentThread().interrupt();
            }
        }

        // cleanup all used resources
        try {
            handler.close();
        } catch (IOException | DMTPException e) {
            System.out.println("Error closing DMTP connection: " + e.getMessage());
        }

        closeResources();
    }

    private void closeResources() {
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
