package dslab.protocols.dmtp.server;

import dslab.protocols.dmtp.DMTPException;
import dslab.protocols.dmtp.Email;

import java.io.IOException;

public interface IDMTPServerHandler {

    void init() throws DMTPException, IOException;
    void receiveEmails(Callback callback) throws DMTPException, IOException;

    interface Callback {

        /**
         * Called when the client requests a "send" command.
         * @param email the Email composed by the client.
         * @return true if the email has been successfully processed, false otherwise
         */
        boolean consumeEmail(Email email);

        /**
         * Called to validate a recipient (e.g. validate the domain or the existence).
         * @param recipient recipient of the email.
         * @return true if valid and the recipient can be assigned, false otherwise.
         */
        boolean validateRecipient(String recipient);
    }

}
