package dslab.transfer;

public class DomainNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public DomainNotFoundException(String message) {
        super(message);
    }

    public DomainNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
