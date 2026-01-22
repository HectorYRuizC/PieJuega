package com.example.PieJuega.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserDetailsImpl implements UserDetails, OAuth2User {

    private final Long id;
    private final String email;
    private final String password;
    private final Set<String> roles;

    // Map para atributos OAuth2
    private Map<String, Object> attributes;

    private UserDetailsImpl(Long id, String email, String password, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }


    // 🔹 Método estático para crear UserDetailsImpl desde User  //viejo
    public static UserDetailsImpl build(com.example.PieJuega.model.User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        return new UserDetailsImpl(user.getId(), user.getEmail(), user.getPassword(), roleNames);
    }

    // Para construir UserDetails a partir de un OAuth2User
    public static UserDetailsImpl build(com.example.PieJuega.model.User user, Map<String, Object> attributes) {
        UserDetailsImpl userDetails = build(user); // usa tu build normal
        userDetails.setAttributes(attributes);
        return userDetails;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }


    //viejo
    public Long getId() { return id; }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    //viejo
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }



    //viejo
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public String getName() {
        return email;
    }
}
