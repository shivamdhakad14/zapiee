package com.zapiee.mediaservice.controller;

import com.zapiee.mediaservice.exception.InvalidFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class InternalFileController {

    private static final String ALLOWED_HOST_SUFFIX = "cloudinary.com";

    private final RestTemplate restTemplate;

    @GetMapping("/internal/v1/files/download")
    public ResponseEntity<byte[]> download(@RequestParam("url") String fileUrl) {
        URI uri = validate(fileUrl);

        ResponseEntity<byte[]> upstream;
        try {
            upstream = restTemplate.getForEntity(uri, byte[].class);
        } catch (RuntimeException e) {
            log.error("Failed to download file from {}", uri, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = upstream.getHeaders().getContentType();
        headers.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.inline().filename(fileNameFrom(uri)).build());

        return new ResponseEntity<>(upstream.getBody(), headers, upstream.getStatusCode());
    }

    private URI validate(String fileUrl) {
        URI uri;
        try {
            uri = new URI(fileUrl);
        } catch (URISyntaxException e) {
            throw new InvalidFileException("Malformed file URL: " + fileUrl);
        }
        String host = uri.getHost();
        if (host == null || !host.endsWith(ALLOWED_HOST_SUFFIX)) {
            throw new InvalidFileException("Refusing to download from untrusted host: " + host);
        }
        return uri;
    }

    private String fileNameFrom(URI uri) {
        String path = uri.getPath();
        int idx = path.lastIndexOf('/');
        return idx >= 0 && idx < path.length() - 1 ? path.substring(idx + 1) : "file";
    }
}
