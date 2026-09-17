package com.familytree.familytree.Controller;

import com.familytree.familytree.DTO.LoginRequest;
import com.familytree.familytree.DTO.UserResponse;
import com.familytree.familytree.Model.User;
import com.familytree.familytree.Service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ============================================================
    // REGISTER
    // ============================================================

    @PostMapping("/register")
    public UserResponse registerUser(
            @RequestBody User user) {

        User registeredUser =
                userService.registerUser(user);

        return convertToResponse(registeredUser);
    }

    // ============================================================
    // LOGIN
    // ============================================================

    @PostMapping("/login")
    public UserResponse loginUser(
            @RequestBody LoginRequest loginRequest) {

        User user =
                userService.loginUser(loginRequest);

        if (user == null) {
            return null;
        }

        return convertToResponse(user);
    }

    // ============================================================
    // CONVERT USER → USER RESPONSE
    // ============================================================

    private UserResponse convertToResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.isProfileCompleted()
        );
    }
}