package dslab.protocols.dmpt;

import java.io.IOException;
import java.net.Socket;

public interface IDMTPServerHandler {

    void handle(Socket socket, Callback callback) throws DMTPError, IOException;

    public interface Callback {
        public void onSend(Email email);
        public boolean validateRecipient(String recipient);
    }

}
