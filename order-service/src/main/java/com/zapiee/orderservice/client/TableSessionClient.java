package com.zapiee.orderservice.client;

import com.zapiee.orderservice.dto.response.TableSessionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "table-service")
public interface TableSessionClient {

    @GetMapping("/api/v1/table-sessions/{sessionToken}")
    TableSessionResponse getSession(@PathVariable("sessionToken") String sessionToken);

}
