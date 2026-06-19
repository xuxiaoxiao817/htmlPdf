package com.example.agreement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageOption {
    private String code;
    private String name;
    private String nativeName;
}
