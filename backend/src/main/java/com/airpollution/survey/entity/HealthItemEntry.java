package com.airpollution.survey.entity;

import java.util.List;

public class HealthItemEntry {
    private String key;
    private String description;
    private Boolean present;
    private Boolean visitedHospital;
    private String hospitalNames;
    private Boolean ipd;
    private Boolean opd;
    private Boolean missedSchoolOrWork;
    private Integer daysMissed;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getPresent() { return present; }
    public void setPresent(Boolean present) { this.present = present; }
    public Boolean getVisitedHospital() { return visitedHospital; }
    public void setVisitedHospital(Boolean visitedHospital) { this.visitedHospital = visitedHospital; }
    public String getHospitalNames() { return hospitalNames; }
    public void setHospitalNames(String hospitalNames) { this.hospitalNames = hospitalNames; }
    public Boolean getIpd() { return ipd; }
    public void setIpd(Boolean ipd) { this.ipd = ipd; }
    public Boolean getOpd() { return opd; }
    public void setOpd(Boolean opd) { this.opd = opd; }
    public Boolean getMissedSchoolOrWork() { return missedSchoolOrWork; }
    public void setMissedSchoolOrWork(Boolean missedSchoolOrWork) { this.missedSchoolOrWork = missedSchoolOrWork; }
    public Integer getDaysMissed() { return daysMissed; }
    public void setDaysMissed(Integer daysMissed) { this.daysMissed = daysMissed; }

    public static HealthItemEntry findByKey(List<HealthItemEntry> items, String key) {
        if (items == null) return null;
        return items.stream().filter(entry -> key.equals(entry.getKey())).findFirst().orElse(null);
    }

    public static boolean isPresent(List<HealthItemEntry> items, String key) {
        HealthItemEntry entry = findByKey(items, key);
        return entry != null && Boolean.TRUE.equals(entry.getPresent());
    }
}
