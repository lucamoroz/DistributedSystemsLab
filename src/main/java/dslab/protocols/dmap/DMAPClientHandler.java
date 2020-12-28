package dslab.protocols.dmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

public class DMAPClientHandler implements IDMAPClientHandler {

    private static final String UNEXPECTED_ANSWER = "protocol error unexpected answer";
    private static final String NO_ANSWER = "protocol error no answer";
    private static final String MALFORMED_ANSWER = "protocol error malformed answer";


    Socket socket;
    BufferedReader reader;
    PrintWriter writer;

    public DMAPClientHandler(Socket socket, BufferedReader reader, PrintWriter writer) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public void init(String username, String password) throws IOException, DMAPException {
        // Init protocol
        String message;
        String command;

        message = reader.readLine();
        if (message == null)
            throw new DMAPException(NO_ANSWER);
        if (!message.equals("ok DMAP"))
            throw new DMAPException(UNEXPECTED_ANSWER);

        command = String.format("login %s %s", username, password);
        executeOrThrowException(command, "ok");
    }

    @Override
    public HashMap<Integer, String[]> list() throws IOException, DMAPException {
        HashMap<Integer, String[]> emails = new HashMap<>();

        String command = "list";
        String response = getResponseOrThrowException(command);
        String[] lines = response.split("\n");

        for (String line : lines) {
            int id;
            try {
                id = Integer.parseInt(line.substring(0, 1));
            } catch (NumberFormatException e) {
                throw new DMAPException(MALFORMED_ANSWER);
            }

            String[] senderSubject = line.substring(2).split(" ");
            if (senderSubject.length < 1) {
                throw new DMAPException(MALFORMED_ANSWER);
            }

            emails.put(id, senderSubject);
        }

        return emails;
    }

    @Override
    public void delete(int id) throws IOException, DMAPException {
        String command = String.format("delete %d", id);
        executeOrThrowException(command, "ok");
    }

    @Override
    public void close() throws IOException, DMAPException {
        String command = "quit";
        executeOrThrowException(command, "ok bye");
    }

    private String getResponseOrThrowException(String command) throws IOException, DMAPException {
        writer.println(command);
        String message = reader.readLine();
        if (message == null) throw new DMAPException(NO_ANSWER);
        if (message.startsWith("error ")) {
            throw new DMAPException(message.substring(6));
        }

        return message;
    }

    private void executeOrThrowException(
            String command,
            String expectedAnswer
    ) throws IOException, DMAPException {

        writer.println(command);
        String message = reader.readLine();
        if (message == null) throw new DMAPException(NO_ANSWER);
        if (!message.equals(expectedAnswer)) {
            if (message.startsWith("error ")) {
                // Insert error message as content
                throw new DMAPException(message.substring(6));
            } else {
                throw new DMAPException(UNEXPECTED_ANSWER);
            }
        }
    }


}
