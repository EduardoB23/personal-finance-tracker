package com.finance.tracker.category;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<Category> getCategories(@RequestParam(required = false) CategoryType type,
                                        @RequestParam(defaultValue = "false") boolean includeArchived) {
        return categoryService.getCategories(type, includeArchived);
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveCategory(@PathVariable Long id) {
        categoryService.archiveCategory(id);
        return ResponseEntity.noContent().build();
    }
}
