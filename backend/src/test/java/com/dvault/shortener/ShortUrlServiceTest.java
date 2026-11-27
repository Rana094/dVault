package com.dvault.shortener;

import com.dvault.user.UserAccountRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {
    @Mock ShortUrlRepository urls;
    @Mock UserAccountRepository users;
    @Mock AliasGenerator generator;
    ShortUrlService service;
    @BeforeEach void setup() { service = new ShortUrlService(urls, users, generator); }

    @Test void retriesGeneratedAliasCollisionBeforeSaving() {
        when(generator.nextAlias()).thenReturn("taken123", "available123");
        when(urls.existsByAlias("taken123")).thenReturn(true);
        when(urls.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create(42L, new ShortUrlInput("https://example.com", "", null, null));
        assertEquals("available123", result.alias());
        assertTrue(result.active());
        verify(generator, times(2)).nextAlias();
        verify(users).getReferenceById(42L);
    }

    @Test void reportsCustomAliasAndDatabaseRaceConflicts() {
        var input = new ShortUrlInput("https://example.com", "Chosen", null, true);
        when(urls.existsByAlias("chosen")).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ShortUrlException.class, () -> service.create(42L, input)).status);
        verify(urls, never()).saveAndFlush(any());
        when(urls.existsByAlias("chosen")).thenReturn(false);
        when(urls.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("alias race"));
        assertEquals("ALIAS_CONFLICT", assertThrows(ShortUrlException.class, () -> service.create(42L, input)).code);
    }

    @Test void deniesNonOwnerDetailUpdateAndDeletionWithoutMutation() {
        when(urls.findByIdAndOwnerId(7L, 42L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ShortUrlException.class, () -> service.get(42L, 7L)).status);
        assertThrows(ShortUrlException.class, () -> service.update(42L, 7L, new ShortUrlInput("https://example.com", "valid", null, true)));
        assertThrows(ShortUrlException.class, () -> service.delete(42L, 7L));
        verify(urls, never()).saveAndFlush(any());
        verify(urls, never()).delete(any());
    }
}
