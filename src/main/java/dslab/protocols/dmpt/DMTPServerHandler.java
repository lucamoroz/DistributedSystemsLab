package dslab.protocols.dmpt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class DMTPServerHandler implements IDMTPServerHandler {

    private Socket socket;

    private Email email;

    public DMTPServerHandler() {
        this.email = new Email();
    }

    @Override
    public void handle(Socket socket, Callback callback) throws DMTPError, IOException {
        this.socket = socket;

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

        String request;

        // todo check reader.close() now

        // Begin protocol
        writer.println("ok DMTP");
        request = reader.readLine();

        if (request != null) {
            if (request.equals("begin")) {
                writer.println("ok");
            } else {
                throw new DMTPError("Protocol error: begin message expected");
            }
        }

        while ((request = reader.readLine()) != null) {
            String[] tokens = request.split(" ");

            if (tokens.length > 2 || tokens.length == 0) {
                throw new DMTPError("Protocol error: expected one or two tokens");
            }

            switch (tokens[0]) {
                case "to":

                    String[] recipients = tokens[1].split(",");
                    for (String r : recipients) {
                        if (!callback.validateRecipient(r)) {
                            writer.println("error unknown recipient " + r);
                            break;
                        }
                    }
                    email.recipients = Arrays.asList(tokens[1].split(","));
                    writer.println("ok " + email.recipients.size());
                    break;

                case "from":

                    email.sender = tokens[1];
                    writer.println("ok");
                    break;

                case "subject":

                    email.subject = tokens[1];
                    writer.println("ok");
                    break;

                case "data":

                    email.data = tokens[1];
                    writer.println("ok");
                    break;

                case "send":

                    String missingParameter = findMissingParameter(email);
                    if (missingParameter == null) {
                        callback.onSend(email);
                        writer.println("ok");
                        this.email = new Email(); // todo ask if this behavior is ok
                    } else {
                        writer.println("error no " + missingParameter);
                    }

                    break;

                case "quit":

                    writer.println("ok bye");
                    break;

                default:
                    writer.println("error protocol error");
                    throw new DMTPError("Protocol error: unknown command.");
            }

        }

        throw new DMTPError("Stream ended before completion.");
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
