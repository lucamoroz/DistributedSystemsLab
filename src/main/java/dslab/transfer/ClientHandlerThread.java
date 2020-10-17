package dslab.transfer;

import dslab.protocols.dmpt.DMTPError;
import dslab.protocols.dmpt.DMTPServerHandler;
import dslab.protocols.dmpt.Email;
import dslab.protocols.dmpt.IDMTPServerHandler;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandlerThread extends Thread implements IDMTPServerHandler.Callback {

    private Socket socket;

    public ClientHandlerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        IDMTPServerHandler DMTPHandler = new DMTPServerHandler();

        try {

            // Communicate via DMTP to get emails
            DMTPHandler.handle(socket, (IDMTPServerHandler.Callback) this);

        }
        catch (DMTPError e) { System.out.println("DMTP error: " + e.getMessage()); }
        catch (IOException e) { System.out.println("IOException: " + e.getMessage()); } // write to client?
        finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    // Any Input/Output stream closes with socket
                    socket.close();
                } catch (IOException e) { }
            }
        }
    }

    @Override
    public void onSend(Email email) {
        // Queue one email per domain
        List<Email> sameDomainEmails = email.getEmailPerDomain();
        for (Email e : sameDomainEmails)
            System.out.println("\n" + e.toString() + "\n");
    }

    @Override
    public boolean validateRecipient(String recipient) {
        // todo according with domain
        return true;
    }
}
