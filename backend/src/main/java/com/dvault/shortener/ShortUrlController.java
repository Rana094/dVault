package com.dvault.shortener;

import com.dvault.security.AccountPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
public class ShortUrlController {
    private final ShortUrlService service;
    public ShortUrlController(ShortUrlService service) { this.service = service; }
    public record UrlPage(List<ShortUrlView> content, int page, int size, long totalElements, int totalPages) {}

    @PostMapping
    public ResponseEntity<ShortUrlView> create(@AuthenticationPrincipal AccountPrincipal user,
                                               @Valid @RequestBody ShortUrlInput input) {
        var result = service.create(user.getAccountId(), input);
        return ResponseEntity.created(URI.create("/api/urls/" + result.id())).body(result);
    }
    @GetMapping
    public UrlPage list(@AuthenticationPrincipal AccountPrincipal user,
                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        var result = service.list(user.getAccountId(), page, size);
        return new UrlPage(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }
    @GetMapping("/{id}")
    public ShortUrlView get(@AuthenticationPrincipal AccountPrincipal user, @PathVariable Long id) {
        return service.get(user.getAccountId(), id);
    }
    @PutMapping("/{id}")
    public ShortUrlView update(@AuthenticationPrincipal AccountPrincipal user, @PathVariable Long id,
                               @Valid @RequestBody ShortUrlInput input) {
        return service.update(user.getAccountId(), id, input);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AccountPrincipal user, @PathVariable Long id) {
        service.delete(user.getAccountId(), id);
    }
}
