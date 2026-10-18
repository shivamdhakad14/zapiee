package com.zapiee.menu_service.dto.response;

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
