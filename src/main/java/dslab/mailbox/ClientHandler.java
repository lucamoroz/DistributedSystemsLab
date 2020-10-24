package dslab.mailbox;

import dslab.mailbox.storage.IEmailStorage;
import dslab.mailbox.storage.IUserEmails;
import dslab.mailbox.storage.InMemoryEmailStorage;
import dslab.protocols.dmap.DMAPException;
import dslab.protocols.dmap.DMAPServerHandler;
import dslab.protocols.dmap.IDMAPServerHandler;
import dslab.protocols.dmpt.Email;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable, IDMAPServerHandler.Callback {

    private final Config userConfig;
    private final IEmailStorage emailStorage;

    private final Socket socket;

    public ClientHandler(Config usersConfig, Socket socket) {
        this.userConfig = usersConfig;
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

            IDMAPServerHandler handler = new DMAPServerHandler(socket, reader, writer);
            handler.handleClient(this);

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (DMAPException e) {
            System.out.println("DMAP exception: " + e.getMessage());
        } finally {
            closeResources(socket, reader, writer);
        }
    }

    @Override
    public Email getEmail(String loggedUser, int id) {
        IUserEmails userEmails = emailStorage.getUserStorage(loggedUser);
        return userEmails.getUserEmail(id);
    }

    @Override
    public boolean deleteEmail(String loggedUser, int id) {
        IUserEmails userEmails = emailStorage.getUserStorage(loggedUser);
        return userEmails.deleteEmail(id) != null;
    }

    @Override
    public boolean isLoginValid(String user, String password) {
        return userConfig.containsKey(user) && password.equals(userConfig.getString(user));
    }

    @Override
    public List<Map.Entry<Integer, Email>> listUserEmails(String loggedUser) {
        IUserEmails userEmails = emailStorage.getUserStorage(loggedUser);
        return List.copyOf(userEmails.getUserEmails());
    }

    @Override
    public boolean userExists(String user) {
        return userConfig.containsKey(user);
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
