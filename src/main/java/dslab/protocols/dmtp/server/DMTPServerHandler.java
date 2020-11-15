package dslab.protocols.dmtp.server;

import dslab.protocols.dmtp.DMTPException;
import dslab.protocols.dmtp.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DMTPServerHandler implements IDMTPServerHandler {

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    private Email email;

    public DMTPServerHandler(Socket socket, BufferedReader reader, PrintWriter writer) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
        this.email = new Email();
    }

    @Override
    public void init() throws DMTPException, IOException {
        // Begin protocol
        writer.println("ok DMTP");
        String message = reader.readLine();

        if (message != null && message.equals("begin")) {
            writer.println("ok");
        } else {
            writer.println("error protocol error");
            throw new DMTPException("protocol error");
        }
    }

    @Override
    public void receiveEmails(Callback callback) throws DMTPException, IOException {
        String request;

        while ((request = reader.readLine()) != null) {
            String[] tokens = request.split(" ");

            if (tokens.length == 0) {
                writer.println("error protocol error");
                throw new DMTPException("protocol error");
            }

            switch (tokens[0]) {
                case "to":

                    if (tokens.length != 2) {
                        writer.println("error protocol error");
                        throw new DMTPException("protocol error");
                    }

                    String[] recipients = tokens[1].split(",");
                    // Validate and check unknown recipients
                    List<String> unknownRecipients = new LinkedList<>();

                    for (String r : recipients) {
                        if (!Email.isValidAddress(r)) {
                            writer.println("error invalid recipient");
                            throw new DMTPException("invalid recipient");
                        }
                        if (!callback.validateRecipient(r)) {
                            unknownRecipients.add(r);
                        }
                    }

                    if (!unknownRecipients.isEmpty()) {
                        // Report unknown recipients removing domain
                        List<String> unknownRecNoDomain = unknownRecipients.stream()
                                .map(Email::getUser)
                                .filter(user -> !Objects.isNull(user))
                                .collect(Collectors.toList());
                        writer.println("error unknown recipient " + String.join(",", unknownRecNoDomain));
                    } else {
                        writer.println("ok " + recipients.length);
                    }

                    email.recipients = Arrays.asList(recipients);
                    break;

                case "from":

                    if (tokens.length != 2) {
                        writer.println("error protocol error");
                        throw new DMTPException("protocol error");
                    }

                    if (Email.isValidAddress(tokens[1])) {
                        email.sender = tokens[1];
                        writer.println("ok");
                    } else {
                        writer.println("error invalid address " + tokens[1]);
                    }
                    break;

                case "subject":

                    // subject starts at index 8
                    email.subject = request.substring(8);
                    writer.println("ok");
                    break;

                case "data":

                    // data starts at index 5
                    email.data = request.substring(5);
                    writer.println("ok");
                    break;

                case "send":

                    if (!request.equals("send")) {
                        writer.println("error protocol error");
                        throw new DMTPException("protocol error");
                    }

                    String missingParameter = findMissingParameter(email);
                    if (missingParameter == null) {
                        if (callback.consumeEmail(email)) {
                            writer.println("ok");
                            this.email = new Email();
                        } else   {
                            writer.println("error consuming email");
                        }

                    } else {
                        writer.println("error no " + missingParameter);
                    }
                    break;

                case "quit":

                    if (!request.equals("quit")) {
                        writer.println("error protocol error");
                        throw new DMTPException("protocol error");
                    }
                    writer.println("ok bye");
                    return;

                default:
                    writer.println("error protocol error");
                    throw new DMTPException("protocol error");
            }
        }

        throw new DMTPException("Stream ended before completion.");
    }

    /**
     * Find missing property in email.
     * @param email email
     * @return name of missing parameter, null if none.
     */
    private static String findMissingParameter(Email email) {
        if (email.sender == null)
            return "sender";
        else if (email.recipients == null)
            return "recipient";
        else if (email.subject == null)
            return "subject";
        else if (email.data == null)
            return "data";

        return null;
    }


}
