package com.finance.tracker.category;

import com.finance.tracker.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getCategories(CategoryType type, boolean includeArchived) {
        return categoryRepository.findAll().stream()
                .filter(category -> type == null || category.getType() == type)
                .filter(category -> includeArchived || !category.isArchived())
                .sorted(Comparator.comparing(Category::getType).thenComparing(Category::getName))
                .toList();
    }

    @Transactional
    public Category createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndType(name, request.type())) {
            throw new IllegalArgumentException("Categoria já existe");
        }
        return categoryRepository.save(new Category(name, request.type()));
    }

    @Transactional
    public void archiveCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        category.setArchived(true);
    }

    // Categoria válida para um novo lançamento: existe, não está arquivada e é do mesmo tipo
    public Category requireForTransaction(Long categoryId, TransactionType transactionType) {
        Category category = categoryRepository.findById(categoryId)
                .filter(found -> !found.isArchived())
                .filter(found -> found.getType().name().equals(transactionType.name()))
                .orElse(null);
        if (category == null) {
            throw new IllegalArgumentException("Categoria inválida para este lançamento");
        }
        return category;
    }
}
