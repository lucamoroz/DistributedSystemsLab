package dslab.protocols.dmpt.client;


import dslab.protocols.dmpt.DMTPException;
import dslab.protocols.dmpt.Email;

import java.io.IOException;
import java.util.List;

public interface IDMTPClientHandler {

    void init() throws DMTPException, IOException;
    void sendEmail(Email email, UnknownRecipientCallback callback) throws DMTPException, IOException;
    void close() throws DMTPException, IOException;

    @FunctionalInterface
    interface UnknownRecipientCallback {
        void onUnknownRecipients(List<String> recipients);
    }
}
