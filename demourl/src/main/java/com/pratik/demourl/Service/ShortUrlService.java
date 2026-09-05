package com.pratik.demourl.Service;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pratik.demourl.Model.ClickEvent;
import com.pratik.demourl.Model.LinkRecord;
import com.pratik.demourl.Repository.ClickEventRepository;
import com.pratik.demourl.Repository.LinkRecordRepository;

@Service
public class ShortUrlService {
    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final AtomicLong sequence = new AtomicLong(1000);
    private final Map<String, LinkRecord> cache = new ConcurrentHashMap<>();
    private final LinkRecordRepository linkRepository;
    private final ClickEventRepository clickRepository;

    public ShortUrlService(LinkRecordRepository linkRepository, ClickEventRepository clickRepository) {
        this.linkRepository = linkRepository;
        this.clickRepository = clickRepository;
    }

    @Transactional
    public LinkRecord create(String url, String alias, Instant expiresAt) {
        validateUrl(url);
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
        String code = alias == null || alias.isBlank() ? nextGeneratedCode() : alias.trim();
        if (!code.matches("[A-Za-z0-9_-]{3,32}")) {
            throw new IllegalArgumentException("alias must be 3-32 letters, numbers, '_' or '-'");
        }
        LinkRecord link = new LinkRecord(code, url, Instant.now(), expiresAt, true);
        if (linkRepository.existsById(code)) {
            throw new IllegalArgumentException("short code is already in use");
        }
        linkRepository.save(link);
        cache.put(code, link);
        return link;
    }

    public LinkRecord find(String code) {
        LinkRecord link = cache.get(code);
        if (link == null) {
            link = linkRepository.findById(code).orElse(null);
            if (link != null) cache.put(code, link);
        }
        return link;
    }

    @Transactional
    public void deactivate(String code) {
        linkRepository.findById(code).ifPresent(link -> {
            link.deactivate();
            linkRepository.save(link);
            cache.put(code, link);
        });
    }

    @Async
    @Transactional
    public void recordClick(String code, String ip, String userAgent, String referrer, String country) {
        ClickEvent event = new ClickEvent(code, Instant.now(), country == null || country.isBlank() ? "Unknown" : country,
                browser(userAgent), device(userAgent), referrer == null || referrer.isBlank() ? "Direct" : referrer,
                ip == null ? "unknown" : ip);
        clickRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Analytics analytics(String code) {
        List<ClickEvent> clicks = clickRepository.findByLinkCode(code);
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        Map<String, Long> countries = top(clicks, ClickEvent::getCountry);
        Map<String, Long> referrers = top(clicks, ClickEvent::getReferrer);
        Map<String, Long> byDay = clickRepository.findByLinkCodeAndTimestampAfter(code, since).stream()
                .collect(Collectors.groupingBy(click -> click.getTimestamp().toString().substring(0, 10), Collectors.counting()));
        return new Analytics(clicks.size(), clicks.stream().map(ClickEvent::getVisitorKey).distinct().count(), countries,
                referrers, byDay);
    }

    private static Map<String, Long> top(List<ClickEvent> clicks, java.util.function.Function<ClickEvent, String> field) {
        return clicks.stream().collect(Collectors.groupingBy(field, Collectors.counting())).entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, java.util.LinkedHashMap::new));
    }

    private static String encode(long value) {
        StringBuilder result = new StringBuilder();
        do { result.append(BASE62[(int) (value % 62)]); value /= 62; } while (value > 0);
        return result.reverse().toString();
    }

    private String nextGeneratedCode() {
        String code;
        do {
            code = encode(sequence.getAndIncrement());
        } while (linkRepository.existsById(code));
        return code;
    }

    private static void validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) throw new Exception();
        } catch (Exception exception) { throw new IllegalArgumentException("url must be a valid http or https URL"); }
    }

    private static String browser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        return "Other";
    }

    private static String device(String ua) {
        if (ua == null) return "Unknown";
        return ua.matches(".*(Mobile|Android|iPhone|iPad).*") ? "Mobile" : "Desktop";
    }

    public record Analytics(long totalClicks, long uniqueVisitors, Map<String, Long> topCountries,
                            Map<String, Long> topReferrers, Map<String, Long> clicksOverTime) {}
}