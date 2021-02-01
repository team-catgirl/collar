package team.catgirl.collar.client;

public abstract class CollarException extends RuntimeException {

    public CollarException(String message) {
        super(message);
    }

    public CollarException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class UnsupportedServerVersionException extends CollarException {
        public UnsupportedServerVersionException(String message) {
            super(message);
        }
    }

    public static class ConnectionException extends CollarException {
        public ConnectionException(String message) {
            super(message);
        }

        public ConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
