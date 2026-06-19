package com.example.agreement.service;

import com.example.agreement.dto.AgreementRequest;
import com.example.agreement.entity.Agreement;
import com.example.agreement.exception.NotFoundException;
import com.example.agreement.repository.AgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgreementService {

    private final AgreementRepository repository;

    public List<Agreement> list() {
        return repository.findAll();
    }

    public Agreement get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("agreement " + id + " not found"));
    }

    @Transactional
    public Agreement create(AgreementRequest req) {
        Agreement a = new Agreement();
        a.setLanguage(req.getLanguage());
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        return repository.save(a);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("agreement " + id + " not found");
        }
        repository.deleteById(id);
    }
}
