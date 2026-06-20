package ru.mirea.repair.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.repair.dto.CategoryRequest;
import ru.mirea.repair.dto.CategoryResponse;
import ru.mirea.repair.entity.Category;
import ru.mirea.repair.exception.ApiException;
import ru.mirea.repair.repository.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Категория с таким названием уже существует");
        }
        Category category = new Category();
        apply(dto, category);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest dto) {
        Category category = findEntity(id);
        apply(dto, category);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findEntity(id);
        categoryRepository.delete(category);
    }

    private Category findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Категория не найдена"));
    }

    private void apply(CategoryRequest dto, Category category) {
        category.setName(dto.name().trim());
        category.setDescription(dto.description() == null ? null : dto.description().trim());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
