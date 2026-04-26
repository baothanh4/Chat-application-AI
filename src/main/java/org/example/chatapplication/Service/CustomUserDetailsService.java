package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Model.Enum.UserRole;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        UserRole role = user.getRole() == null ? UserRole.USER : user.getRole();

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash() == null ? "" : user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role.name())))
                .accountLocked(user.isAccountLocked())
                .build();
    }
}

