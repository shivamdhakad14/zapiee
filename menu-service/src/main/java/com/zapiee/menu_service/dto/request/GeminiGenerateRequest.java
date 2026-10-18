package com.zapiee.menu_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiGenerateRequest {
    private List<Content> contents;
    private GenerationConfig generationConfig;

    @Getter @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Getter @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Part {
        private String text;
        private InlineData inlineData;
    }

    @Getter @Builder
    public static class InlineData {
        private String mimeType;
        private String data;
    }

    @Getter @Builder
    public static class GenerationConfig {
        private String responseMimeType;
        private Object responseSchema;
        private Double temperature;
    }
}