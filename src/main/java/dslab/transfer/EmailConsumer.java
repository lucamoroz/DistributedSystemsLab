package dslab.transfer;

import dslab.protocols.dmpt.UnknownRecipientException;
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

    public EmailConsumer(BlockingQueue<Email> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void run() {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        DMTPClientHandler dmtpClientHandler = null;

        while (!Thread.currentThread().isInterrupted()) {

            try {
                Email email = blockingQueue.take();

                // Stores a list of recipients to which it wasn't possible to send the email
                Set<String> failedRecipients = new HashSet<>();
                // Stores any unknown recipients of any domain
                Set<String> unknownRecipients = new HashSet<>();

                // Stores any DMTP error encountered while processing the email
                Set<String> encounteredProtocolErrors = new HashSet<>();

                // Partition the email by recipients domain, domains will be processed sequentially
                List<Email> sameDomainEmails = email.getEmailPerDomain();

                for (Email e : sameDomainEmails) {

                    // Try sending the email to all the recipients within a domain using one connection
                    try {
                        String domain = getDomainFromEmail(e);
                        socket = initSocketTo(domain);
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        writer = new PrintWriter(socket.getOutputStream(), true);

                        dmtpClientHandler = new DMTPClientHandler(socket, reader, writer);

                        dmtpClientHandler.init();

                        // Try sending the email until no unknown recipients
                        List<String> currDomainUnknownRecipients = trySendUntilKnownRecipients(dmtpClientHandler, e);
                        if (!currDomainUnknownRecipients.isEmpty())
                            unknownRecipients.addAll(currDomainUnknownRecipients);

                        try {
                            dmtpClientHandler.close();
                        } catch (IOException | DMTPException exception) {
                            // An error here can be ignored: all the important work has already been done
                            System.out.println(exception.getMessage());
                        }


                    } catch (IOException exception) {
                        // In that case the error has been thrown during init() or sendEmail(), I assume that the email
                        // wasn't sent to any recipient so he can handle the issue.
                        failedRecipients.addAll(e.recipients);
                    } catch (DMTPException error) {
                        // Error from init() or sendEmail() methods, store the error and the list of recipients that
                        // didn't receive the email to create a report.
                        encounteredProtocolErrors.add(error.getMessage());
                        failedRecipients.addAll(e.recipients);
                    } finally {
                        closeResources(socket, reader, writer);
                    }

                    if (!failedRecipients.isEmpty() || !encounteredProtocolErrors.isEmpty()) {
                        // todo send email to sender, but ask what to do in case of unknown recipients
                    }

                }

            } catch (InterruptedException e) {
                // In case of interruption, email will be lost.
                // A better approach could permanently store email data and remove that data only after an email has
                // been processed.
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Terminated " + Thread.currentThread().getName());
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
        // todo - handle server down or not found
        Config config = new Config("domains");
        String addr = config.getString(domain);
        String ip = addr.split(":")[0];
        int port = Integer.parseInt(addr.split(":")[1]);
        return new Socket(ip, port);
    }

    private static String getDomainFromEmail(Email email) {
        return email.recipients.get(0).split("@")[1];
    }

    /**
     *
     * @param handler DMTP client protocol handler.
     * @param email Email to send. Any unknown recipient found will be removed from the recipients.
     * @return List of unknown recipients, empty if none.
     */
    private static List<String> trySendUntilKnownRecipients(IDMTPClientHandler handler, Email email)
            throws DMTPException, IOException {
        List<String> unknownRecipients = new LinkedList<>();
        boolean retry = true;
        while (retry) {

            try {
                handler.sendEmail(email);
                retry = false;
            } catch (UnknownRecipientException exception) {
                unknownRecipients.add(exception.getUnknownRecipient());
                email.recipients = email.recipients.stream()
                        .filter(r -> !r.equals(exception.getUnknownRecipient()))
                        .collect(Collectors.toList());
            }

            if (email.recipients.isEmpty())
                retry = false;
        }

        return unknownRecipients;
    }

}
