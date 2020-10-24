package dslab.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.storage.InMemoryEmailStorage;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable {

    final private Config config;
    final private Shell shell;
    private ServerSocket dmapServerSocket;
    private ServerSocket dmtpServerSocket;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");

        InMemoryEmailStorage.init(config);
    }

    @Override
    public void run() {

        try {
            dmtpServerSocket = new ServerSocket(config.getInt("dmtp.tcp.port"));
            dmapServerSocket = new ServerSocket(config.getInt("dmap.tcp.port"));

            new DMTPListenerThread(config, dmtpServerSocket).start();
            new DMAPListenerThread(config, dmapServerSocket).start();


        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
        }

        shell.run();
    }

    @Override
    @Command
    public void shutdown() {

        if (dmtpServerSocket != null) {
            try {
                dmtpServerSocket.close();
            } catch (IOException e) {
                shell.out().println("Error while closing server socket: " + e.getMessage());
            }
        }
        if (dmapServerSocket != null) {
            try {
                dmapServerSocket.close();
            } catch (IOException e) {
                shell.out().println("Error while closing server socket: " + e.getMessage());
            }
        }

        shell.out().println("Bye bye");
        // Stop the Shell from reading from System.in by throwing a StopShellException
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
