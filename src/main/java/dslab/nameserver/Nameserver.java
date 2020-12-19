package dslab.nameserver;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.stream.Collectors;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

public class Nameserver implements INameserver {

    private final Config config;
    private final Shell shell;
    private final NameserverRemote managedNameserverRemote;
    private Registry registry;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;

        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");

        this.managedNameserverRemote = new NameserverRemote();
    }

    @Override
    public void run() {
        try {
            if (config.containsKey("domain")) {
                registry = LocateRegistry.getRegistry(
                        config.getString("registry.host"),
                        config.getInt("registry.port")
                );

                INameserverRemote rootNameserverRemote =
                        (INameserverRemote) registry.lookup(config.getString("root_id"));

                INameserverRemote nameserverRemote =
                        (INameserverRemote) UnicastRemoteObject.exportObject(managedNameserverRemote, 0);
                rootNameserverRemote.registerNameserver(config.getString("domain"), nameserverRemote);

            } else {
                // Case root nameserver
                registry = LocateRegistry.createRegistry(config.getInt("registry.port"));

                INameserverRemote nameserverRemote =
                        (INameserverRemote) UnicastRemoteObject.exportObject(managedNameserverRemote, 0);

                registry.bind(config.getString("root_id"), nameserverRemote);
            }

        } catch (RemoteException e) {
            throw new RuntimeException("Error while starting server.", e);
        } catch (NotBoundException e) {
            throw new RuntimeException("Error while looking for root nameserver.", e);
        } catch (InvalidDomainException e) {
            throw new RuntimeException("Error while registering nameserver.", e);
        } catch (AlreadyRegisteredException e) {
            throw new RuntimeException("Error while registering nameserver.", e);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException("Error while binding remote object to registry.", e);
        }

        shell.run();
    }

    @Override
    @Command
    public void nameservers() {
        List<String> nameservers =
                managedNameserverRemote.getNameservers().stream().sorted().collect(Collectors.toList());

        if (nameservers.isEmpty()) {
            shell.out().println("None");
            return;
        }

        for (int i=1; i<=nameservers.size(); i++)
            shell.out().println(i + ". " + nameservers.get(i-1));
    }

    @Override
    @Command
    public void addresses() {
        List<String> addresses =
                managedNameserverRemote.getAddresses().stream().sorted().collect(Collectors.toList());

        if (addresses.isEmpty()) {
            shell.out().println("None");
            return;
        }

        for (int i=1; i<=addresses.size(); i++)
            shell.out().println(i + ". " + addresses.get(i-1));
    }

    @Override
    @Command
    public void shutdown() {
        // Unbind remote object and shutdown registry if this is the root nameserver
        if (!config.containsKey("domain")) {
            try {
                registry.unbind(config.getString("root_id"));
                UnicastRemoteObject.unexportObject(registry, true);
            } catch (Exception e) {
                System.err.println("Error while unbinding object: " + e.getMessage());
            }
        }

        try {
            UnicastRemoteObject.unexportObject(managedNameserverRemote, true);
        } catch (NoSuchObjectException e) {
            System.err.println("Error while unexporting object: " + e.getMessage());
        }

        shell.out().println("Bye bye");
        // Stop the Shell from reading from System.in by throwing a StopShellException
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }

}
