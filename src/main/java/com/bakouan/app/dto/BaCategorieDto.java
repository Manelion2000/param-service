package com.bakouan.app.dto;

import com.bakouan.app.enums.EStatut;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BaCategorieDto {
    private String id;
    @NotBlank
    private String code;
    @NotBlank(message = "le nom est obligatoire")
    private String nom;

}
