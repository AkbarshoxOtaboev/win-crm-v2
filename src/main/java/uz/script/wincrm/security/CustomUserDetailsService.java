package uz.script.wincrm.security;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.script.wincrm.roles.Role;
import uz.script.wincrm.users.User;
import uz.script.wincrm.users.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        Hibernate.initialize(user.getRoles());
        Hibernate.initialize(user.getFilial());
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                Hibernate.initialize(role.getPermissions());
            }
        }
        return new CustomUserDetails(user);
    }
}
