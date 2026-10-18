package com.zapiee.api_gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class RoleBasedAccessGatewayFilterFactory extends AbstractGatewayFilterFactory<RoleBasedAccessGatewayFilterFactory.Config> {

    public RoleBasedAccessGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String rolesHeader = exchange.getRequest().getHeaders().getFirst("X-User-Roles");

            if (rolesHeader == null || rolesHeader.isBlank()) {
                log.warn("Access denied: no roles found in request headers for path={}",
                        exchange.getRequest().getPath());
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            List<String> userRoles = Arrays.asList(rolesHeader.split(","));
            List<String> requiredRoles = config.getRequiredRoles();

            boolean hasAccess = checkRoleAccess(userRoles, requiredRoles, config.isAnyRole());

            if (!hasAccess) {
                log.warn("Access denied: roles={} attempted path={} requiring={}",
                        userRoles, exchange.getRequest().getPath(), requiredRoles);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    private boolean checkRoleAccess(List<String> userRoles,
                                    List<String> requiredRoles,
                                    boolean anyRole) {
        if (anyRole) {
            return userRoles.stream().anyMatch(requiredRoles::contains);
        }
        return userRoles.containsAll(requiredRoles);
    }

    @Data
    public static class Config {
        private List<String> requiredRoles;
        private boolean anyRole = true;
    }
}