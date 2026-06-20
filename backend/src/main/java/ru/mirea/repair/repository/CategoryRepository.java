package ru.mirea.repair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.repair.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
}
