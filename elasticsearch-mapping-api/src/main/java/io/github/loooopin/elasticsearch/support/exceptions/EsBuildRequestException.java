package io.github.loooopin.elasticsearch.support.exceptions;

public class EsBuildRequestException extends RuntimeException {
    public EsBuildRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public EsBuildRequestException(String message) {
        super(message);
    }
}
