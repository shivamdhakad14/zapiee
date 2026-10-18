package com.zapiee.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        log.info(
                "Incoming Request: Method={}, URI={}, RemoteAddress={}",
                request.getMethod(),
                request.getURI(),
                request.getRemoteAddress()
        );

        exchange.getResponse().beforeCommit(() -> {
            long duration = System.currentTimeMillis() - startTime;
            exchange.getResponse()
                    .getHeaders()
                    .add("X-Response-Time", String.valueOf(duration));
            return Mono.empty();
        });

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info(
                            "Outgoing Response: StatusCode={}, Duration={}ms, URI={}",
                            exchange.getResponse().getStatusCode(),
                            duration,
                            request.getURI()
                    );
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
