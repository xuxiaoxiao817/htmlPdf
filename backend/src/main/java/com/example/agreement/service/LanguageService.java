package com.example.agreement.service;

import com.example.agreement.dto.LanguageOption;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class LanguageService {

    private List<LanguageOption> languages = Collections.emptyList();

    @PostConstruct
    void load() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource("languages.json").getInputStream()) {
            languages = mapper.readValue(in, new TypeReference<List<LanguageOption>>() {});
            log.info("loaded {} language options", languages.size());
        } catch (IOException e) {
            log.error("failed to load languages.json", e);
        }
    }

    public List<LanguageOption> all() {
        return languages;
    }
}
