package com.frist.assesspro.repository.specification;

import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class TestSpecifications {

    public static Specification<Test> byCreator(User creator) {
        return (root, query, cb) -> cb.equal(root.get("createdBy"), creator);
    }

    public static Specification<Test> byPublishedStatus(Boolean published) {
        return (root, query, cb) -> {
            if (published == null) return cb.conjunction();
            return cb.equal(root.get("isPublished"), published);
        };
    }

    public static Specification<Test> byTitleContaining(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
        };
    }

    public static Specification<Test> byCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return cb.conjunction();
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }
}
