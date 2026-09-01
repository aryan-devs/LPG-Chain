package com.lpgchain.service;

import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public UserRoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    public List<UserRole> getAllUserRoles() {
        return userRoleRepository.findAll();
    }

    public Optional<UserRole> getUserRoleById(Long id) {
        return userRoleRepository.findById(id);
    }

    public UserRole saveUserRole(UserRole userRole) {
        return userRoleRepository.save(userRole);
    }

    public void deleteUserRole(Long id) {
        userRoleRepository.deleteById(id);
    }
}
