package com.airpollution.survey.repository;

import com.airpollution.survey.entity.SurveyRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyRecordRepository extends JpaRepository<SurveyRecord, Long>, JpaSpecificationExecutor<SurveyRecord> {
    Optional<SurveyRecord> findTopBySurveyDateBetweenOrderByIdDesc(LocalDate start, LocalDate end);
    boolean existsBySurveyId(String surveyId);
    long countBySurveyIdStartingWith(String prefix);

    @Query(value = "SELECT survey_id FROM survey_records WHERE survey_id LIKE CONCAT(:prefix, '%')", nativeQuery = true)
    List<String> findSurveyIdsStartingWith(@Param("prefix") String prefix);

    @Query(value = "SELECT id FROM survey_records WHERE primary_cooking_fuel @> jsonb_build_array(CAST(:fuel AS text))", nativeQuery = true)
    List<Long> findIdsByCookingFuel(@Param("fuel") String fuel);

    @Query(value = "SELECT id FROM survey_records WHERE EXISTS ("
            + "SELECT 1 FROM jsonb_array_elements(COALESCE(symptoms, '[]'::jsonb)) elem "
            + "WHERE elem->>'key' = :symptomKey AND (elem->>'present')::boolean IS TRUE)", nativeQuery = true)
    List<Long> findIdsBySymptomPresent(@Param("symptomKey") String symptomKey);
}
