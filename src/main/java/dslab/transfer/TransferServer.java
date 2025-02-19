package dslab.transfer;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.nameserver.INameserverRemote;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TransferServer implements ITransferServer, Runnable {

    final private Config config;
    final private Shell shell;
    private ServerSocket serverSocket;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        // Spawn a thread to accept incoming requests
        try {
            serverSocket = new ServerSocket(config.getInt("tcp.port"));
            new DMTPListenerThread(serverSocket, config, getRootNameserver()).start();

        } catch (IOException e) {
            throw new UncheckedIOException("Error while creating server socket", e);
        }

        shell.run();
    }

    @Override
    @Command
    public void shutdown() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                shell.out().println("Error while closing server socket: " + e.getMessage());
            }
        }

        shell.out().println("Bye bye");
        // Stop the Shell from reading from System.in by throwing a StopShellException
        throw new StopShellException();
    }

    /**
     * Get root nameserver.
     * In case of failure prints an error and returns null.
     */
    private INameserverRemote getRootNameserver() {
        INameserverRemote rootNameserver = null;
        try {
            Registry registry = LocateRegistry.getRegistry(
                    config.getString("registry.host"),
                    config.getInt("registry.port")
            );

            rootNameserver = (INameserverRemote) registry.lookup(config.getString("root_id"));

        } catch (RemoteException | NotBoundException e) {
            shell.out().println("Couldn't get root nameserver: " + e);
        }

        return rootNameserver;
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
