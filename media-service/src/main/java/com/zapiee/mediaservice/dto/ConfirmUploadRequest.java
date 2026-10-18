package com.zapiee.mediaservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ConfirmUploadRequest {

    @NotNull
    private Long restaurantId;

    @NotBlank
    private String publicId;

    @NotBlank
    private String url;

    @NotBlank
    private String secureUrl;

    private String format;

    private Long bytes;
    private Integer width;
    private Integer height;
}
