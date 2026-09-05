package com.pratik.demourl.Controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.pratik.demourl.Model.LinkRecord;
import com.pratik.demourl.Service.ShortUrlService;

@RestController
public class ShortUrlController {
    private final ShortUrlService service;

    public ShortUrlController(ShortUrlService service) {
        this.service = service;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<?> create(@RequestBody CreateRequest request) {
        LinkRecord link = service.create(request.url(), request.alias(), request.expiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateResponse(link.getCode(), "/" + link.getCode(),
            link.getUrl(), link.getExpiresAt()));
    }

    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Referer", required = false) String referrer,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "X-Country", required = false) String country) {
        LinkRecord link = service.find(code);
        if (link == null) return ResponseEntity.notFound().build();
        if (!link.isActive()) throw new LinkUnavailableException(HttpStatus.FORBIDDEN, "This short link has been deactivated");
        if (link.expiredAt(Instant.now())) throw new LinkUnavailableException(HttpStatus.GONE, "This short link has expired");
        service.recordClick(code, firstIp(forwardedFor), userAgent, referrer, country);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, link.getUrl()).build();
    }

    @GetMapping("/api/urls/{code}/analytics")
    public ResponseEntity<?> analytics(@PathVariable String code) {
        if (service.find(code) == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(service.analytics(code));
    }

    @DeleteMapping("/api/urls/{code}")
    public ResponseEntity<?> deactivate(@PathVariable String code) {
        if (service.find(code) == null) return ResponseEntity.notFound().build();
        service.deactivate(code);
        return ResponseEntity.ok(Map.of("message", "Short link deactivated", "code", code));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(LinkUnavailableException.class)
    ResponseEntity<Map<String, String>> unavailable(LinkUnavailableException exception) {
        return ResponseEntity.status(exception.status).body(Map.of("error", exception.getMessage()));
    }

    private static String firstIp(String forwardedFor) {
        return forwardedFor == null ? "unknown" : forwardedFor.split(",")[0].trim();
    }

    public record CreateRequest(String url, String alias, Instant expiresAt) {}
    public record CreateResponse(String code, String shortPath, String url, Instant expiresAt) {}

    private static class LinkUnavailableException extends RuntimeException {
        private final HttpStatus status;

        LinkUnavailableException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }
}