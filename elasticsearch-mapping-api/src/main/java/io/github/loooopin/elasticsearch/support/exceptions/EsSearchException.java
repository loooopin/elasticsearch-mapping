package io.github.loooopin.elasticsearch.support.exceptions;

public class EsSearchException extends RuntimeException {
    public EsSearchException(String message, Throwable cause) {
        super(message, cause);
    }

    public EsSearchException(String message) {
        super(message);
    }
}
