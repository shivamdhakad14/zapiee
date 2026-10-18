package com.zapiee.menu_service.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class GeminiGenerateResponse {
    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;

    @Getter @Setter
    public static class Candidate {
        private Content content;
        private String finishReason;
    }

    @Getter @Setter
    public static class Content {
        private List<Part> parts;
    }

    @Getter @Setter
    public static class Part {
        private String text;
    }

    @Getter @Setter
    public static class PromptFeedback {
        private String blockReason;
    }
}
