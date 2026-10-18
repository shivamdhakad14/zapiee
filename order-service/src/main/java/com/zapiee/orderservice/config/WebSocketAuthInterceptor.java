package com.zapiee.orderservice.config;

import com.zapiee.orderservice.client.TableSessionClient;
import com.zapiee.orderservice.dto.response.TableSessionResponse;
import com.zapiee.orderservice.enums.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    public static final String ATTRIBUTE_ROLE = "role";
    public static final String ATTRIBUTE_SESSION = "sessionToken";
    public static final String ATTRIBUTE_RESTAURANT_ID = "restaurantId";

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_OWNER = "OWNER";

    private static final String HEADER_ROLE = "role";
    private static final String HEADER_RESTAURANT_ID = "restaurantId";

    private static final String PARAM_SESSION_TOKEN = "sessionToken";

    private final TableSessionClient tableSessionClient;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket handshake rejected: unsupported request type.");
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        String sessionToken = trimToNull(httpRequest.getParameter(PARAM_SESSION_TOKEN));
        HttpHeaders headers = request.getHeaders();

        String restaurantIdHeader = trimToNull(headers.getFirst(HEADER_RESTAURANT_ID));
        String roleHeader = trimToNull(headers.getFirst(HEADER_ROLE));

        if (sessionToken != null) {
            if (restaurantIdHeader != null || roleHeader != null) {
                log.warn("WebSocket handshake rejected: mixed customer and staff/owner authentication.");
                return false;
            }

            TableSessionResponse tableSession;

            try {
                tableSession = tableSessionClient.getSession(sessionToken);
            } catch (Exception ex) {
                log.warn("Customer WebSocket handshake rejected: unable to validate table session.");
                return false;
            }

            if (!isValidActiveSession(tableSession, sessionToken)) {
                log.warn("Customer WebSocket handshake rejected: invalid or inactive table session.");
                return false;
            }

            attributes.put(ATTRIBUTE_SESSION, tableSession);
            attributes.put(ATTRIBUTE_ROLE, ROLE_CUSTOMER);
            attributes.put(ATTRIBUTE_RESTAURANT_ID, tableSession.getRestaurantId());

            attributes.put("sessionToken", tableSession.getSessionToken());
            attributes.put("tableId", tableSession.getTableId());
            attributes.put("tableNumber", tableSession.getTableNumber());
            attributes.put("sessionId", tableSession.getSessionId());

            log.info("Customer WebSocket handshake accepted | restaurantId={} | tableId={} | sessionId={}",
                    tableSession.getRestaurantId(), tableSession.getTableId(), tableSession.getSessionId());

            return true;
        }


        if (restaurantIdHeader != null || roleHeader != null) {
            if (restaurantIdHeader == null || roleHeader == null) {
                log.warn("Staff/Owner WebSocket handshake rejected: restaurantId and role headers are required.");
                return false;
            }

            Long restaurantId = parsePositiveLong(restaurantIdHeader);

            if (restaurantId == null) {
                log.warn("Staff/Owner WebSocket handshake rejected: invalid restaurantId header.");
                return false;
            }

            String normalizedRole = roleHeader.trim().toUpperCase();

            if (!isStaffOrOwner(normalizedRole)) {
                log.warn("Staff/Owner WebSocket handshake rejected: unsupported role={}", normalizedRole);
                return false;
            }

            attributes.put(ATTRIBUTE_RESTAURANT_ID, restaurantId);
            attributes.put(ATTRIBUTE_ROLE, normalizedRole);

            log.info("Staff/Owner WebSocket handshake accepted | restaurantId={} | role={}",
                    restaurantId,normalizedRole );

            return true;
        }

        log.warn("WebSocket handshake rejected: missing sessionToken or Staff/Owner headers.");

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {

        if (exception != null) {
            log.warn("WebSocket handshake completed with exception: {}",
                    exception.getMessage());
            return;
        }

        log.debug("WebSocket handshake completed successfully.");
    }

    private boolean isValidActiveSession(TableSessionResponse session, String requestedToken) {

        if (session == null) {
            return false;
        }

        if (session.getSessionId() == null || session.getSessionId() <= 0) {
            return false;
        }

        if (session.getRestaurantId() == null || session.getRestaurantId() <= 0) {
            return false;
        }

        if (session.getTableId() == null || session.getTableId() <= 0) {
            return false;
        }

        if (session.getTableNumber() == null || session.getTableNumber() <= 0) {
            return false;
        }

        if (!StringUtils.hasText(session.getSessionToken())) {
            return false;
        }

        if (!requestedToken.equals(session.getSessionToken().trim())) {
            return false;
        }

        if (session.getSessionStatus() != SessionStatus.ACTIVE) {
            return false;
        }

        return true;
    }

    private boolean isStaffOrOwner(String role) {
        return ROLE_STAFF.equals(role) ||
                ROLE_OWNER.equals(role);
    }


    private Long parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;

        } catch (NumberFormatException ex) {
            return null;
        }
    }


    private String trimToNull(String value) {

        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}