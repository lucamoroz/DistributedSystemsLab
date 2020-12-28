package dslab.protocols.dmtp;

/**
 * Exception while verifying email hash.
 */
public class VerificationException extends Exception {
    public VerificationException(String message)
    {
        super(message);
    }
}
