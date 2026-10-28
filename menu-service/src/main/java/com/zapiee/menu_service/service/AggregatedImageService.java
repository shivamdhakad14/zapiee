package com.zapiee.menu_service.service;

import com.zapiee.menu_service.configuration.ImageProperties;
import com.zapiee.menu_service.dto.request.ImageResult;
import com.zapiee.menu_service.dto.response.ImageSearchResponse;
import com.zapiee.menu_service.enums.ImageSource;
import com.zapiee.menu_service.exception.ImageSearchUnavailableException;
import com.zapiee.menu_service.provider.ImageProvider;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class AggregatedImageService {

    private static final Logger log = LoggerFactory.getLogger(AggregatedImageService.class);

    private final List<ImageProvider> providers;
    private final Duration aggregateTimeout;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AggregatedImageService(List<ImageProvider> providers, ImageProperties props) {
        this.providers = List.copyOf(providers);
        this.aggregateTimeout = props.aggregateTimeout();
    }

    public ImageSearchResponse search(String rawQuery, int page, int perPage, boolean foodContext) {
        String query = normalize(rawQuery);
        if (query.isEmpty()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        // Menu items like "mango" or "cold coffee" are ambiguous; bias results towards food photos.
        String effectiveQuery = foodContext ? query + " food" : query;

        // Providers run in parallel; total latency = slowest provider, not the sum.
        List<CompletableFuture<Outcome>> futures = providers.stream()
                .map(p -> callProvider(p, effectiveQuery, page, perPage))
                .toList();
        List<Outcome> outcomes = futures.stream().map(CompletableFuture::join).toList(); // never throws

        List<ImageSource> failed = outcomes.stream().filter(Outcome::failed).map(Outcome::source).toList();
        if (failed.size() == outcomes.size()) {
            throw new ImageSearchUnavailableException(failed);
        }

        List<ImageResult> merged = interleave(outcomes.stream().map(Outcome::results).toList());
        return new ImageSearchResponse(query, page, merged, failed, !failed.isEmpty());
    }

    private CompletableFuture<Outcome> callProvider(ImageProvider provider, String query, int page, int perPage) {
        return CompletableFuture.supplyAsync(() -> provider.search(query, page, perPage), executor)
                .orTimeout(aggregateTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((results, ex) -> {
                    if (ex == null) {
                        return new Outcome(provider.source(), results == null ? List.of() : results, false);
                    }
                    Throwable cause = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
                    if (cause instanceof CallNotPermittedException) {
                        log.warn("{} circuit breaker is open, skipping", provider.source());
                    } else {
                        log.warn("{} search failed: {}", provider.source(), cause.toString());
                    }
                    return new Outcome(provider.source(), List.of(), true);
                });
    }

    /** Alternates providers (P1, U1, P2, U2...) and drops duplicate URLs, so the UI shows a mix. */
    private static List<ImageResult> interleave(List<List<ImageResult>> lists) {
        Set<String> seen = new HashSet<>();
        List<ImageResult> out = new ArrayList<>();
        int max = lists.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < max; i++) {
            for (List<ImageResult> list : lists) {
                if (i < list.size() && seen.add(list.get(i).url())) {
                    out.add(list.get(i));
                }
            }
        }
        return List.copyOf(out);
    }

    /** Also improves cache hit rate: "  Paneer   Tikka " and "paneer tikka" share one entry. */
    static String normalize(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }

    @PreDestroy
    void shutdown() {
        executor.close();
    }

    private record Outcome(ImageSource source, List<ImageResult> results, boolean failed) {}
}
