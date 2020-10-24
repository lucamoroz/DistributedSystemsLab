package dslab.mailbox.storage;

import dslab.protocols.dmpt.Email;

public interface IEmailStorage {
    IUserEmails getUserStorage(String user);
    void addEmail(Email email);
}
