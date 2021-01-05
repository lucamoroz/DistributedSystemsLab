package dslab.protocols.dmtp.client;

import dslab.protocols.dmtp.DMTPException;
import dslab.protocols.dmtp.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class DMTPClientHandler implements IDMTPClientHandler {

    private static final String UNEXPECTED_ANSWER = "protocol error unexpected answer";
    private static final String NO_ANSWER = "protocol error no answer";


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
        if (message == null)
            throw new DMTPException(NO_ANSWER);
        if (!message.equals("ok DMTP"))
            throw new DMTPException(UNEXPECTED_ANSWER);

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
        if (message == null) throw new DMTPException(NO_ANSWER);
        if (!message.equals("ok " + email.recipients.size())) {
            // Check if unknown recipients with content (i.e. length >24)
            if (message.startsWith("error unknown recipient ") && message.length() > 24) {
                String unknownRecipients = message.split(" ")[3];
                callback.onUnknownRecipients(Arrays.asList(unknownRecipients.split(",")));
            } else {
                throw new DMTPException(UNEXPECTED_ANSWER);
            }
        }

        command = "from " + email.sender;
        executeOrThrowException(command, "ok", reader, writer);

        command = "subject " + email.subject;
        executeOrThrowException(command, "ok", reader, writer);

        command = "data " + email.data;
        executeOrThrowException(command, "ok", reader, writer);

        command = "hash " + email.hash;
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
        if (message == null) throw new DMTPException(NO_ANSWER);
        if (!message.equals(expectedAnswer)) {
            if (message.startsWith("error ")) {
                // Insert error message as content
                throw new DMTPException(message.substring(6));
            } else {
                throw new DMTPException(UNEXPECTED_ANSWER);
            }
        }
    }


}
