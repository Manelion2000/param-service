package com.bakouan.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class BaProductDto {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String  idCategorie;
    private String  nomCategorie;
}
