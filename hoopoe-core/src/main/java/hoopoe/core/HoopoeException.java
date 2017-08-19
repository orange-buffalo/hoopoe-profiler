package hoopoe.core;

/**
 * Exception to be thrown by Hoopoe components.
 * We prefer runtime exceptions over checked ones.
 */
public class HoopoeException extends RuntimeException {

    public HoopoeException(String message) {
        super(message);
    }

    public HoopoeException(String message, Throwable cause) {
        super(message, cause);
    }

    public HoopoeException(Throwable cause) {
        super(cause);
    }

}
