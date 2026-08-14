package com.airpollution.survey.dto;

import com.airpollution.survey.entity.HealthItemEntry;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SurveyPayload {
    @NotBlank
    private String surveyId;
    @NotNull
    private LocalDate surveyDate;
    @NotBlank
    private String surveyorId;
    @AssertTrue(message = "Survey cannot be submitted without consent.")
    private Boolean consentObtained;
    @NotBlank
    private String studyArea;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal gpsAccuracy;
    private String gridId;
    private String distanceToHighway;
    private String distanceToFactory;

    @NotNull
    @Min(0)
    private Integer age;
    private String durationOfStayAtStudyArea;
    @NotBlank
    private String gender;
    private List<String> tobaccoUse;
    private Boolean alcohol;
    private String ethnicity;
    private String otherEthnicity;
    private String education;
    private String otherEducation;
    private List<String> occupation;
    private String otherOccupation;

    @NotEmpty
    private List<String> primaryCookingFuel;
    private List<String> woodCoalCookingLocation;

    private Boolean hasChildren;
    @Min(0)
    private Integer numberOfChildren;
    private List<String> childBirthplace;
    private String childVaccination;
    private Boolean respondentVaccination;
    private Boolean mhisSmartCard;

    private List<HealthItemEntry> conditions;
    private String cancerType;

    private List<HealthItemEntry> symptoms;
    private String feverDuration;

    private List<HealthItemEntry> otherIssues;

    private String remarks;

    @AssertFalse(message = "Other ethnicity is required when ethnicity is OTHER")
    public boolean isOtherEthnicityMissing() {
        return "OTHER".equals(ethnicity) && isBlank(otherEthnicity);
    }

    @AssertFalse(message = "Other education is required when education is OTHER")
    public boolean isOtherEducationMissing() {
        return "OTHER".equals(education) && isBlank(otherEducation);
    }

    @AssertFalse(message = "Other occupation is required when occupation is OTHER")
    public boolean isOtherOccupationMissing() {
        return occupation != null && occupation.contains("OTHER") && isBlank(otherOccupation);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getSurveyId() { return surveyId; }
    public void setSurveyId(String surveyId) { this.surveyId = surveyId; }
    public LocalDate getSurveyDate() { return surveyDate; }
    public void setSurveyDate(LocalDate surveyDate) { this.surveyDate = surveyDate; }
    public String getSurveyorId() { return surveyorId; }
    public void setSurveyorId(String surveyorId) { this.surveyorId = surveyorId; }
    public Boolean getConsentObtained() { return consentObtained; }
    public void setConsentObtained(Boolean consentObtained) { this.consentObtained = consentObtained; }
    public String getStudyArea() { return studyArea; }
    public void setStudyArea(String studyArea) { this.studyArea = studyArea; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getGpsAccuracy() { return gpsAccuracy; }
    public void setGpsAccuracy(BigDecimal gpsAccuracy) { this.gpsAccuracy = gpsAccuracy; }
    public String getGridId() { return gridId; }
    public void setGridId(String gridId) { this.gridId = gridId; }
    public String getDistanceToHighway() { return distanceToHighway; }
    public void setDistanceToHighway(String distanceToHighway) { this.distanceToHighway = distanceToHighway; }
    public String getDistanceToFactory() { return distanceToFactory; }
    public void setDistanceToFactory(String distanceToFactory) { this.distanceToFactory = distanceToFactory; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getDurationOfStayAtStudyArea() { return durationOfStayAtStudyArea; }
    public void setDurationOfStayAtStudyArea(String durationOfStayAtStudyArea) { this.durationOfStayAtStudyArea = durationOfStayAtStudyArea; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public List<String> getTobaccoUse() { return tobaccoUse; }
    public void setTobaccoUse(List<String> tobaccoUse) { this.tobaccoUse = tobaccoUse; }
    public Boolean getAlcohol() { return alcohol; }
    public void setAlcohol(Boolean alcohol) { this.alcohol = alcohol; }
    public String getEthnicity() { return ethnicity; }
    public void setEthnicity(String ethnicity) { this.ethnicity = ethnicity; }
    public String getOtherEthnicity() { return otherEthnicity; }
    public void setOtherEthnicity(String otherEthnicity) { this.otherEthnicity = otherEthnicity; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getOtherEducation() { return otherEducation; }
    public void setOtherEducation(String otherEducation) { this.otherEducation = otherEducation; }
    public List<String> getOccupation() { return occupation; }
    public void setOccupation(List<String> occupation) { this.occupation = occupation; }
    public String getOtherOccupation() { return otherOccupation; }
    public void setOtherOccupation(String otherOccupation) { this.otherOccupation = otherOccupation; }
    public List<String> getPrimaryCookingFuel() { return primaryCookingFuel; }
    public void setPrimaryCookingFuel(List<String> primaryCookingFuel) { this.primaryCookingFuel = primaryCookingFuel; }
    public List<String> getWoodCoalCookingLocation() { return woodCoalCookingLocation; }
    public void setWoodCoalCookingLocation(List<String> woodCoalCookingLocation) { this.woodCoalCookingLocation = woodCoalCookingLocation; }
    public Boolean getHasChildren() { return hasChildren; }
    public void setHasChildren(Boolean hasChildren) { this.hasChildren = hasChildren; }
    public Integer getNumberOfChildren() { return numberOfChildren; }
    public void setNumberOfChildren(Integer numberOfChildren) { this.numberOfChildren = numberOfChildren; }
    public List<String> getChildBirthplace() { return childBirthplace; }
    public void setChildBirthplace(List<String> childBirthplace) { this.childBirthplace = childBirthplace; }
    public String getChildVaccination() { return childVaccination; }
    public void setChildVaccination(String childVaccination) { this.childVaccination = childVaccination; }
    public Boolean getRespondentVaccination() { return respondentVaccination; }
    public void setRespondentVaccination(Boolean respondentVaccination) { this.respondentVaccination = respondentVaccination; }
    public Boolean getMhisSmartCard() { return mhisSmartCard; }
    public void setMhisSmartCard(Boolean mhisSmartCard) { this.mhisSmartCard = mhisSmartCard; }
    public List<HealthItemEntry> getConditions() { return conditions; }
    public void setConditions(List<HealthItemEntry> conditions) { this.conditions = conditions; }
    public String getCancerType() { return cancerType; }
    public void setCancerType(String cancerType) { this.cancerType = cancerType; }
    public List<HealthItemEntry> getSymptoms() { return symptoms; }
    public void setSymptoms(List<HealthItemEntry> symptoms) { this.symptoms = symptoms; }
    public String getFeverDuration() { return feverDuration; }
    public void setFeverDuration(String feverDuration) { this.feverDuration = feverDuration; }
    public List<HealthItemEntry> getOtherIssues() { return otherIssues; }
    public void setOtherIssues(List<HealthItemEntry> otherIssues) { this.otherIssues = otherIssues; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
