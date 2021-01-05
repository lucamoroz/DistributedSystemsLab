package dslab.protocols.dmap;

import dslab.protocols.dmtp.Email;
import dslab.util.CipherDMAP;
import dslab.util.Keys;
import dslab.util.SecurityHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

public class DMAPServerHandler implements IDMAPServerHandler {

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private boolean isEncrypted = false;
    private CipherDMAP cipher;
    private String loggedUser = null;
    private final String componentId;

    public DMAPServerHandler(Socket socket, BufferedReader reader, PrintWriter writer, String componentId) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
        this.componentId = componentId;
    }

    private void initSecureCommunication() throws IOException, DMAPException {
        String request = "";
        PrivateKey key = SecurityHelper.getPrivateKey(componentId);
        cipher = new CipherDMAP("RSA/ECB/PKCS1Padding", key);

        while ((request = reader.readLine())!= null) {
            request = new String(cipher.decrypt(
                    SecurityHelper.decodeBase64(request)
            ));

            String[] tokens = request.split(" ");

            if (tokens.length == 4) {
                if(!tokens[0].equals("ok")) throw new DMAPException("error protocol error");
                byte[] secret = SecurityHelper.decodeBase64(tokens[2]);
                byte[] iv = SecurityHelper.decodeBase64(tokens[3]);
                cipher = new CipherDMAP(secret, iv,"AES/CTR/NoPadding");
                String answer = "ok " + tokens[1];
                answer = SecurityHelper.enocdeToBase64(
                        cipher.encrypt(answer.getBytes())
                );
                writer.println(answer);
                isEncrypted = true;
            }else if(tokens.length == 1 && tokens[0].equals("ok") && isEncrypted){
                break;
            }else{
                throw new DMAPException("error protocol");
            }
        }
    }

    private void sendMessage(String msg) throws DMAPException{
        if (isEncrypted) {
            writer.println(cipher.encryptString(msg));
        } else {
            writer.println(msg);
        }
    }

    @Override
    public void handleClient(Callback callback) throws IOException, DMAPException {

        writer.println("ok DMAP2.0");
        String request;

        while ((request = reader.readLine())!= null) {


            if(isEncrypted) {
                request = new String(cipher.decrypt(
                        SecurityHelper.decodeBase64(request)
                ));
            }

            String[] tokens = request.split(" ");

            switch (tokens[0]) {
                case "startsecure":
                    if(isEncrypted) {
                        sendMessage("A secure communication has already been started!");
                        break;
                    }
                    writer.println("ok " + componentId);
                    initSecureCommunication();
                    break;
                case "login":

                    if (this.loggedUser != null) {
                        sendMessage("error already logged in");
                        break;
                    }
                    if (tokens.length == 3) {
                        String user = tokens[1];
                        String password = tokens[2];

                        if (!callback.userExists(user)) {
                            sendMessage("error unknown user");
                            break;
                        }
                        if (!callback.isLoginValid(user, password)) {
                            sendMessage("error wrong password");
                            break;
                        } else {
                            this.loggedUser = user;
                            sendMessage("ok");
                        }

                    } else {
                        sendMessage("error protocol error");
                        throw new DMAPException("protocol error");
                    }
                    break;
                case "list":

                    if (!request.equals("list")) {
                        sendMessage("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        sendMessage("error not logged in");
                        break;
                    }

                    List<Map.Entry<Integer, Email>> userEmails = callback.listUserEmails(this.loggedUser);
                    if (userEmails.isEmpty()) {
                        sendMessage("no emails :(");
                    }else {
                        for (Map.Entry<Integer, Email> entry : userEmails) {
                            String header = entry.getKey().toString() + " " + entry.getValue().sender + " " + entry.getValue().subject;
                            sendMessage(header);
                        }
                    }

                    break;
                case "show":

                    if (tokens.length != 2) {
                        sendMessage("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        sendMessage("error not logged in");
                        break;
                    }

                    Email email = callback.getEmail(this.loggedUser, Integer.parseInt(tokens[1]));
                    if (email != null) {
                        sendMessage(email.printToDmtpFormat());
                    } else {
                        sendMessage("error unknown message id");
                    }

                    break;
                case "delete":

                    if (tokens.length != 2) {
                        sendMessage("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        sendMessage("error not logged in");
                        break;
                    }

                    if (callback.deleteEmail(this.loggedUser, Integer.parseInt(tokens[1]))) {
                        sendMessage("ok");
                    } else {
                        sendMessage("error unknown message id");
                    }

                    break;
                case "logout":

                    if (!request.equals("logout")) {
                        sendMessage("error protocol error");
                        throw new DMAPException("error protocol error");
                    }
                    if (this.loggedUser == null) {
                        sendMessage("error not logged in");
                        break;
                    }

                    this.loggedUser = null;
                    sendMessage("ok");

                    break;
                case "quit":
                    sendMessage("ok bye");
                    this.isEncrypted = false;
                    this.loggedUser = null;
                    return;

                default:
                    sendMessage("error protocol error");
                    throw new DMAPException("protocol error");
            }
        }

        throw new DMAPException("protocol error");

    }
}
