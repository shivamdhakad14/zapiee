package com.zapiee.mediaservice.dto;

import lombok.*;
import org.springframework.stereotype.Service;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignedUploadResponse{
        private String cloudName;
        private String apiKey;
        private String signature;
        private long timestamp;
        private String folder;
        private String publicId;
}
