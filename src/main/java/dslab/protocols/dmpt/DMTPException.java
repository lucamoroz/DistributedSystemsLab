package dslab.protocols.dmpt;

/**
 * Fatal protocol error.
 */
public class DMTPException extends Exception {
    public DMTPException(String message)
    {
        super(message);
    }
}
