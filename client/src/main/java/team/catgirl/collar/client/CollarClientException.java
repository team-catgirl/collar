package team.catgirl.collar.client;

public abstract class CollarClientException extends RuntimeException {

    public CollarClientException(String message) {
        super(message);
    }

    public CollarClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class UnsupportedVersionException extends CollarClientException {
        public UnsupportedVersionException(String message) {
            super(message);
        }
    }

    public static class ConnectionException extends CollarClientException {
        public ConnectionException(String message) {
            super(message);
        }

        public ConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CollarStateException extends CollarClientException {
        public CollarStateException(String message) {
            super(message);
        }

        public CollarStateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
