package com.zapiee.menu_service.exception;

import com.zapiee.menu_service.enums.ImageSource;

public class ImageProviderException extends RuntimeException {
    private final ImageSource source;

    public ImageProviderException(ImageSource source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public ImageSource getSource() {
        return source;
    }
}
