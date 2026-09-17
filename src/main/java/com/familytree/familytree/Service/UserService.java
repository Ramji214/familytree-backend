package com.familytree.familytree.Service;

import com.familytree.familytree.DTO.LoginRequest;
import com.familytree.familytree.Model.User;
import com.familytree.familytree.Repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ============================================================
    // REGISTER
    // ============================================================

    public User registerUser(User user) {

        // New users must start with profileCompleted = false
        user.setProfileCompleted(false);

        // Encrypt password before saving
        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        return userRepository.save(user);
    }

    // ============================================================
    // LOGIN
    // ============================================================

    public User loginUser(LoginRequest loginRequest) {

        User user = userRepository.findByUsername(
                loginRequest.getUsername()
        );

        // Username not found
        if (user == null) {
            return null;
        }

        // Check raw password against encrypted password
        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword()
        )) {
            return null;
        }

        return user;
    }
}