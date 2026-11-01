package com.zapiee.tableservice.controller;

import com.zapiee.tableservice.dto.response.TableSessionResponse;
import com.zapiee.tableservice.dto.ApiResponse;
import com.zapiee.tableservice.service.TableSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/table-sessions")
@RequiredArgsConstructor
public class TableSessionController {

    private final TableSessionService tableSessionService;

    @GetMapping("/{sessionToken}")
    public ResponseEntity<TableSessionResponse> getSession(@PathVariable String sessionToken) {
        TableSessionResponse response = tableSessionService.getSession(sessionToken);
        return ResponseEntity.ok(response);
    }

}