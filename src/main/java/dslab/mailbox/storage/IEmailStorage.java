package dslab.mailbox.storage;

import dslab.protocols.dmtp.Email;

public interface IEmailStorage {
    IUserEmails getUserStorage(String user);
    void addEmail(Email email);
}
