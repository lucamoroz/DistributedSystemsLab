package dslab.protocols.dmtp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Email {

    public Email() { }

    public Email(String sender, List<String> recipients, String subject, String data) {
        this.sender = sender;
        this.recipients = recipients;
        this.subject = subject;
        this.data = data;
    }

    public int id;
    public String sender;
    public List<String> recipients;
    public String subject;
    public String data;
    public String hash;

    public List<String> getRecipientsDomains() {
        return recipients.stream()
                .map(Email::getDomain)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Email{" +
                "sender='" + sender + '\'' +
                ", recipients=" + recipients.toString() +
                ", subject='" + subject + '\'' +
                ", data='" + data + '\'' +
                '}';
    }

    public String prettyPrint() {
        String prettyString = String.format("(%d) %s%n", id, subject);
        prettyString += String.format("\tfrom:\t%s%n", sender);
        prettyString += String.format("\tto:\t%s%n", String.join("; ", recipients));
        prettyString += String.format("\tdata:\t%s", data);

        return prettyString;
    }

    public String printToDmtpFormat() {
        return "from " + sender + "\n" +
                "to " + String.join(",", recipients) + "\n" +
                "subject " + subject + "\n" +
                "data " + data + "\n" +
                "hash " + (hash == null ? "" : hash);
    }

    public void setHash(SecretKeySpec secret) throws VerificationException {
        hash = getHash(secret);
    }

    public String getHash(SecretKeySpec secret) throws VerificationException {
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new VerificationException("SHA256 is not available as algorithm");
        }

        try {
            mac.init(secret);
        } catch (InvalidKeyException e) {
            throw new VerificationException("Invalid key: " + e.getMessage());
        }

        String msg = String.join("\n", sender, String.join(",", recipients), subject, data);
        byte[] hashBytes = mac.doFinal(msg.getBytes());

        return Base64.getEncoder().encodeToString(hashBytes);
    }

    public void verify(SecretKeySpec secret) throws VerificationException {
        if (hash == null || hash.isBlank()) {
            System.out.println("No hash available for message");
            return;
        }

        String calculatedHash = getHash(secret);

        if (!calculatedHash.equals(hash)) {
            throw new VerificationException("message hash does not match");
        }
    }

    /**
     * Get email's domain, e.g. given example@domain.com returns domain.com
     * @param address an email address
     * @return email's domain, null if it wasn't possible to get the domain (e.g. invalid address)
     */
    static public String getDomain(String address) {
        if (isValidAddress(address)) {
            return address.split("@")[1];
        } else {
            return null;
        }
    }

    static public String getUser(String address) {
        if (isValidAddress(address)) {
            return address.split("@")[0];
        } else {
            return null;
        }
    }



    public static boolean isValidAddress(String address) {
        // Check if recipient follows email pattern
        Pattern pattern = Pattern.compile("^.+@.+\\..+$");
        Matcher matcher = pattern.matcher(address);
        return matcher.matches();
    }

}
