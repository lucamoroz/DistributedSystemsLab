package dslab.protocols.dmpt;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Returns a list of emails. The domain within a single email will be the same for all the recipients.
     * @return List of Email, each Email will have one or more recipients that belong to the same domain.
     */
    public List<Email> getEmailPerDomain() {
        List<Email> emails = new ArrayList<>();
        HashMap<String, List<String>> domainRecipients = new HashMap<>();

        for (String recipient : recipients) {
            String domain = getDomain(recipient);
            if (domain != null) {
                List<String> prev = domainRecipients.getOrDefault(domain, new ArrayList<String>());
                prev.add(recipient);
                domainRecipients.put(domain, prev);
            }
        }

        for (List<String> sameDomainRecipients : domainRecipients.values()) {
            Email email = new Email(sender, sameDomainRecipients, subject, data);
            emails.add(email);
        }

        return emails;
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

    public static boolean isValidAddress(String address) {
        // Check if recipient follows email pattern
        Pattern pattern = Pattern.compile("^.+@.+\\..+$");
        Matcher matcher = pattern.matcher(address);
        return matcher.matches();
    }

    public static boolean isEmailValid(Email email) {
        // todo check fields and recipients
        return true;
    }

}
