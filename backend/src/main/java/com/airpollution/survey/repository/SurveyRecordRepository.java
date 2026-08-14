package com.airpollution.survey.repository;

import com.airpollution.survey.entity.SurveyRecord;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SurveyRecordRepository extends JpaRepository<SurveyRecord, Long>, JpaSpecificationExecutor<SurveyRecord> {
    Optional<SurveyRecord> findTopBySurveyDateBetweenOrderByIdDesc(LocalDate start, LocalDate end);
    boolean existsBySurveyId(String surveyId);
    long countBySurveyIdStartingWith(String prefix);
}
