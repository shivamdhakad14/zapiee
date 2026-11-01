package com.zapiee.mediaservice.dto;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MenuImagesUploadResponse {
    private String url;
    private long bytes;
}
