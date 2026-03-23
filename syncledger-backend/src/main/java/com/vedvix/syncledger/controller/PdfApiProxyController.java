package com.vedvix.syncledger.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Reverse proxy that forwards /pdf-api/** requests to the PDF microservice.
 * In production the frontend (CloudFront) has no nginx layer, so the backend
 * proxies mapping-profile and field-info requests to pdf-service on the
 * Docker network.
 */
@Slf4j
@RestController
public class PdfApiProxyController {

    private final RestTemplate restTemplate;
    private final String pdfServiceUrl;

    public PdfApiProxyController(
            RestTemplate restTemplate,
            @Value("${pdf-service.url:http://localhost:8001}") String pdfServiceUrl) {
        this.restTemplate = restTemplate;
        this.pdfServiceUrl = pdfServiceUrl;
        log.info("PdfApiProxyController initialized with PDF service URL: {}", pdfServiceUrl);
    }

    @RequestMapping(value = "/pdf-api/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {

        // Build downstream URL: strip /pdf-api (or /api/pdf-api) prefix
        String requestUri = request.getRequestURI();
        // Handle both context-path=/api and context-path=/ deployments
        String downstream = requestUri.replaceFirst("^(/api)?/pdf-api", "");
        if (downstream.isEmpty()) {
            downstream = "/";
        }

        // Build target URL
        StringBuilder targetBuilder = new StringBuilder(pdfServiceUrl);
        targetBuilder.append(downstream);
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            targetBuilder.append('?').append(queryString);
        }
        String targetUrl = targetBuilder.toString();

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        log.debug("Proxying {} {} -> {}", method, requestUri, targetUrl);

        // Forward request headers
        HttpHeaders headers = new HttpHeaders();
        String contentType = request.getContentType();
        if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }

        try {
            // Read body for non-GET requests
            byte[] body = null;
            if (method != HttpMethod.GET && method != HttpMethod.DELETE) {
                body = request.getInputStream().readAllBytes();
                if (body.length == 0) body = null;
            }

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    targetUrl, method, new HttpEntity<>(body, headers), byte[].class);

            HttpHeaders responseHeaders = new HttpHeaders();
            if (response.getHeaders().getContentType() != null) {
                responseHeaders.setContentType(response.getHeaders().getContentType());
            }
            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());

        } catch (HttpStatusCodeException ex) {
            log.warn("PDF service returned {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            HttpHeaders responseHeaders = new HttpHeaders();
            if (ex.getResponseHeaders() != null && ex.getResponseHeaders().getContentType() != null) {
                responseHeaders.setContentType(ex.getResponseHeaders().getContentType());
            }
            return new ResponseEntity<>(ex.getResponseBodyAsByteArray(), responseHeaders, ex.getStatusCode());

        } catch (RestClientException ex) {
            log.error("Failed to reach PDF service at {}: {}", targetUrl, ex.getMessage());
            String errorJson = "{\"success\":false,\"message\":\"PDF microservice is unreachable\"}";
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(errorJson.getBytes(), responseHeaders, HttpStatus.BAD_GATEWAY);

        } catch (Exception ex) {
            log.error("Proxy error for {}: ", targetUrl, ex);
            String msg = ex.getMessage() != null ? ex.getMessage().replace("\"", "'") : "Unknown proxy error";
            String errorJson = "{\"success\":false,\"message\":\"Proxy error: " + msg + "\"}";
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(errorJson.getBytes(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
