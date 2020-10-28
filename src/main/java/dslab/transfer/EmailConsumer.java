package dslab.transfer;

import dslab.protocols.dmpt.client.DMTPClientHandler;
import dslab.protocols.dmpt.DMTPException;
import dslab.protocols.dmpt.Email;
import dslab.protocols.dmpt.client.IDMTPClientHandler;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class EmailConsumer extends Thread {

    private final BlockingQueue<Email> blockingQueue;
    private final Config domainsConfig;

    public EmailConsumer(Config domainsConfig, BlockingQueue<Email> blockingQueue) {
        this.domainsConfig = domainsConfig;
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void run() {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        IDMTPClientHandler dmtpClientHandler = null;

        while (!Thread.currentThread().isInterrupted()) {

            try {
                Email email = blockingQueue.take();

                // Stores a list of recipients to which it wasn't possible to send the email
                Set<String> failedRecipients = new HashSet<>();
                // Stores any unknown recipients of any domain
                Set<String> unknownRecipients = new HashSet<>();
                // Stores any DMTP error encountered while processing the email
                Set<String> encounteredProtocolErrors = new HashSet<>();

                List<String> domains = email.getRecipientsDomains();

                for (String domain : domains) {

                    // Try sending the email to all the recipients within a domain using one connection
                    try {

                        if (!isDomainKnown(domain)) {
                            System.out.println("Skipping unknown domain: " + domain);
                            List<String> domainFailedRecipients = email.recipients.stream()
                                    .filter(r -> domain.equals(Email.getDomain(r)))
                                    .collect(Collectors.toList());
                            failedRecipients.addAll(domainFailedRecipients);
                            continue;
                        }

                        socket = initSocketTo(domain);
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        writer = new PrintWriter(socket.getOutputStream(), true);

                        dmtpClientHandler = new DMTPClientHandler(socket, reader, writer);
                        dmtpClientHandler.init();
                        dmtpClientHandler.sendEmail(email, unknownRecipients::addAll);

                        try {
                            dmtpClientHandler.close();
                        } catch (IOException | DMTPException e) {
                            // An error here can be ignored: all the important work has already been done
                            System.out.println(e.getMessage());
                        }


                    } catch (IOException e) {
                        // In that case the error has been thrown during init() or sendEmail(), I assume that the email
                        // wasn't sent to any recipient so the sender can handle the issue.
                        List<String> failed = email.recipients.stream()
                                .filter(r -> domain.equals(Email.getDomain(r)))
                                .collect(Collectors.toList());
                        failedRecipients.addAll(failed);
                    } catch (DMTPException e) {
                        // Error from init() or sendEmail() methods, store the error and the list of recipients that
                        // didn't receive the email to create a report.
                        encounteredProtocolErrors.add(e.getMessage());
                        List<String> failed = email.recipients.stream()
                                .filter(r -> domain.equals(Email.getDomain(r)))
                                .collect(Collectors.toList());
                        failedRecipients.addAll(failed);
                    } finally {
                        closeResources(socket, reader, writer);
                    }

                }

                // todo check and add error if unknown domain - add that to report
                // (try) Send error email
                if (!unknownRecipients.isEmpty() || !encounteredProtocolErrors.isEmpty() || !failedRecipients.isEmpty()) {
                    Email errorEmail = getErrorEmail(email, unknownRecipients, encounteredProtocolErrors, failedRecipients);

                    try {
                        String domain = Email.getDomain(email.sender);

                        // ignore if sender unknown
                        if (isDomainKnown(domain)) {
                            System.out.println("Trying to send: " + errorEmail.toString());
                            socket = initSocketTo(domain);
                            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            writer = new PrintWriter(socket.getOutputStream(), true);

                            dmtpClientHandler = new DMTPClientHandler(socket, reader, writer);
                            System.out.println("init");
                            dmtpClientHandler.init();
                            System.out.println("sending");
                            dmtpClientHandler.sendEmail(errorEmail, recipients -> {});

                            try {
                                dmtpClientHandler.close();
                            } catch (IOException | DMTPException e) {
                                // An error here can be ignored: all the important work has already been done
                                System.out.println(e.getMessage());
                            }
                        } else {
                            System.out.println("Skipping error email: unknown domain");
                        }

                    } catch (IOException | DMTPException e ) {
                        // Ignore errors at this point
                        System.out.println("Error sending error email: " + e.getMessage());
                    } finally {
                        closeResources(socket, reader, writer);
                    }
                }

            } catch (InterruptedException e) {
                // In case of interruption, email will be lost.
                // A better approach could permanently store email data and remove that data only after an email has
                // been processed.
                Thread.currentThread().interrupt();
            }
        }
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

    private Socket initSocketTo(String domain) throws IOException {
        String addr = domainsConfig.getString(domain);
        String ip = addr.split(":")[0];
        int port = Integer.parseInt(addr.split(":")[1]);
        return new Socket(ip, port);
    }

    private static Email getErrorEmail(
            Email email,
            Set<String> unknownRecipients,
            Set<String> encounteredProtocolErrors,
            Set<String> failedRecipients
    ) {
        Email errorEmail = new Email();
        errorEmail.sender = "noreply@transfer.com";
        errorEmail.recipients = List.of(email.sender);
        errorEmail.subject = "Error sending email";

        if (!encounteredProtocolErrors.isEmpty() || !unknownRecipients.isEmpty()) {
            errorEmail.data += "Encountered errors: ";
            if (!unknownRecipients.isEmpty())
                errorEmail.data += "error unknown recipient " + String.join(",", unknownRecipients);
            for (String error : encounteredProtocolErrors)
                errorEmail.data += " - " + error + " - ";
        }

        if (!failedRecipients.isEmpty()) {
            errorEmail.data += " - We can't guarantee that your email was sent to the following addresses: ";
            for (String rec : failedRecipients)
                errorEmail.data += rec + " - ";
        }

        return errorEmail;
    }

    private boolean isDomainKnown(String domain) {
        return domainsConfig.containsKey(domain);
    }

}
