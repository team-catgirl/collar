package team.catgirl.collar.security.cipher;

public abstract class CipherException extends Exception {
    public CipherException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when there was an unrecoverable problem with the cipher state
     */
    public static class UnknownCipherException extends CipherException {
        public UnknownCipherException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when there was a recoverable problem with the cipher state.
     * Usually exchanging {@link team.catgirl.collar.security.signal.PreKeys} will fix the problem when this occurs
     * on the client
     */
    public static class InvalidCipherSessionException extends CipherException {
        public InvalidCipherSessionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
