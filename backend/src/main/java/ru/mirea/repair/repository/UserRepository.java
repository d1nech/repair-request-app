package ru.mirea.repair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.repair.entity.Role;
import ru.mirea.repair.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRoleOrderByFullNameAsc(Role role);
}
