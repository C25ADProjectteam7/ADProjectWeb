package com.expensehub.webbackend.service;

import com.expensehub.webbackend.dto.CreateUserRequest;
import com.expensehub.webbackend.dto.UpdateUserRequest;
import com.expensehub.webbackend.entity.User;
import java.util.List;

public interface AdminUserService {

    List<User> listUsers();

    User createUser(CreateUserRequest request);

    User updateUser(Long id, UpdateUserRequest request);

    User updateUserStatus(Long id, boolean enabled);

    User unlockUser(Long id);
}