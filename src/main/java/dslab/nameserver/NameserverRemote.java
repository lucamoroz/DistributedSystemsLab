package dslab.nameserver;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NameserverRemote implements INameserverRemote {
    private final ConcurrentMap<String, INameserverRemote> subdomains;
    private final ConcurrentMap<String, String> mailboxServerAddress;

    public NameserverRemote() {
        this.subdomains = new ConcurrentHashMap<>();
        this.mailboxServerAddress = new ConcurrentHashMap<>();
    }

    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver)
            throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        if (isDomainInvalid(domain))
            throw new InvalidDomainException(domain + " has syntax error(s).");

        String[] zones = domain.split("\\.");
        int nZones = zones.length;

        if (nZones == 1) {
            // Add new subdomain to this nameserver
            if (subdomains.containsKey(domain))
                throw new AlreadyRegisteredException(domain + " is already registered.");

            subdomains.put(domain, nameserver);
        } else {
            // Forward request to subdomain
            if (!subdomains.containsKey(zones[nZones-1]))
                throw new InvalidDomainException("Nameserver for " + domain + " not found");

            INameserverRemote nextNameserver = subdomains.get(zones[nZones-1]);
            nextNameserver.registerNameserver(domain.substring(0, domain.lastIndexOf(".")), nameserver);
        }

    }

    @Override
    public void registerMailboxServer(String domain, String address)
            throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        if (isDomainInvalid(domain))
            throw new InvalidDomainException(domain + " has syntax error(s).");

        String[] zones = domain.split("\\.");
        int nZones = zones.length;

        if (nZones == 1) {
            // Add new mailbox address to this nameserver
            if (mailboxServerAddress.containsKey(domain))
                throw new AlreadyRegisteredException(domain + " is already registered.");

            mailboxServerAddress.put(domain, address);

        } else {
            // Forward request to subdomain
            if (!subdomains.containsKey(zones[nZones-1]))
                throw new InvalidDomainException("Nameserver for " + domain + " not found");

            INameserverRemote nextNameserver = subdomains.get(zones[nZones-1]);
            nextNameserver.registerMailboxServer(domain.substring(0, domain.lastIndexOf(".")), address);
        }
    }

    @Override
    public INameserverRemote getNameserver(String zone) throws RemoteException {
        return subdomains.getOrDefault(zone, null);
    }

    @Override
    public String lookup(String domain) throws RemoteException {
        return mailboxServerAddress.getOrDefault(domain, null);
    }

    public List<String> getNameservers() {
        return new ArrayList<>(subdomains.keySet());
    }

    public List<String> getAddresses() {
        return new ArrayList<>(mailboxServerAddress.keySet());
    }

    private boolean isDomainInvalid(String domain) {
        if (domain == null || domain.isEmpty())
            return true;

        for(String zone : domain.split("\\.")) {
            if (!zone.matches("^[a-zA-Z]*$"))
                return true;
        }

        return false;
    }
}
