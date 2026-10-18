package com.zapiee.menu_service.configuration;

import com.zapiee.menu_service.dto.request.GeminiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiClientConfig {

    @Bean
    public RestClient geminiRestClient(GeminiProperties props) {
        return RestClient.builder().baseUrl(props.getBaseUrl()).build();
    }
}
