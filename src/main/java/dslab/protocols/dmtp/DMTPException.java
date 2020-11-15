package dslab.protocols.dmtp;

/**
 * Fatal protocol error.
 */
public class DMTPException extends Exception {
    public DMTPException(String message)
    {
        super(message);
    }
}
