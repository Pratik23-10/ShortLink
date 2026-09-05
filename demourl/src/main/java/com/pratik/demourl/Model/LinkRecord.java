package com.pratik.demourl.Model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "short_links")
public class LinkRecord {
    @Id
    @Column(length = 32, nullable = false, updatable = false)
    private String code;
    @Column(nullable = false, length = 2048)
    private String url;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean active;

    protected LinkRecord() {}

    public LinkRecord(String code, String url, Instant createdAt, Instant expiresAt, boolean active) {
        this.code = code;
        this.url = url;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public String getCode() { return code; }
    public String getUrl() { return url; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public boolean expiredAt(Instant now) { return expiresAt != null && !expiresAt.isAfter(now); }
}