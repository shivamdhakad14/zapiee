package com.zapiee.menu_service.exception;

import com.zapiee.menu_service.enums.ImageSource;

public class TransientImageProviderException extends ImageProviderException {
    public TransientImageProviderException(ImageSource source, String message, Throwable cause) {
        super(source, message, cause);
    }
}
