package com.expensehub.webbackend.service.impl;

import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.repository.UserRepository;
import com.expensehub.webbackend.security.HashUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user =
                userRepository
                        .findByEmailHash(HashUtil.sha256Hex(email.toLowerCase().trim()))
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "User not found: " + email));

        UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(user.getEmail());
        builder.password(user.getPasswordHash());
        builder.roles(user.getRole().name());
        builder.disabled(!user.isEnabled());
        return builder.build();
    }
}
