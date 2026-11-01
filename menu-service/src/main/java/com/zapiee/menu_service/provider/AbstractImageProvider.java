package com.zapiee.menu_service.provider;

import com.zapiee.menu_service.exception.ImageProviderException;
import com.zapiee.menu_service.exception.TransientImageProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

abstract class AbstractImageProvider implements ImageProvider {

    private static final Logger log = LoggerFactory.getLogger(AbstractImageProvider.class);

    /** Translates every HTTP-client failure into our own exception types (transient vs. permanent). */
    protected <T> T execute(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String msg = source() + " responded with HTTP " + status;
            if (status == 401 || status == 403) {
                log.error("{} rejected our credentials (HTTP {}). Check the API key.", source(), status);
            }
            if (status == 429) {
                log.warn("{} rate limit reached", source());
            }
            if (status == 408 || e.getStatusCode().is5xxServerError()) {
                throw new TransientImageProviderException(source(), msg, e);
            }
            throw new ImageProviderException(source(), msg, e); // 4xx incl. 429: retrying won't help
        } catch (ResourceAccessException e) { // connect/read timeout, DNS, connection reset
            throw new TransientImageProviderException(source(), source() + " unreachable or timed out", e);
        } catch (RestClientException e) { // e.g. malformed JSON
            throw new ImageProviderException(source(), source() + " returned an unreadable response", e);
        }
    }
}
