package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaCategorie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaCategorieRepository extends JpaRepository<BaCategorie, String> {

    Boolean existsByCode(String code);

    List<BaCategorie> findByStatut(EStatut eStatus)
            ;
}
