package com.zapiee.menu_service.client;

import com.zapiee.menu_service.dto.request.TableResolveRequest;
import com.zapiee.menu_service.dto.response.TableResolveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "table-service")
public interface TableClient {

    @PostMapping("/api/v1/tables/resolve")
    TableResolveResponse resolveTable(@RequestBody TableResolveRequest request);
}
