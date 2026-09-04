package com.pratik.demourl;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "click_events", indexes = @Index(name = "idx_click_link_time", columnList = "linkCode,timestamp"))
public class ClickEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32)
    private String linkCode;
    @Column(nullable = false)
    private Instant timestamp;
    @Column(nullable = false, length = 100)
    private String country;
    @Column(nullable = false, length = 50)
    private String browser;
    @Column(nullable = false, length = 30)
    private String device;
    @Column(nullable = false, length = 2048)
    private String referrer;
    @Column(nullable = false, length = 100)
    private String visitorKey;

    protected ClickEvent() {}

    public ClickEvent(String linkCode, Instant timestamp, String country, String browser, String device,
            String referrer, String visitorKey) {
        this.linkCode = linkCode;
        this.timestamp = timestamp;
        this.country = country;
        this.browser = browser;
        this.device = device;
        this.referrer = referrer;
        this.visitorKey = visitorKey;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getCountry() { return country; }
    public String getReferrer() { return referrer; }
    public String getVisitorKey() { return visitorKey; }
}