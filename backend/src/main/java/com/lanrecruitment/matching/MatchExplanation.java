package com.lanrecruitment.matching;

import java.util.*;
import lombok.Data;

@Data
public class MatchExplanation {
    private Long jobId;
    private Double matchScore;
    private Double recommendationScore;
    private String algorithmVersion = ContentScorer.VERSION;
    private String strategy;
    private Map<String,Double> components = new LinkedHashMap<>();
    private Map<String,Double> effectiveWeights = new LinkedHashMap<>();
    private Map<String,Double> dimensions = new LinkedHashMap<>();
    private Map<String,Double> dimensionWeights = new LinkedHashMap<>();
    private List<String> matchedSkills = new ArrayList<>();
    private List<Map<String,Object>> missingSkills = new ArrayList<>();
    private List<String> missingProfileFields = new ArrayList<>();
    private List<String> reasons = new ArrayList<>();
    private String fallbackReason;
    @com.fasterxml.jackson.annotation.JsonIgnore private Double rawContent;
    @com.fasterxml.jackson.annotation.JsonIgnore private double rawRecommendation;
}
