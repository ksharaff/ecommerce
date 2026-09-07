package com.khaled.ecommerce.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.khaled.ecommerce.userservice.model.User;
import com.khaled.ecommerce.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email, String rawPassword, String firstName, String lastName) {
        if(userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        String hashed = passwordEncoder.encode(rawPassword);
        User user = new User(email, hashed, firstName, lastName);
        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

        public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new); // unknown email -> SAME exception as wrong password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
