package dslab.protocols.dmtp;

import java.util.*;
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

    public String sender;
    public List<String> recipients;
    public String subject;
    public String data;

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

    public String printToDmtpFormat() {
        return "from " + sender + "\n" +
                "to " + String.join(",", recipients) + "\n" +
                "subject " + subject + "\n" +
                "data " + data;
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
