package com.dvault.shortener;

import com.dvault.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "short_urls")
public class ShortUrl {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;
    @Column(nullable = false, unique = true, length = 32)
    private String alias;
    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean active;

    protected ShortUrl() {}
    public ShortUrl(UserAccount owner, String alias, String targetUrl, Instant expiresAt, boolean active) {
        this.owner = owner;
        update(alias, targetUrl, expiresAt, active);
    }
    void update(String alias, String targetUrl, Instant expiresAt, boolean active) {
        this.alias = alias;
        this.targetUrl = targetUrl;
        this.expiresAt = expiresAt;
        this.active = active;
    }
    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public String getTargetUrl() { return targetUrl; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isActive() { return active; }
}
