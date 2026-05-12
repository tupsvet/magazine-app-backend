package com.magazines.service

import com.magazines.data.repository.CategoryRepository
import com.magazines.domain.exception.BadRequestException
import com.magazines.domain.exception.ConflictException
import com.magazines.domain.exception.NotFoundException
import com.magazines.domain.model.Category

class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    fun list(): List<Category> = categoryRepository.findAll()

    fun getById(id: Int): Category =
        categoryRepository.findById(id)
            ?: throw NotFoundException("Category not found: $id")

    fun create(name: String, description: String?): Category {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            throw BadRequestException("Category name must not be empty")
        }
        val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryRepository.findByName(normalizedName) != null) {
            throw ConflictException("Category already exists: $normalizedName")
        }
        return categoryRepository.create(normalizedName, normalizedDescription)
    }

    fun update(id: Int, name: String?, description: String?): Category {
        val existing = categoryRepository.findById(id)
            ?: throw NotFoundException("Category not found: $id")

        val normalizedName = name?.trim()?.also {
            if (it.isEmpty()) throw BadRequestException("Category name must not be empty")
        }
        val normalizedDescription = description?.trim()

        if (normalizedName != null && normalizedName != existing.name) {
            val conflict = categoryRepository.findByName(normalizedName)
            if (conflict != null && conflict.id != id) {
                throw ConflictException("Category already exists: $normalizedName")
            }
        }

        return categoryRepository.update(id, normalizedName, normalizedDescription)
            ?: throw NotFoundException("Category not found: $id")
    }

    fun delete(id: Int) {
        val removed = categoryRepository.delete(id)
        if (!removed) {
            throw NotFoundException("Category not found: $id")
        }
    }
}
