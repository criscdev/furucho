package com.robertafurucho.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Simple in-memory user details service for admin authentication.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private final PasswordEncoder passwordEncoder;
    
    // Cache the encoded password to avoid re-encoding on every authentication
    private volatile String encodedPassword;

    public AdminUserDetailsService(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username.equals(adminUsername)) {
            // Lazy initialize the encoded password on first use
            if (encodedPassword == null) {
                synchronized (this) {
                    if (encodedPassword == null) {
                        encodedPassword = passwordEncoder.encode(adminPassword);
                    }
                }
            }
            
            return User.builder()
                    .username(adminUsername)
                    .password(encodedPassword)
                    .roles("ADMIN")
                    .build();
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
