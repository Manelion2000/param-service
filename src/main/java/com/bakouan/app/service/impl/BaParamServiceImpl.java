package com.bakouan.app.service.impl;

import com.bakouan.app.dto.BaCategorieDto;
import com.bakouan.app.dto.BaLogDto;
import com.bakouan.app.dto.BaProductDto;
import com.bakouan.app.enums.EAction;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaCategorie;
import com.bakouan.app.model.BaProduct;
import com.bakouan.app.repositories.BaCategorieRepository;
import com.bakouan.app.repositories.BaProductRepository;
import com.bakouan.app.service.BaLogService;
import com.bakouan.app.service.BaParamService;
import com.bakouan.app.utils.BaUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Slf4j
@Service
public class BaParamServiceImpl implements BaParamService {

    private final BaCategorieRepository baCategorieRepository;
    private final BaProductRepository productRepository;
    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);
    private final BaLogService logService;

    @Override
    public List<BaCategorieDto> getAllCategories() {
        logService.log(new BaLogDto(EAction.V, "Catégories"));
        return baCategorieRepository.findByStatut(EStatut.A)
                .stream()
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    @Override
    public List<BaCategorieDto> getAllCategoriesArchive() {
        logService.log(new BaLogDto(EAction.V, "Catégories Archivées"));

        return baCategorieRepository.findByStatut(EStatut.D)
                .stream()
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    @Override
    public BaCategorieDto getCategorieById(final String id) {
        logService.log(new BaLogDto(EAction.V, "Catégories " + id));

        return baCategorieRepository.findById(id).map(mapper::maps)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, " cette categorie n'existe pas"));
    }

    @Override
    public BaCategorieDto createCategorie(final BaCategorieDto categorieDto) {
        logService.log(new BaLogDto(EAction.C, "Catégories " + categorieDto.getCode()));

        Boolean exist = this.baCategorieRepository.existsByCode(categorieDto.getCode());
        if (exist) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "il existe une categorie avec ce code");
        }
        BaCategorie categorie = mapper.maps(categorieDto);
        categorie.setId(BaUtils.randomUUID());

        BaCategorie savedCategorie = baCategorieRepository.save(categorie);
        return mapper.maps(savedCategorie);
    }

    @Override
    public BaCategorieDto updateCategorie(final String id, final BaCategorieDto categorieDto) {
        logService.log(new BaLogDto(EAction.U, "Catégories " + categorieDto.getCode()));

        boolean existingCategorie = baCategorieRepository.existsById(id);
        if (existingCategorie) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cette categorie n'existe pas");
        }
        BaCategorie entitie = mapper.maps(categorieDto);
        entitie = baCategorieRepository.save(entitie);
        return mapper.maps(entitie);
    }

    @Override
    public void deleteCategorie(final String id) {
        logService.log(new BaLogDto(EAction.D, "Catégories " + id));

        boolean existingCategorie = baCategorieRepository.existsById(id);
        if (existingCategorie) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cette categorie n'existe pas");
        }
        List<BaProduct> products = productRepository.findByCategorieId(id);
        productRepository.deleteAll(products);
        baCategorieRepository.deleteById(id);
    }

    @Override
    public void deleteCategorieLogique(final String id) {
        logService.log(new BaLogDto(EAction.D, "Catégories " + id));
        boolean existingCategorie = baCategorieRepository.existsById(id);
        if (!existingCategorie) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cette categorie n'existe pas");
        }
        BaCategorie categorie = baCategorieRepository.getReferenceById(id);
        categorie.setStatut(EStatut.D);
        baCategorieRepository.save(categorie);
    }

    @Override
    public List<BaProductDto> getAllProducts() {
        logService.log(new BaLogDto(EAction.V, "Produits"));

        return productRepository.findAll().stream()
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    @Override
    public BaProductDto getProductById(final String id) {
        logService.log(new BaLogDto(EAction.V, "Produits " + id));
        BaProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "BaProduct not found"));
        return mapper.maps(product);
    }

    @Override
    public BaProductDto createProduct(final BaProductDto productDTO) {
        logService.log(new BaLogDto(EAction.C, "Produits " + productDTO.getName()));
        BaProduct product = mapper.maps(productDTO);
        product.setId(BaUtils.randomUUID());
        BaProduct savedProduct = productRepository.save(product);
        return mapper.maps(savedProduct);
    }

    @Override
    public BaProductDto updateProduct(final String id, final BaProductDto productDTO) {
        logService.log(new BaLogDto(EAction.U, "Produits " + productDTO.getName()));

        BaProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "BaProduct not found"));
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        BaProduct updatedProduct = productRepository.save(product);
        return mapper.maps(updatedProduct);
    }

    @Override
    public void deleteProduct(final String id) {
        logService.log(new BaLogDto(EAction.D, "Produits " + id));
        BaProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "BaProduct not found"));
        productRepository.delete(product);
    }

}
