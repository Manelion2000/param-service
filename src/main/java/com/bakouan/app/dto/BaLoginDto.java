package com.bakouan.app.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * View Model object for storing a user's credentials.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaLoginDto {

    @NotEmpty
    @Size(min = 1, max = 50)
    private String username;

    @NotEmpty
    @Size(min = 1, max = 50)
    private String password;

    private Boolean rememberMe;

}
