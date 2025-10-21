package com.bakouan.app.mapper;

import com.bakouan.app.dto.BaCategorieDto;
import com.bakouan.app.dto.BaLogDto;
import com.bakouan.app.dto.BaProductDto;
import com.bakouan.app.dto.BaProfilDto;
import com.bakouan.app.dto.BaRoleDto;
import com.bakouan.app.dto.BaUserDto;
import com.bakouan.app.model.BaCategorie;
import com.bakouan.app.model.BaLog;
import com.bakouan.app.model.BaProduct;
import com.bakouan.app.model.BaProfil;
import com.bakouan.app.model.BaRole;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.utils.BaUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
public interface YtMapper {
    @Mappings({
            @Mapping(target = "idCategorie", source = "categorie.id"),
            @Mapping(target = "nomCategorie", source = "categorie.nom"),
    })
    BaProductDto maps(BaProduct entity);

    @InheritInverseConfiguration
    BaProduct maps(BaProductDto dto);

    @Mappings({})
    BaLogDto maps(BaLog entity);

    @InheritInverseConfiguration
    BaLog maps(BaLogDto dto);

    @Mappings({})
    BaCategorieDto maps(BaCategorie entity);

    @InheritInverseConfiguration
    BaCategorie maps(BaCategorieDto dto);

    /**
     * Convertir une entité user en DTO.
     *
     * @param entity
     * @return le dto
     */
    @Mappings({
            @Mapping(source = "profil.id", target = "idProfil"),
            @Mapping(source = "profil.libelle", target = "libelleProfil"),
    })
    BaUserDto maps(BaUser entity);

    /**
     * Convertir une entité user en DTO.
     *
     * @param entity
     * @return le dto
     */
    @Mappings({})
    BaRoleDto maps(BaRole entity);

    /**
     * Convertir une entité profil  en DTO.
     *
     * @param entity
     * @return le dto
     */
    @Mappings({})
    BaProfilDto maps(BaProfil entity);

    /**
     * Convertir un DTO user en entité.
     *
     * @param dto
     * @return le dto
     */
    @InheritInverseConfiguration
    BaUser maps(BaUserDto dto);

    /**
     * Convertir un DTO Role en entité.
     *
     * @param dto
     * @return le dto
     */
    @InheritInverseConfiguration
    BaRole maps(BaRoleDto dto);

    /**
     * Convertir un dto Profil en entité.
     *
     * @param dto
     * @return le dto
     */
    @InheritInverseConfiguration
    BaProfil maps(BaProfilDto dto);

    /**
     * After mapping method.
     *
     * @param dto    dto
     * @param entity entity
     */
    @AfterMapping()
    default void afterMapping(final BaUserDto dto,
                              @MappingTarget BaUser entity) {
        if (dto == null) {
            return;
        }

        if (BaUtils.isEmpty(dto.getIdProfil())) {
            entity.setProfil(null);
        }
    }

}
