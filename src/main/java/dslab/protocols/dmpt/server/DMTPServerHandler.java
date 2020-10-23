package dslab.protocols.dmpt.server;

import dslab.protocols.dmpt.DMTPException;
import dslab.protocols.dmpt.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

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

        if (message != null) {
            if (message.equals("begin")) {
                writer.println("ok");
            } else {
                throw new DMTPException("protocol error");
            }
        }
    }

    @Override
    public void receiveEmails(Callback callback) throws DMTPException, IOException {
        String request;

        // todo check reader.close() now

        while ((request = reader.readLine()) != null) {
            // todo handle subject / data containing spaces..............................
            String[] tokens = request.split(" ");

            if (tokens.length == 0) {
                throw new DMTPException("protocol error");
            }

            if (tokens.length == 2) {
                // if 2-words command
                switch (tokens[0]) {
                    case "to":

                        String[] recipients = tokens[1].split(",");
                        // Reject recipients list if any of them is invalid
                        for (String r : recipients) {
                            if (!Email.isValidAddress(r)) {
                                writer.println("error invalid address " + r);
                                break;
                            }
                            if (!callback.validateRecipient(r)) {
                                writer.println("error unknown recipient " + r);
                                break;
                            }
                        }
                        email.recipients = Arrays.asList(tokens[1].split(","));
                        writer.println("ok " + email.recipients.size());
                        break;

                    case "from":

                        if (Email.isValidAddress(tokens[1])) {
                            email.sender = tokens[1];
                            writer.println("ok");
                        }
                        else {
                            writer.println("error invalid address " + tokens[1]);
                        }

                        break;

                    case "subject":

                        email.subject = tokens[1];
                        writer.println("ok");
                        break;

                    case "data":

                        email.data = tokens[1];
                        writer.println("ok");
                        break;
                    default:
                        throw new DMTPException("protocol error");
                }
            } else {
                // if 1-word command
                switch (tokens[0]) {
                    case "send":

                        String missingParameter = findMissingParameter(email);
                        if (missingParameter == null) {
                            callback.consumeEmail(email);
                            writer.println("ok");
                            // todo activate this.email = new Email();
                        } else {
                            writer.println("error no " + missingParameter);
                        }
                        break;

                    case "quit":

                        writer.println("ok bye");
                        return;

                    default:
                        throw new DMTPException("protocol error");
                }
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
