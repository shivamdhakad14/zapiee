package com.zapiee.menu_service.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zapiee.menu_service.configuration.ImageProperties;
import com.zapiee.menu_service.dto.request.ImageResult;
import com.zapiee.menu_service.enums.ImageSource;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Component
public class UnsplashProvider extends AbstractImageProvider {

    private static final int MAX_PER_PAGE = 30;

    private final RestClient client;

    public UnsplashProvider(RestClient.Builder builder, ImageProperties props) {
        this.client = ImageHttpClients.create(builder, props, props.unsplash().baseUrl(), h -> {
            h.set(HttpHeaders.AUTHORIZATION, "Client-ID " + props.unsplash().apiKey());
            h.set("Accept-Version", "v1");
        });
    }

    @Override
    public ImageSource source() {
        return ImageSource.UNSPLASH;
    }

    @Override
    @Retry(name = "images")
    @CircuitBreaker(name = "unsplash")
    @Cacheable(cacheNames = "imageSearch", key = "'unsplash:' + #query + ':' + #page + ':' + #perPage")
    public List<ImageResult> search(String query, int page, int perPage) {
        UnsplashResponse body = execute(() -> client.get()
                .uri(u -> u.path("/search/photos")
                        .queryParam("query", "{q}")
                        .queryParam("page", page)
                        .queryParam("per_page", Math.min(perPage, MAX_PER_PAGE))
                        .queryParam("orientation", "landscape")
                        .queryParam("content_filter", "high")
                        .build(query))
                .retrieve()
                .body(UnsplashResponse.class));

        if (body == null || body.results() == null) {
            return List.of();
        }
        return body.results().stream()
                .filter(p -> p != null && p.urls() != null && hasText(p.urls().regular()))
                .map(p -> new ImageResult(
                        "unsplash-" + p.id(),
                        ImageSource.UNSPLASH,
                        p.urls().regular(),
                        hasText(p.urls().small()) ? p.urls().small() : p.urls().regular(),
                        hasText(p.altDescription()) ? p.altDescription() : p.description(),
                        p.user() != null ? p.user().name() : null,
                        p.user() != null && p.user().links() != null ? p.user().links().html() : null,
                        p.links() != null ? p.links().html() : null))
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UnsplashResponse(List<Photo> results) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Photo(String id, String description,
                     @JsonProperty("alt_description") String altDescription,
                     Urls urls, User user, Links links) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Urls(String regular, String small) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record User(String name, Links links) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Links(String html) {}
    }
}
