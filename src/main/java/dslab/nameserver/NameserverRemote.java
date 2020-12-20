package dslab.nameserver;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameserverRemote implements INameserverRemote {
    private final ConcurrentMap<String, INameserverRemote> subdomains;
    private final ConcurrentMap<String, String> mailboxServerAddresses;

    public NameserverRemote() {
        this.subdomains = new ConcurrentHashMap<>();
        this.mailboxServerAddresses = new ConcurrentHashMap<>();
    }

    @Override
    public void registerNameserver(String domain, INameserverRemote nameserver)
            throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        // domain must be case insensitive
        domain = domain.toLowerCase();

        if (isDomainInvalid(domain))
            throw new InvalidDomainException(domain + " has syntax error(s).");

        String[] zones = domain.split("\\.");
        int nZones = zones.length;

        if (nZones == 1) {
            // Add new subdomain to this nameserver
            if (subdomains.containsKey(domain))
                throw new AlreadyRegisteredException(domain + " is already registered.");

            System.out.println("Registering nameserver for zone " + domain);
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

        // domain must be case insensitive
        domain = domain.toLowerCase();

        if (isDomainInvalid(domain) || isAddressInvalid(address))
            throw new InvalidDomainException(domain + " or " + address + " have syntax error(s).");

        String[] zones = domain.split("\\.");
        int nZones = zones.length;

        if (nZones == 1) {
            // Add new mailbox address to this nameserver
            if (mailboxServerAddresses.containsKey(domain))
                throw new AlreadyRegisteredException(domain + " is already registered.");

            System.out.println("Registering mailbox server " + address + " for zone " + zones[nZones-1]);
            mailboxServerAddresses.put(domain, address);

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
        // zones must be case insensitive
        zone = zone.toLowerCase();
        System.out.println("Nameserver for " + zone + " requested.");
        return subdomains.getOrDefault(zone, null);
    }

    @Override
    public String lookup(String domain) throws RemoteException {
        // domain must be case insensitive
        domain = domain.toLowerCase();
        System.out.println("Mailbox address for " + domain + " requested.");
        return mailboxServerAddresses.getOrDefault(domain, null);
    }

    public List<String> getNameservers() {
        return new ArrayList<>(subdomains.keySet());
    }

    public Map<String, String> getMailboxServerAddresses() {
        return Collections.unmodifiableMap(this.mailboxServerAddresses);
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

    private boolean isAddressInvalid(String address) {
        Pattern p = Pattern.compile("^\\s*(.*?):(\\d+)\\s*$");
        Matcher m = p.matcher(address);
        if (m.matches())
            return false;
        else
            return true;
    }
}
