package com.khaled.ecommerce.userservice.service;

import com.khaled.ecommerce.userservice.model.User;
import com.khaled.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock // UserService now has two dependencies - both get mocked, same technique, just twice
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_withNewEmail_hashesPasswordBeforeSaving() {
        when(userRepository.existsByEmail("khaled@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext123")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.registerUser("khaled@example.com", "plaintext123", "Khaled", "S");

        // The actual point of this test: prove the RAW password never reaches save() unhashed
        assertThat(result.getPasswordHash()).isEqualTo("hashed-value");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("hashed-value")));
    }

    @Test
    void registerUser_withExistingEmail_failsFastWithoutHashingOrSaving() {
        when(userRepository.existsByEmail("khaled@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser("khaled@example.com", "pw", "Khaled", "S"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        // Confirms we bail out BEFORE doing the (deliberately slow) hashing work or touching save()
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withCorrectPassword_returnsUser() {
        User existing = new User("khaled@example.com", "hashed-value", "Khaled", "S");
        when(userRepository.findByEmail("khaled@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("plaintext123", "hashed-value")).thenReturn(true);

        User result = userService.login("khaled@example.com", "plaintext123");

        assertThat(result.getEmail()).isEqualTo("khaled@example.com");
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        User existing = new User("khaled@example.com", "hashed-value", "Khaled", "S");
        when(userRepository.findByEmail("khaled@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrongpw", "hashed-value")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("khaled@example.com", "wrongpw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withUnknownEmail_throwsSameExceptionTypeAsWrongPassword() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        // This IS the security property we care about: an unknown email and a wrong password must be
        // indistinguishable from outside. A different exception type here than the test above would BE the leak.
        assertThatThrownBy(() -> userService.login("nobody@example.com", "anything"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}