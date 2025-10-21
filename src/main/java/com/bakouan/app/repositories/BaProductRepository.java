package com.bakouan.app.repositories;

import com.bakouan.app.model.BaProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaProductRepository extends JpaRepository<BaProduct,String> {
    boolean existsById(String id);

    List<BaProduct> findByCategorieId(String id);
}
