package com.zapiee.menu_service.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zapiee.menu_service.configuration.ImageProperties;
import com.zapiee.menu_service.dto.request.ImageResult;
import com.zapiee.menu_service.enums.ImageSource;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Component
public class PexelsProvider extends AbstractImageProvider {

    private static final int MAX_PER_PAGE = 80;

    private final RestClient client;

    public PexelsProvider(RestClient.Builder builder, ImageProperties props) {
        this.client = ImageHttpClients.create(builder, props, props.pexels().baseUrl(),
                h -> h.set(HttpHeaders.AUTHORIZATION, props.pexels().apiKey()));
    }

    @Override
    public ImageSource source() {
        return ImageSource.PEXELS;
    }

    // Failures are not cached (exceptions skip the cache), successes are.
    @Override
    @Retry(name = "images")
    @CircuitBreaker(name = "pexels")
    @Cacheable(cacheNames = "imageSearch", key = "'pexels:' + #query + ':' + #page + ':' + #perPage")
    public List<ImageResult> search(String query, int page, int perPage) {
        PexelsResponse body = execute(() -> client.get()
                .uri(u -> u.path("/search")
                        .queryParam("query", "{q}") // template var => value is strictly URL-encoded
                        .queryParam("page", page)
                        .queryParam("per_page", Math.min(perPage, MAX_PER_PAGE))
                        .queryParam("orientation", "landscape")
                        .build(query))
                .retrieve()
                .body(PexelsResponse.class));

        if (body == null || body.photos() == null) {
            return List.of();
        }
        return body.photos().stream()
                .filter(p -> p != null && p.src() != null && hasText(p.src().large()))
                .map(p -> new ImageResult(
                        "pexels-" + p.id(),
                        ImageSource.PEXELS,
                        p.src().large(),
                        hasText(p.src().medium()) ? p.src().medium() : p.src().large(),
                        p.alt(),
                        p.photographer(),
                        p.photographerUrl(),
                        p.url()))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PexelsResponse(List<Photo> photos) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Photo(Long id, String url, String alt, String photographer,
                     @JsonProperty("photographer_url") String photographerUrl, Src src) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Src(String large, String medium) {}
    }
}
