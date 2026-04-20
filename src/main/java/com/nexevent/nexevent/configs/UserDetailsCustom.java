package com.nexevent.nexevent.configs;

import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.services.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;


@Component("userDetailsService")
//Or @Service
public class UserDetailsCustom implements UserDetailsService {
    private final UserService userService;
    public UserDetailsCustom(UserService userService) {
        this.userService = userService;
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = this.userService.getUserByEmail(email);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("username/password error");
        }
        var authority = new SimpleGrantedAuthority(user.get().getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.get().getEmail(),
                user.get().getPassword(),
                Collections.singletonList(authority)
        );
    }

}