package dslab.protocols.dmpt.client;

import dslab.protocols.dmpt.DMTPException;
import dslab.protocols.dmpt.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class DMTPClientHandler implements IDMTPClientHandler {

    Socket socket;
    BufferedReader reader;
    PrintWriter writer;

    public DMTPClientHandler(Socket socket, BufferedReader reader, PrintWriter writer) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void init() throws IOException, DMTPException {
        // Init protocol
        String message;
        String command;

        message = reader.readLine();
        if (message == null || !message.equals("ok DMTP"))
            throw new DMTPException("protocol error");

        command = "begin";
        executeOrThrowException(command, "ok", reader, writer);
    }

    @Override
    public void sendEmail(Email email, UnknownRecipientCallback callback) throws DMTPException, IOException {

        String message;
        String command;

        command = "to " + String.join(",", email.recipients);
        writer.println(command);
        message = reader.readLine();
        if (message == null) throw new DMTPException("protocol error");
        if (!message.equals("ok " + email.recipients.size())) {
            // todo this should only contain username not domain
            if (message.startsWith("error unknown recipient ") && message.split(" ").length == 4) {
                String unknownRecipients = message.split(" ")[3];
                callback.onUnknownRecipients(Arrays.asList(unknownRecipients.split(",")));
            } else {
                throw new DMTPException("protocol error");
            }
        }

        command = "from " + email.sender;
        executeOrThrowException(command, "ok", reader, writer);

        command = "subject " + email.subject;
        executeOrThrowException(command, "ok", reader, writer);

        command = "data " + email.subject;
        executeOrThrowException(command, "ok", reader, writer);

        command = "send";
        executeOrThrowException(command, "ok", reader, writer);

    }

    @Override
    public void close() throws IOException, DMTPException {
        String command = "quit";
        executeOrThrowException(command, "ok bye", reader, writer);
    }

    private static void executeOrThrowException(
            String command,
            String expectedAnswer,
            BufferedReader reader,
            PrintWriter writer
    ) throws IOException, DMTPException {

        writer.println(command);
        String message = reader.readLine();
        if (message == null) throw new DMTPException("protocol error");
        if (!message.equals(expectedAnswer)) {
            if (message.startsWith("error ")) {
                // Insert error message as content
                throw new DMTPException(message.substring(6));
            } else {
                throw new DMTPException("protocol error");
            }
        }
    }


}