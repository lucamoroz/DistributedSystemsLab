package dslab.protocols.dmap;

import dslab.protocols.dmtp.Email;
import dslab.util.CipherDMAP;
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
    private static final String WRONG_ANSWER = "protocol error wrong answer while establishing connection";


    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private boolean isEncrypted = false;

    private CipherDMAP aesCipher;

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
    public void stSecure() throws DMAPException, IOException {
        String command = "startsecure";
        List<String> response = getResponseOrThrowException(command);
        if(response.size() == 1){
            String compID = response.get(0).split(" ", 2)[1];
            byte[] num = SecurityHelper.generateRandom(32);
            String numBase64 = SecurityHelper.enocdeToBase64(num);
            PublicKey key = SecurityHelper.getPublicKey(compID);
            CipherDMAP pubCipher = new CipherDMAP("RSA/ECB/PKCS1Padding", key);
            aesCipher = new CipherDMAP(16,256,"AES/CTR/NoPadding");
            String message = "ok " + numBase64 + " " +
                    SecurityHelper.enocdeToBase64(aesCipher.getKey().getEncoded()) + " " +
                    SecurityHelper.enocdeToBase64(aesCipher.getIv().getIV());
            byte[] cipherText = pubCipher.encrypt(message.getBytes());
            String cipherMsg = SecurityHelper.enocdeToBase64(cipherText);
            List<String> resp = getResponseOrThrowException(cipherMsg);
            if(resp.size() != 1){
                pubCipher.destroy();
                throw new DMAPException(MALFORMED_ANSWER);
            }

            byte[] answer = SecurityHelper.decodeBase64(resp.get(0));
            String answerSt = new String(aesCipher.decrypt(answer));
            if(answerSt.startsWith("ok")){
                String[] results = answerSt.split(" ", 2);
                byte[] challenge = SecurityHelper.decodeBase64(results[1]);
                if(results[1].equals(numBase64) && Arrays.equals(challenge, num)){
                    isEncrypted = true;
                }else{
                    aesCipher.destroy();
                    throw new DMAPException(WRONG_ANSWER);
                }
            }else{
                throw new DMAPException(MALFORMED_ANSWER);
            }

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
        aesCipher.destroy();
        executeOrThrowException(command, "ok bye");
    }

    private List<String> getResponseOrThrowException(String command) throws IOException, DMAPException {
        ArrayList<String> response = new ArrayList<>();
        String message = initWrite(command);
        if (message.startsWith("error ")) {
            throw new DMAPException(message.substring(6));
        }

        response.add(message);
        while (reader.ready() && (message = reader.readLine()) != null) {
            message = decrypt(message);
            response.add(message);
        }

        return response;
    }

    private void executeOrThrowException(
            String command,
            String expectedAnswer
    ) throws IOException, DMAPException {

        String message = initWrite(command);
        if (!message.equals(expectedAnswer)) {
            if (message.startsWith("error ")) {
                // Insert error message as content
                throw new DMAPException(message.substring(6));
            } else {
                throw new DMAPException(UNEXPECTED_ANSWER);
            }
        }
    }

    private String initWrite(String command) throws DMAPException, IOException{
        command = encrypt(command);
        writer.println(command);
        String message = reader.readLine();
        if (message == null) throw new DMAPException(NO_ANSWER);
        return decrypt(message);
    }

    private String encrypt(String command) throws DMAPException{
        if(isEncrypted){
            command = aesCipher.encryptString(command);
        }
        return command;
    }

    private String decrypt(String command) throws DMAPException{
        if(isEncrypted){
            command = aesCipher.decryptString(command);
        }
        return command;
    }


}
