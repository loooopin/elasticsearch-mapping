package io.github.loooopin.elasticsearch.support.exceptions;

public class EsContextInitException extends RuntimeException {
    public EsContextInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public EsContextInitException(String message) {
        super(message);
    }
}
