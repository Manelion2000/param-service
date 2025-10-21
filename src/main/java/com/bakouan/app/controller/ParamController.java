package com.bakouan.app.controller;

import com.bakouan.app.dto.BaCategorieDto;
import com.bakouan.app.dto.BaProductDto;
import com.bakouan.app.service.BaFileStorageService;
import com.bakouan.app.service.BaParamService;
import com.bakouan.app.utils.BaConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(BaConstants.URL.BASE_URL)
public class ParamController {

    private final BaParamService paramService;
    private final BaFileStorageService fileStorage;


    /**
     * Récupérer un fichier/image/document.
     *
     * @param id : l'id du fichier
     * @return {@link ResponseEntity}
     */
    @GetMapping(BaConstants.URL.DOCUMENT + "/{id}")
    public ResponseEntity<byte[]> loadFile(@PathVariable final String id) {
        return new ResponseEntity<>(fileStorage.get(id), HttpStatus.OK);
    }


    @GetMapping(value = BaConstants.URL.CATEGORIE)
    public List<BaCategorieDto> getAllCategories() {
        return paramService.getAllCategories();
    }

    @GetMapping(value = BaConstants.URL.CATEGORIE + "/{id}")
    public ResponseEntity<BaCategorieDto> getCategorieById(@PathVariable("id") String idCategorie) {
        return ResponseEntity.ok(paramService.getCategorieById(idCategorie));
    }

    @PostMapping(value = BaConstants.URL.CATEGORIE)
    public ResponseEntity<BaCategorieDto> createCategorie(@RequestBody @Valid BaCategorieDto categorieDto) {
        BaCategorieDto objet = paramService.createCategorie(categorieDto);
        return new ResponseEntity(objet, HttpStatus.CREATED);
    }

    @PutMapping(value = BaConstants.URL.CATEGORIE + "/{id}")
    public ResponseEntity<BaCategorieDto> updateCategorie(@PathVariable String id, @RequestBody BaCategorieDto categorieDto) {
        return ResponseEntity.ok(paramService.updateCategorie(id, categorieDto));
    }

    @DeleteMapping(value = BaConstants.URL.CATEGORIE + "/{id}")
    public ResponseEntity<Void> deleteCategorie(@PathVariable String id) {
        paramService.deleteCategorieLogique(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * gestion des produits
     */
    @GetMapping(value = BaConstants.URL.PRODUCT)
    public List<BaProductDto> getAllProducts() {
        return paramService.getAllProducts();
    }

    @GetMapping(value = BaConstants.URL.PRODUCT + "/{id}")
    public ResponseEntity<BaProductDto> getProductById(@PathVariable("id") String idProduct) {
        return ResponseEntity.ok(paramService.getProductById(idProduct));
    }

    @PostMapping(value = BaConstants.URL.PRODUCT)
    public ResponseEntity<BaProductDto> createProduct(@RequestBody @Valid BaProductDto productDto) {
        BaProductDto objet = paramService.createProduct(productDto);
        return new ResponseEntity(objet, HttpStatus.CREATED);
    }

    @PutMapping(value = BaConstants.URL.PRODUCT + "/{id}")
    public ResponseEntity<BaProductDto> updateProduct(@PathVariable String id, @RequestBody BaProductDto productDto) {
        return ResponseEntity.ok(paramService.updateProduct(id, productDto));
    }

    @DeleteMapping(value = BaConstants.URL.PRODUCT + "/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        paramService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
