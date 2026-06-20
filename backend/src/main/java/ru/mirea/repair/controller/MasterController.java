package ru.mirea.repair.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.repair.dto.MasterOptionResponse;
import ru.mirea.repair.entity.Role;
import ru.mirea.repair.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/masters")
public class MasterController {
    private final UserRepository userRepository;

    public MasterController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public List<MasterOptionResponse> findAll() {
        return userRepository.findAllByRoleOrderByFullNameAsc(Role.MASTER).stream()
                .map(user -> new MasterOptionResponse(user.getId(), user.getEmail(), user.getFullName()))
                .toList();
    }
}
