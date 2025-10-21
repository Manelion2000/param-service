package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaProfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Repository
public interface BaProfilRepository extends JpaRepository<BaProfil, String> {

    /**
     * Filtre des profils par status.
     *
     * @param statut
     * @return la liste des profils
     */
    List<BaProfil> findByStatutOrderByCreatedDateDesc(EStatut statut);

}
