 package com.Zapiee.staff_service.service;

import com.Zapiee.staff_service.model.Staff;
import com.Zapiee.staff_service.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Staff staff = staffRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Staff not found with email: " + email
                        )
                );

        boolean enabled =
                Boolean.TRUE.equals(staff.getAccountEnabled());

        boolean locked = staff.isCurrentlyLocked()
                || !Boolean.TRUE.equals(staff.getAccountNonLocked());

        return User.builder()
                .username(staff.getEmail())
                .password(staff.getPin())
                .authorities(
                        staff.getRoles().stream()
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                role.getName()
                                        )
                                )
                                .collect(Collectors.toList())
                )
                .accountExpired(false)
                .accountLocked(locked)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }
}
