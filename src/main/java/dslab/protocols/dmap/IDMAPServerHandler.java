package dslab.protocols.dmap;

import dslab.protocols.dmpt.Email;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IDMAPServerHandler {
    void handleClient(Callback callback) throws IOException, DMAPException;

    interface Callback {
        boolean deleteEmail(String loggedUser, int id);
        Email getEmail(String loggedUser, int id);
        List<Map.Entry<Integer, Email>> listUserEmails(String loggedUser);
        boolean userExists(String user);
        boolean isLoginValid(String user, String password);
    }
}
