package dslab.mailbox;

import dslab.mailbox.storage.IEmailStorage;
import dslab.mailbox.storage.InMemoryEmailStorage;
import dslab.protocols.dmtp.DMTPException;
import dslab.protocols.dmtp.Email;
import dslab.protocols.dmtp.server.DMTPServerHandler;
import dslab.protocols.dmtp.server.IDMTPServerHandler;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EmailReceiver implements Runnable {

    private final Config userConfig;
    private final String domain;
    private final IEmailStorage emailStorage;

    private final Socket socket;

    public EmailReceiver(Config usersConfig, String domain, Socket socket) {
        this.userConfig = usersConfig;
        this.domain = domain;
        this.socket = socket;

        this.emailStorage = InMemoryEmailStorage.getEmailStorage();
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;

        try {

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            IDMTPServerHandler serverHandler = new DMTPServerHandler(socket, reader, writer);

            serverHandler.init();

            serverHandler.receiveEmails(new IDMTPServerHandler.Callback() {
                @Override
                public boolean consumeEmail(Email email) {
                    System.out.println("Email server received: " + email.toString());
                    emailStorage.addEmail(email);
                    return true;
                }

                @Override
                public boolean validateRecipient(String recipient) {
                    String usr = recipient.split("@")[0];
                    String dom = recipient.split("@")[1];

                    // If the recipient belongs to this domain: check user existence
                    return !domain.equals(dom) || userConfig.containsKey(usr);
                }
            });

        } catch (DMTPException e) {
            System.out.println("DMTP error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
        finally {
            closeResources(socket, reader, writer);
        }
    }

    private void closeResources(Socket socket, BufferedReader reader, PrintWriter writer) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException exception) { }
        }

        if (writer != null) {
            writer.close();
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException exception) { }
        }
    }
}
