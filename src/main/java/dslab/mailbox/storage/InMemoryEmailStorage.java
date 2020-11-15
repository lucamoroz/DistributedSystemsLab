package dslab.mailbox.storage;

import dslab.protocols.dmtp.Email;
import dslab.util.Config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryEmailStorage implements IEmailStorage {

    private static IEmailStorage instance = null;
    private final ConcurrentMap<String, IUserEmails> userEmails;
    private final String domain;

    // Singleton
    private InMemoryEmailStorage(Config config) {
        Config userConfig = new Config(config.getString("users.config"));
        this.userEmails = new ConcurrentHashMap<>();
        for (String usr : userConfig.listKeys()) {
            userEmails.put(usr, new InMemoryUserEmails());
        }
        this.domain = config.getString("domain");
    }

    public static IEmailStorage getEmailStorage() {
        if (instance == null) {
            throw new RuntimeException("Storage not initialized.");
        }

        return instance;
    }

    public static void init(Config config) {
        if (instance == null) {
            instance = new InMemoryEmailStorage(config);
        }
    }

    @Override
    public IUserEmails getUserStorage(String user) {
        return userEmails.get(user);
    }

    @Override
    public void addEmail(Email email) {
        // Add email to each recipient that belongs to this server
        for (String recipient : email.recipients) {
            if (this.domain.equals(Email.getDomain(recipient))) {
                IUserEmails recipientStorage = getUserStorage(Email.getUser(recipient));
                if (recipientStorage != null) recipientStorage.addEmail(email);
            }
        }
    }
}
