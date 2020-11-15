package dslab.protocols.dmtp.client;


import dslab.protocols.dmtp.DMTPException;
import dslab.protocols.dmtp.Email;

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
