package com.bakouan.app.repositories;

import com.bakouan.app.enums.EStatut;
import com.bakouan.app.model.BaUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Repository
public interface BaUserRepository extends JpaRepository<BaUser, String> {

    /**
     * Recherche un utilisateur par son login.
     *
     * @param username
     * @param statut
     * @return L'utilisateur ou un objet vide
     */
    Optional<BaUser> findOneByUsernameIgnoreCaseAndStatut(String username, EStatut statut);

    /**
     * Recherche un utilisateur par son email.
     *
     * @param email
     * @param statut
     * @return L'utilisateur ou un objet vide
     */
    Optional<BaUser> findOneByEmailAndStatut(String email, EStatut statut);

    /**
     * Vérifie l'existence d'un utilisateur à partir de son
     * nom d'utilisateur.
     *
     * @param username
     * @return un boolean.
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Vérifie l'existence d'un utilisateur à partir du numéro de téléphone.
     *
     * @param tel
     * @return un boolean.
     */
    boolean existsByTelephoneIgnoreCase(String tel);


    /**
     * Vérifie l'existence d'un utilisateur à partir de l'email.
     *
     * @param email
     * @return un boolean.
     */
    boolean existsByEmailIgnoreCase(String email);


    /**
     * Controler la duplication de l'utilisateur
     * par téléphone.
     *
     * @param id
     * @param telephone
     * @return un booléen
     */
    @Query("SELECT COUNT(*) > 0 FROM BaUser e "
            + "WHERE  (:id IS NULL AND upper(e.telephone) = upper(:telephone)) "
            + "OR (:id IS NOT NULL AND e.id != :id AND upper(e.telephone) = upper(:telephone))")
    Boolean checkDuplicateTelephone(@Param("id") String id, @Param("telephone") String telephone);

    /**
     * Controler la duplication de l'utilisateur
     * par email.
     *
     * @param id
     * @param email
     * @return un booléen
     */
    @Query("SELECT COUNT(*) > 0 FROM BaUser e "
            + "WHERE  (:id IS NULL AND upper(e.email) = upper(:email)) "
            + "OR (:id IS NOT NULL AND e.id != :id AND upper(e.email) = upper(:email))")
    Boolean checkDuplicateEmail(@Param("id") String id, @Param("email") String email);


    /**
     * Liste des utilisateurs pour plusieurs critères.
     *
     * @param statut
     * @param telephone
     * @param username
     * @param id
     * @return Liste des utilisateurs.
     */
    @Query(value = "SELECT * "
            + "FROM ba_utilisateur u "
            + "WHERE u.statut = :statut "
            + "  AND (:id = '' OR u.id = :id) "
            + "  AND (:username = '' OR u.username like '%' || :username || '%') "
            + "  AND (:telephone = '' OR u.telephone like '%' || :telephone || '%') "
            + "ORDER BY u.created_date DESC ", nativeQuery = true)
    Stream<BaUser> fetchMulticrites(
            @Param("statut") String statut,
            @Param("id") String id,
            @Param("telephone") String telephone,
            @Param("username") String username
    );


    /**
     * Récupérer l'utilisateur selon le numéro.
     *
     * @param telephone
     * @param statut
     * @return L'utilisateur
     */
    Optional<BaUser> findOneByTelephoneAndStatut(String telephone, EStatut statut);

}
