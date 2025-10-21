package com.bakouan.app.service;

import com.bakouan.app.dto.BaCategorieDto;
import com.bakouan.app.dto.BaProductDto;
import org.springframework.stereotype.Service;

import java.util.List;

public interface BaParamService {
    List<BaCategorieDto> getAllCategories();

    List<BaCategorieDto> getAllCategoriesArchive();

    BaCategorieDto getCategorieById(String id);

    BaCategorieDto createCategorie(BaCategorieDto categorieDto);

    BaCategorieDto updateCategorie(String id, BaCategorieDto categorieDto);

    void deleteCategorie(String id);

    void deleteCategorieLogique(String id);

    List<BaProductDto> getAllProducts();

    BaProductDto getProductById(String id);

    BaProductDto createProduct(BaProductDto productDto);

    BaProductDto updateProduct(String id, BaProductDto productDTO);

    void deleteProduct(String id);
}
