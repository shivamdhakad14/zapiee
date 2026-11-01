package com.zapiee.menu_service.service;

import com.zapiee.menu_service.client.FileUploadClient;
import com.zapiee.menu_service.dto.request.GeminiGenerateRequest;
import com.zapiee.menu_service.dto.request.GeminiProperties;
import com.zapiee.menu_service.dto.response.ExtractedMenuItem;
import com.zapiee.menu_service.dto.response.GeminiGenerateResponse;
import com.zapiee.menu_service.dto.response.MenuExtractionSchema;
import com.zapiee.menu_service.exception.MenuExtractionException;
import com.zapiee.menu_service.exception.MenuExtractionSafetyBlockedException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuImageExtractionService {

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final FileUploadClient fileStorageClient;

    private static final String EXTRACTION_PROMPT = """
        You are analyzing a photograph of a restaurant menu. Extract every menu
        item visible. For each item return: category (the section header it
        appears under, or "Uncategorized" if none is visible), name (exactly as
        written), description (or null if none), price (numeric only, no
        currency symbol — if a range like "150/250" is shown, use the higher
        value and mention the range in description), isVeg (true/false/null if
        not indicated by a veg/non-veg marker), and confidence (0.0 to 1.0 —
        use a LOWER value for blurry text, ambiguous prices, or handwriting).
        If the image is not a menu at all, return an empty array.
        """;

    @Bulkhead(
            name = "geminiExtraction",
            type = Bulkhead.Type.SEMAPHORE
    )
    @RateLimiter(name = "geminiExtraction")
    @Retry(name = "geminiExtraction")
    public List<ExtractedMenuItem> extractFromImage(String imageUrl) {
        log.info("menu extraction start");
        byte[] imageBytes = fileStorageClient.download(imageUrl);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        GeminiGenerateRequest request = GeminiGenerateRequest.builder()
                .contents(List.of(GeminiGenerateRequest.Content.builder()
                        .parts(List.of(
                                GeminiGenerateRequest.Part.builder().text(EXTRACTION_PROMPT).build(),
                                GeminiGenerateRequest.Part.builder()
                                        .inlineData(GeminiGenerateRequest.InlineData.builder()
                                                .mimeType("image/jpeg")
                                                .data(base64Image)
                                                .build())
                                        .build()
                        ))
                        .build()))
                .generationConfig(GeminiGenerateRequest.GenerationConfig.builder()
                        .responseMimeType("application/json")
                        .responseSchema(MenuExtractionSchema.build())
                        .temperature(0.1)
                        .build())
                .build();

        log.info(
                "Gemini request: model={}, imageBytes={}, base64Length={}",
                geminiProperties.getModel(),
                imageBytes.length,
                base64Image.length()
        );

        GeminiGenerateResponse response = geminiRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", geminiProperties.getApiKey())
                        .build(geminiProperties.getModel()))
                .body(request)
                .retrieve()
                .body(GeminiGenerateResponse.class);

        return parseResponse(response);
    }

//    public List<ExtractedMenuItem> extractFromImage(String imageUrl) {
//
//        int attempt = 0;
//        long waitMs = 5000;
//
//        while (true) {
//            attempt++;
//
//            try {
//                log.info("Gemini attempt {} for image {}", attempt, imageUrl);
//
//                byte[] imageBytes = fileStorageClient.download(imageUrl);
//                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
//
//                GeminiGenerateRequest request = GeminiGenerateRequest.builder()
//                        .contents(List.of(
//                                GeminiGenerateRequest.Content.builder()
//                                        .parts(List.of(
//                                                GeminiGenerateRequest.Part.builder()
//                                                        .text(EXTRACTION_PROMPT)
//                                                        .build(),
//                                                GeminiGenerateRequest.Part.builder()
//                                                        .inlineData(
//                                                                GeminiGenerateRequest.InlineData.builder()
//                                                                        .mimeType("image/jpeg")
//                                                                        .data(base64Image)
//                                                                        .build()
//                                                        )
//                                                        .build()
//                                        ))
//                                        .build()
//                        ))
//                        .generationConfig(
//                                GeminiGenerateRequest.GenerationConfig.builder()
//                                        .responseMimeType("application/json")
//                                        .responseSchema(MenuExtractionSchema.build())
//                                        .build()
//                        )
//                        .build();
//
//                GeminiGenerateResponse response = geminiRestClient.post()
//                        .uri(uriBuilder -> uriBuilder
//                                .path("/models/{model}:generateContent")
//                                .queryParam("key", geminiProperties.getApiKey())
//                                .build(geminiProperties.getModel()))
//                        .body(request)
//                        .retrieve()
//                        .body(GeminiGenerateResponse.class);
//
//                log.info(
//                        "Gemini success on attempt {} for image {}",
//                        attempt,
//                        imageUrl
//                );
//
//                return parseResponse(response);
//
//            } catch (HttpServerErrorException.ServiceUnavailable ex) {
//
//                log.warn(
//                        "Gemini 503 on attempt {}. Waiting {} ms before retry",
//                        attempt,
//                        waitMs
//                );
//
//                try {
//                    Thread.sleep(waitMs);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    throw new MenuExtractionException(
//                            "Gemini retry interrupted"
//                    );
//                }
//
//                // 5s → 10s → 20s → 40s → 60s → 60s...
//                waitMs = Math.min(waitMs * 2, 60_000L);
//            }
//        }
//    }

    private List<ExtractedMenuItem> parseResponse(GeminiGenerateResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            throw new MenuExtractionException("Gemini returned no candidates");
        }

        if (response.getPromptFeedback() != null && response.getPromptFeedback().getBlockReason() != null) {
            throw new MenuExtractionSafetyBlockedException(
                    "Image was blocked by content safety filters: " + response.getPromptFeedback().getBlockReason());
        }

        var candidate = response.getCandidates().get(0);
        if (!"STOP".equals(candidate.getFinishReason())) {
            log.warn("Gemini extraction finished abnormally: {}", candidate.getFinishReason());
        }

        String jsonText = candidate.getContent().getParts().get(0).getText();

        try {
            ExtractedMenuItem[] items = objectMapper.readValue(jsonText, ExtractedMenuItem[].class);
            return List.of(items);
        } catch (Exception e) {
            throw new MenuExtractionException("Failed to parse Gemini's structured output: " + e.getMessage());
        }
    }
}