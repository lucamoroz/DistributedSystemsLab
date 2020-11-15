package dslab.protocols.dmap;

import dslab.protocols.dmtp.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class DMAPServerHandler implements IDMAPServerHandler {

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private String loggedUser = null;

    public DMAPServerHandler(Socket socket, BufferedReader reader, PrintWriter writer) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void handleClient(Callback callback) throws IOException, DMAPException {

        writer.println("ok DMAP");
        String request;

        while ((request = reader.readLine())!= null) {

            String[] tokens = request.split(" ");

            switch (tokens[0]) {
                case "login":

                    if (this.loggedUser != null) {
                        writer.println("error already logged in");
                        break;
                    }
                    if (tokens.length == 3) {
                        String user = tokens[1];
                        String password = tokens[2];

                        if (!callback.userExists(user)) {
                            writer.println("error unknown user");
                            break;
                        }
                        if (!callback.isLoginValid(user, password)) {
                            writer.println("error wrong password");
                            break;
                        } else {
                            this.loggedUser = user;
                            writer.println("ok");
                        }

                    } else {
                        writer.println("error protocol error");
                        throw new DMAPException("protocol error");
                    }
                    break;
                case "list":

                    if (!request.equals("list")) {
                        writer.println("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        writer.println("error not logged in");
                        break;
                    }

                    List<Map.Entry<Integer, Email>> userEmails = callback.listUserEmails(this.loggedUser);
                    if (userEmails.isEmpty()) {
                        writer.println("no emails :(");
                    }else {
                        for (Map.Entry<Integer, Email> entry : userEmails) {
                            String header = entry.getKey().toString() + " " + entry.getValue().sender + " " + entry.getValue().subject;
                            writer.println(header);
                        }
                    }

                    break;
                case "show":

                    if (tokens.length != 2) {
                        writer.println("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        writer.println("error not logged in");
                        break;
                    }

                    Email email = callback.getEmail(this.loggedUser, Integer.parseInt(tokens[1]));
                    if (email != null) {
                        writer.println(email.printToDmtpFormat());
                    } else {
                        writer.println("error unknown message id");
                    }

                    break;
                case "delete":

                    if (tokens.length != 2) {
                        writer.println("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        writer.println("error not logged in");
                        break;
                    }

                    if (callback.deleteEmail(this.loggedUser, Integer.parseInt(tokens[1]))) {
                        writer.println("ok");
                    } else {
                        writer.println("error unknown message id");
                    }

                    break;
                case "logout":

                    if (!request.equals("logout")) {
                        writer.println("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        writer.println("error not logged in");
                        break;
                    }

                    this.loggedUser = null;
                    writer.println("ok");

                    break;
                case "quit":

                    writer.println("ok bye");
                    this.loggedUser = null;
                    return;

                default:
                    writer.println("error protocol error");
                    throw new DMAPException("protocol error");
            }
        }

        throw new DMAPException("protocol error");

    }
}
