package com.zapiee.restaurantservice.dto.Response;

import com.zapiee.restaurantservice.dto.Request.MediaFileData;
import lombok.Data;


@Data
public class MediaUploadResponse {

    private boolean success;
    private String message;
    private MediaFileData data;

}