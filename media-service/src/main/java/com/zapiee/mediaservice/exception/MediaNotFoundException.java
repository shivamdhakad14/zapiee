package com.zapiee.mediaservice.exception;

public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(Long id) {
        super("Media asset not found: " + id);
    }
}
