package dslab.protocols.dmpt.client;


import dslab.protocols.dmpt.DMTPException;
import dslab.protocols.dmpt.Email;
import dslab.protocols.dmpt.UnknownRecipientException;

import java.io.IOException;

public interface IDMTPClientHandler {

    void init() throws DMTPException, IOException;
    void sendEmail(Email email) throws DMTPException, UnknownRecipientException, IOException;
    void close() throws DMTPException, IOException;

}
