package team.catgirl.collar.client;

public abstract class CollarClientException extends RuntimeException {

    public CollarClientException(String message) {
        super(message);
    }

    public CollarClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class CollarConnectionException extends CollarClientException {
        public CollarConnectionException(String message) {
            super(message);
        }

        public CollarConnectionException(String message, Throwable cause) {
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
