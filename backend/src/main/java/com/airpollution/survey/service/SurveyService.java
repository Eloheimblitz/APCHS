package com.airpollution.survey.service;

import com.airpollution.survey.dto.SurveyCreateRequest;
import com.airpollution.survey.dto.SurveyResponse;
import com.airpollution.survey.dto.SurveyUpdateRequest;
import com.airpollution.survey.entity.SurveyRecord;
import com.airpollution.survey.repository.SurveyRecordRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SurveyService {
    private final SurveyRecordRepository repository;
    private final SurveyMapper mapper;

    public SurveyService(SurveyRecordRepository repository, SurveyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public SurveyResponse create(SurveyCreateRequest request, Authentication authentication) {
        SurveyRecord record = new SurveyRecord();
        mapper.copyPayload(request, record);
        record.setSubmittedBy(authentication.getName());
        record.setCreatedAt(OffsetDateTime.now());
        record.setUpdatedAt(OffsetDateTime.now());
        String surveyId = assignSurveyId(request.getSurveyId());
        record.setSurveyId(surveyId);
        record.setHouseholdId(surveyId.replace("APCHS", "HH"));
        return mapper.toResponse(repository.save(record));
    }

    @Transactional(readOnly = true)
    public List<SurveyResponse> list(Map<String, String> filters, Authentication authentication) {
        List<SurveyRecord> records = findFiltered(filters, authentication);
        return records.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SurveyRecord> findFiltered(Map<String, String> filters, Authentication authentication) {
        List<SurveyRecord> records = repository.findAll(specification(filters, authentication));
        if (hasValue(filters.get("symptom"))) {
            String symptom = filters.get("symptom");
            records = records.stream().filter(r -> hasSymptomPresent(r, symptom)).toList();
        }
        if (hasValue(filters.get("cookingFuel"))) {
            String fuel = filters.get("cookingFuel");
            records = records.stream().filter(r -> r.getPrimaryCookingFuel() != null && r.getPrimaryCookingFuel().contains(fuel)).toList();
        }
        return records;
    }

    private boolean hasSymptomPresent(SurveyRecord record, String symptomKey) {
        if (record.getSymptoms() == null) return false;
        return record.getSymptoms().stream()
                .anyMatch(entry -> symptomKey.equals(entry.getKey()) && Boolean.TRUE.equals(entry.getPresent()));
    }

    @Transactional(readOnly = true)
    public SurveyRecord getRecord(Long id, Authentication authentication) {
        SurveyRecord record = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Survey not found"));
        assertCanAccess(record, authentication);
        return record;
    }

    @Transactional(readOnly = true)
    public SurveyResponse get(Long id, Authentication authentication) {
        return mapper.toResponse(getRecord(id, authentication));
    }

    @Transactional
    public SurveyResponse update(Long id, SurveyUpdateRequest request, Authentication authentication) {
        SurveyRecord record = getRecord(id, authentication);
        mapper.copyPayload(request, record);
        record.setUpdatedAt(OffsetDateTime.now());
        return mapper.toResponse(repository.save(record));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        SurveyRecord record = getRecord(id, authentication);
        if (!isAdmin(authentication)) {
            throw new AccessDeniedException("Only admins can delete survey records");
        }
        repository.delete(record);
    }

    public Specification<SurveyRecord> specification(Map<String, String> filters, Authentication authentication) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addEquals(predicates, cb, root.get("studyArea"), filters.get("studyArea"));
            if (hasValue(filters.get("fromDate"))) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("surveyDate"), LocalDate.parse(filters.get("fromDate"))));
            }
            if (hasValue(filters.get("toDate"))) {
                predicates.add(cb.lessThanOrEqualTo(root.get("surveyDate"), LocalDate.parse(filters.get("toDate"))));
            }
            if (!isAdmin(authentication)) {
                predicates.add(cb.equal(root.get("submittedBy"), authentication.getName()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private String assignSurveyId(String requestedId) {
        String trimmed = requestedId.trim();
        if (repository.existsBySurveyId(trimmed)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Survey ID '" + trimmed + "' is already in use");
        }
        return trimmed;
    }

    private void assertCanAccess(SurveyRecord record, Authentication authentication) {
        if (!isAdmin(authentication) && !record.getSubmittedBy().equals(authentication.getName())) {
            throw new AccessDeniedException("You do not have permission to access this survey record");
        }
    }

    private void addEquals(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
                           jakarta.persistence.criteria.Path<String> path, String value) {
        if (hasValue(value)) {
            predicates.add(cb.equal(path, value));
        }
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
