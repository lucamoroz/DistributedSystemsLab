package dslab.protocols.dmap;

import dslab.protocols.dmtp.Email;
import dslab.util.Keys;
import dslab.util.SecurityHelper;

import javax.crypto.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
        List<String> response = getResponseOrThrowException(command);
        // check for empty inbox
        if (response.get(0).startsWith("no emails")) {
            return emails;
        }

        for (String line : response) {
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
    public Email show(int id) throws IOException, DMAPException {
        String command = String.format("show %d", id);
        List<String> response = getResponseOrThrowException(command);
        Email email = new Email();

        for (String line : response) {
            String[] tagAndData = line.split(" ", 2);
            if (tagAndData.length < 1) {
                throw new DMAPException(MALFORMED_ANSWER);
            }

            switch (tagAndData[0]) {
                case "from": {
                    if (tagAndData.length != 2) {
                        throw new DMAPException(MALFORMED_ANSWER);
                    }

                    if (tagAndData[1].isBlank()) {
                        throw new DMAPException(MALFORMED_ANSWER);
                    }

                    email.sender = tagAndData[1];
                    break;
                }
                case "to": {
                    if (tagAndData.length != 2) {
                        throw new DMAPException(MALFORMED_ANSWER);
                    }

                    if (tagAndData[1].isBlank()) {
                        throw new DMAPException(MALFORMED_ANSWER);
                    }

                    String[] recipients = tagAndData[1].split(", *");

                    email.recipients = Arrays.asList(recipients);
                    break;
                }
                case "subject": {
                    if (tagAndData.length != 2) {
                        continue;
                    }

                    if (tagAndData[1].isBlank()) {
                        continue;
                    }

                    email.subject = tagAndData[1];

                    break;
                }
                case "data": {
                    if (tagAndData.length != 2) {
                        continue;
                    }

                    if (tagAndData[1].isBlank()) {
                        continue;
                    }

                    email.data = tagAndData[1];

                    break;
                }
                default: {
                    throw new DMAPException(UNEXPECTED_ANSWER);
                }
            }
        }

        if (email.sender == null || email.recipients == null || email.recipients.size() == 0) {
            throw new DMAPException(MALFORMED_ANSWER);
        }

        email.id = id;

        return email;
    }

    @Override
    public PublicKey stSecure() throws DMAPException, IOException, NoSuchAlgorithmException {
        String command = "startsecure";
        List<String> response = getResponseOrThrowException(command);
        if(response.size() == 1){
            String compID = response.get(0).split(" ", 2)[1];
            byte[] num = SecurityHelper.generateRandom(32);
            String message = "ok " + SecurityHelper.enocdeToBase64(num);
            PublicKey key = SecurityHelper.getPublicKey(compID);
            Cipher cipher = SecurityHelper.generateCipher("RSA/ECB/PKCS1Padding", Cipher.ENCRYPT_MODE, key);
            SecretKey aesKey = SecurityHelper.generateAESKey(16);
            byte[] cipherText;
            try {
                cipherText = cipher.doFinal(message.getBytes());
            }  catch (BadPaddingException | IllegalBlockSizeException e) {
                throw new DMAPException("Bad Padding/Illegal Blocksize");
            }
            String result = SecurityHelper.enocdeToBase64(cipherText);
            List<String> resp = getResponseOrThrowException(result);
            return null;
        }else{
            throw new DMAPException(NO_ANSWER);
        }
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

    private List<String> getResponseOrThrowException(String command) throws IOException, DMAPException {
        ArrayList<String> response = new ArrayList<>();
        writer.println(command);
        String message = reader.readLine();
        if (message == null) throw new DMAPException(NO_ANSWER);
        if (message.startsWith("error ")) {
            throw new DMAPException(message.substring(6));
        }

        response.add(message);
        while (reader.ready() && (message = reader.readLine()) != null) {
            response.add(message);
        }

        return response;
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
