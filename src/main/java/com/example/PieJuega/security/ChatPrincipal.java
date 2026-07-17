package com.example.PieJuega.security;

import java.security.Principal;

public record ChatPrincipal(Long userId, String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
