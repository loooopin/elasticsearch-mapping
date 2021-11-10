package io.github.loooopin.elasticsearch.support.exceptions;

public class EsResolveResponseException extends RuntimeException {
    public EsResolveResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public EsResolveResponseException(String message) {
        super(message);
    }
}
