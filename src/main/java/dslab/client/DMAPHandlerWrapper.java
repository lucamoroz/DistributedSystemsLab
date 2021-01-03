package dslab.client;

import dslab.protocols.dmap.DMAPClientHandler;
import dslab.protocols.dmap.DMAPException;
import dslab.protocols.dmtp.Email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class DMAPHandlerWrapper {
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private DMAPClientHandler handler;

    public DMAPHandlerWrapper(Socket socket) {
        this.socket = socket;
    }

    public boolean init(String username, String password) {
        // Create resources for DMAP handler
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Could not create reader and writer for client DMTP handler thread: " + e.getMessage());
            return false;
        }

        handler = new DMAPClientHandler(socket, reader, writer);

        // Initialize handler and clean up on error
        try {
            handler.init(username, password);
        } catch (IOException | DMAPException e) {
            System.out.println("DMAP handler init threw exception: " + e.getMessage());
            closeResources();
            return false;
        }

        return true;
    }

    public void printInbox() {
        HashMap<Integer, String[]> emailListing;
        try {
            emailListing = handler.list();
        } catch (IOException | DMAPException e) {
            System.out.println("error receiving email list: " + e.getMessage());
            return;
        }

        if (emailListing.size() == 0) {
            System.out.println("No emails to display");
            return;
        }

        ArrayList<Email> emails = new ArrayList<>();
        for (int id : emailListing.keySet()) {
            Email email;
            try {
                email = handler.show(id);
            } catch (IOException | DMAPException e) {
                System.out.println("error receiving email list: " + e.getMessage());
                return;
            }

            emails.add(email);
        }

        for (Email email : emails) {
            System.out.printf("%s%n", email.prettyPrint());
        }
    }

    public Email getEmail(int id) {
        Email email;
        try {
            email = handler.show(id);
        } catch (IOException | DMAPException e) {
            System.out.printf("error could not get email with id %d for verification: %s%n", id, e.getMessage());
            return null;
        }

        return email;
    }

    public void delete(int id) {
        try {
            handler.delete(id);
        } catch (IOException | DMAPException e) {
            System.out.printf("error could not delete message with id %d: %s%n", id, e.getMessage());
        }

        System.out.println("ok");
    }

    public void close() throws IOException, DMAPException {
        handler.close();
    }

    private void closeResources() {
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