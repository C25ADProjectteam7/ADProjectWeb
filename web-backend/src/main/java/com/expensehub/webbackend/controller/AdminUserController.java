package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.repository.UserRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

// Corresponding to backlog Item 2: Administrator account functions (viewing, creating, editing, disabling, and enabling other accounts).
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @PatchMapping("/{id}/status")
    public User toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(enabled);
        return userRepository.save(user);
    }
}
