package dslab.protocols.dmpt;


public class UnknownRecipientException extends Exception {

    private final String unknownRecipient;

    public UnknownRecipientException(String unknownRecipient) {
        this.unknownRecipient = unknownRecipient;
    }

    public String getUnknownRecipient() { return unknownRecipient; }
}
