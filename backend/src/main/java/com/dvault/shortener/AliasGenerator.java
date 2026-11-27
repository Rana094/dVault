package com.dvault.shortener;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class AliasGenerator {
    private final SecureRandom random = new SecureRandom();

    public String nextAlias() {
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
