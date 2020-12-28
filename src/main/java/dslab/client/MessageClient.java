package dslab.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.protocols.dmap.DMAPException;
import dslab.protocols.dmtp.Email;
import dslab.util.Config;

public class MessageClient implements IMessageClient, Runnable {
    final private Config config;
    final private Shell shell;
    private Socket dmtpSocket;
    private Socket dmapSocket;
    private DMTPHandlerThread dmtpHandler;
    private DMAPHandlerWrapper dmapHandler;
    private final BlockingQueue<Email> blockingQueue;
    private final String sender;

    /**
     * Creates a new client instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MessageClient(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        sender = config.getString("transfer.email");

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");

        blockingQueue = new ArrayBlockingQueue<>(40);
    }

    @Override
    public void run() {
        try {
            dmtpSocket = new Socket(config.getString("transfer.host"), config.getInt("transfer.port"));
        } catch (IOException e) {
            System.out.println("Error while creating client DMTP socket: " + e.getMessage());
            shutdown();
            return;
        }

        dmtpHandler = new DMTPHandlerThread(dmtpSocket, blockingQueue);
        if (!dmtpHandler.init()) {
            shutdown();
            return;
        }
        dmtpHandler.start();

        try {
            dmapSocket = new Socket(config.getString("mailbox.host"), config.getInt("mailbox.port"));
        } catch (IOException e) {
            System.out.println("Error while creating client DMAP socket: " + e.getMessage());
            shutdown();
            return;
        }

        dmapHandler = new DMAPHandlerWrapper(dmapSocket);
        if (!dmapHandler.init(config.getString("mailbox.user"), config.getString("mailbox.password"))) {
            shutdown();
            return;
        }

        shell.run();
    }

    @Override
    @Command
    public void inbox() {

    }

    @Override
    @Command
    public void delete(String id) {
        int parsedId;
        try {
            parsedId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            System.out.println("error given ID is not a valid integer");
            return;
        }

        dmapHandler.delete(parsedId);
    }

    @Override
    @Command
    public void verify(String id) {

    }

    @Override
    @Command
    public void msg(String to, String subject, String data) {
        // validate address format
        if (!Email.isValidAddress(to)) {
            shell.out().printf("error given recipient '%s' is not a valid mail address%n", to);
            return;
        }

        ArrayList<String> recipients = new ArrayList<>();
        recipients.add(to);
        Email email = new Email(sender, recipients, subject, data);
        blockingQueue.add(email);
    }

    @Override
    @Command
    public void shutdown() {
        // quit DMTP handler thread
        if (dmtpHandler != null) {
            dmtpHandler.interrupt();
            try {
                // wait for thread to quit
                dmtpHandler.join();
            } catch (InterruptedException e) {
                // noop
            }
        }

        // close DMTP socket
        if (dmtpSocket != null && !dmtpSocket.isClosed()) {
            try {
                dmtpSocket.close();
            } catch (IOException e) {
                shell.out().println("Error while closing DMTP socket: " + e.getMessage());
            }
        }

        if (dmapHandler != null) {
            try {
                dmapHandler.close();
            } catch (IOException | DMAPException e) {
                shell.out().println("Error while closing DMAP connection: " + e.getMessage());
            }
        }

        // close DMAP socket
        if (dmapSocket != null && !dmapSocket.isClosed()) {
            try {
                dmapSocket.close();
            } catch (IOException e) {
                shell.out().println("Error while closing DMAP socket: " + e.getMessage());
            }
        }

        shell.out().println("Bye bye");

        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
