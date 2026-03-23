package com.vedvix.syncledger.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Reverse proxy that forwards /pdf-api/** requests to the PDF microservice.
 * In production the frontend (CloudFront) has no nginx layer, so the backend
 * proxies mapping-profile and field-info requests to pdf-service on the
 * Docker network.
 */
@Slf4j
@RestController
@RequestMapping("/pdf-api")
public class PdfApiProxyController {

    private final RestTemplate restTemplate;
    private final String pdfServiceUrl;

    public PdfApiProxyController(
            RestTemplate restTemplate,
            @Value("${pdf-service.url:http://localhost:8001}") String pdfServiceUrl) {
        this.restTemplate = restTemplate;
        this.pdfServiceUrl = pdfServiceUrl;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<byte[]> proxy(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        // Strip the /pdf-api prefix (and optional context-path prefix)
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());          // e.g. /pdf-api/api/v1/mapping/profiles
        String downstream = path.replaceFirst("^/pdf-api", "");           // e.g. /api/v1/mapping/profiles

        String targetUrl = UriComponentsBuilder
                .fromHttpUrl(pdfServiceUrl + downstream)
                .query(request.getQueryString())
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        String contentType = request.getContentType();
        if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    targetUrl, method, new HttpEntity<>(body, headers), byte[].class);

            HttpHeaders responseHeaders = new HttpHeaders();
            if (response.getHeaders().getContentType() != null) {
                responseHeaders.setContentType(response.getHeaders().getContentType());
            }
            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsByteArray(),
                    ex.getResponseHeaders(), ex.getStatusCode());
        }
    }
}
