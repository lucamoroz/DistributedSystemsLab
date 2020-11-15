package dslab.mailbox.storage;

import dslab.protocols.dmtp.Email;

import java.util.Map;
import java.util.Set;

public interface IUserEmails {

    /**
     * Add an email to the user storage. Thread safe.
     * @param email email to add
     */
    void addEmail(Email email);

    /**
     * Delete email with id.
     * @param id the id of the email
     * @return the email deleted. Null if email not found
     */
    Email deleteEmail(Integer id);

    /**
     * Get user emails.
     * @return unmodifiable set of entries (id, email).
     */
    Set<Map.Entry<Integer, Email>> getUserEmails();

    Email getUserEmail(Integer id);
}
