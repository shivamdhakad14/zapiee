package com.zapiee.api_gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestTimeGatewayFilterFactory extends
        AbstractGatewayFilterFactory<RequestTimeGatewayFilterFactory.Config> {

    private static final String REQUEST_TIME_BEGIN = "requestTimeBegin";
    private static final String REQUEST_TIME_ELAPSED = "X-Request-Time";

    public RequestTimeGatewayFilterFactory(){
        super(Config.class);
    }

    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            exchange.getAttributes().put(REQUEST_TIME_BEGIN, System.currentTimeMillis());

            return chain.filter(exchange).then(
                    Mono.fromRunnable(() -> {
                        Long startTime = exchange.getAttribute(REQUEST_TIME_BEGIN);
                        if (startTime != null) {
                            long elapsed = System.currentTimeMillis() - startTime;

                            if (config.isEnabled()) {
                                exchange.getResponse().getHeaders()
                                        .add(REQUEST_TIME_ELAPSED, elapsed + "ms");
                            }

                            log.info("Request processed: URI={}, Time={}ms",
                                    exchange.getRequest().getURI(),
                                    elapsed);

                            if (elapsed > config.getWarningThreshold()) {
                                log.warn("Slow request detected: URI={}, Time={}ms",
                                        exchange.getRequest().getURI(),
                                        elapsed);
                            }
                        }
                    })
            );
        };
    }

    @Data
    public static class Config {
        private boolean enabled = true;
        private long warningThreshold = 1000;
    }

}
