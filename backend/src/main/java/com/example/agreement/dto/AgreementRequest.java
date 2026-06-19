package com.example.agreement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgreementRequest {

    @NotBlank
    @Size(max = 10)
    private String language;

    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;
}
