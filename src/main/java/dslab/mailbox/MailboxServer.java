package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.storage.InMemoryEmailStorage;
import dslab.nameserver.AlreadyRegisteredException;
import dslab.nameserver.INameserverRemote;
import dslab.nameserver.InvalidDomainException;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MailboxServer implements IMailboxServer, Runnable {

    final private Config config;
    final private Shell shell;
    private String componentId;
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
        this.componentId = componentId;

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");

        InMemoryEmailStorage.init(config);
    }

    @Override
    public void run() {
        registerMailDomain();

        try {
            dmtpServerSocket = new ServerSocket(config.getInt("dmtp.tcp.port"));
            dmapServerSocket = new ServerSocket(config.getInt("dmap.tcp.port"));

            new DMTPListenerThread(config, dmtpServerSocket).start();
            new DMAPListenerThread(config, dmapServerSocket, componentId).start();


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

    /**
     * Register the mail domain to the nameservers.
     * In case of failure only an error is printed and the server.
     */
    private void registerMailDomain() {

        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            int port = config.getInt("dmtp.tcp.port");

            Registry registry = LocateRegistry.getRegistry(
                    config.getString("registry.host"),
                    config.getInt("registry.port")
            );

            INameserverRemote rootNameserver =
                    (INameserverRemote) registry.lookup(config.getString("root_id"));

            rootNameserver.registerMailboxServer(config.getString("domain"), host + ":" + port);
        } catch (RemoteException | NotBoundException e) {
            shell.out().println("Couldn't get root nameserver: " + e);
        } catch (InvalidDomainException | AlreadyRegisteredException e) {
            shell.out().println("Couldn't register mail domain: " + e);
        } catch (UnknownHostException e) {
            shell.out().println("Couldn't get host address: " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
