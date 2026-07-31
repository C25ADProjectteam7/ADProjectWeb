package com.expensehub.webbackend.controller;

import com.expensehub.webbackend.entity.User;
import com.expensehub.webbackend.repository.UserRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 对应 backlog Item 2：管理员账号功能（查看、创建、编辑、禁用、启用其他账号）。
 * 目前只是骨架，后续补充 DTO、参数校验和权限细分。
 */
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
