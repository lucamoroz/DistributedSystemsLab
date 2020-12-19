package dslab.monitoring;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MonitoringServer implements IMonitoringServer {

    private final Config config;
    private final Shell shell;
    private DatagramSocket datagramSocket;
    private ListenerThread listenerThread;
    private final Map<String, Integer> nEmailsPerAddress;
    private final Map<String, Integer> nEmailsPerServer;


    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");

        this.nEmailsPerAddress = new ConcurrentHashMap<>();
        this.nEmailsPerServer = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        try {
            this.datagramSocket = new DatagramSocket(config.getInt("udp.port"));
            this.listenerThread = new ListenerThread(datagramSocket, nEmailsPerAddress, nEmailsPerServer);
            this.listenerThread.start();
        } catch (IOException e) {
            throw new RuntimeException("Cannot listen on UDP port.", e);
        }

        shell.run();
    }

    @Command
    @Override
    public void addresses() {
        nEmailsPerAddress.forEach((email, count) -> shell.out().println(email + " " + count));
    }

    @Command
    @Override
    public void servers() {
        nEmailsPerServer.forEach((server, count) -> shell.out().println(server + " " + count));
    }

    @Override
    @Command
    public void shutdown() {
        listenerThread.interrupt();
        if (datagramSocket != null)
            datagramSocket.close();

        shell.out().println("Bye bye");
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
