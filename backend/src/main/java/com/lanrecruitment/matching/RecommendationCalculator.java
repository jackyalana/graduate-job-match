package com.lanrecruitment.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** No persistence, authentication, wall clock or random values. */
public final class RecommendationCalculator {
    private RecommendationCalculator() {}
    private static Double round(Double value,int places) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(places,RoundingMode.HALF_UP).doubleValue();
    }
    public static MatchExplanation calculate(long jobId, Map<Long,Integer> studentSkills,
            Map<Long,ContentScorer.Requirement> requiredSkills, Map<Long,String> names,
            Long desiredCategory, Long jobCategory, Set<String> desiredCities, String jobCity,
            Integer desiredSalary, Integer maxSalary, Boolean negotiable, double hot) {
        return calculate(jobId,studentSkills,requiredSkills,names,desiredCategory,jobCategory,desiredCities,jobCity,desiredSalary,maxSalary,negotiable,hot,null);
    }
    public static MatchExplanation calculate(long jobId, Map<Long,Integer> studentSkills,
            Map<Long,ContentScorer.Requirement> requiredSkills, Map<Long,String> names,
            Long desiredCategory, Long jobCategory, Set<String> desiredCities, String jobCity,
            Integer desiredSalary, Integer maxSalary, Boolean negotiable, double hot, Double cf) {
        MatchExplanation out=new MatchExplanation(); out.setJobId(jobId);
        Double skill=ContentScorer.skill(studentSkills,requiredSkills);
        Double category=desiredCategory==null || jobCategory==null ? null : desiredCategory.equals(jobCategory)?1.0:0.0;
        Double city=desiredCities.isEmpty() || jobCity==null || jobCity.isEmpty() ? null : desiredCities.contains(jobCity)?1.0:0.0;
        Double salary=ContentScorer.salary(desiredSalary,maxSalary,negotiable);
        Double[] dimensions={skill,category,city,salary}; String[] keys={"skill","category","city","salary"};
        double available=0;
        for(int i=0;i<keys.length;i++) if(dimensions[i]!=null) available+=ContentScorer.DEFAULT_WEIGHTS[i];
        for(int i=0;i<keys.length;i++) {
            out.getDimensions().put(keys[i],round(dimensions[i],4));
            out.getDimensionWeights().put(keys[i], dimensions[i]==null ? 0.0 : round(ContentScorer.DEFAULT_WEIGHTS[i]/available,4));
        }
        if(studentSkills.isEmpty()) out.getMissingProfileFields().add("skills");
        if(desiredCategory==null) out.getMissingProfileFields().add("expectedCategoryId");
        if(desiredCities.isEmpty()) out.getMissingProfileFields().add("cityCodes");
        if(desiredSalary==null || desiredSalary<=0) out.getMissingProfileFields().add("salaryMin");
        for(Map.Entry<Long,ContentScorer.Requirement> e:requiredSkills.entrySet()) {
            int p=studentSkills.getOrDefault(e.getKey(),0), q=e.getValue().level;
            String name=names.getOrDefault(e.getKey(),String.valueOf(e.getKey()));
            if(p>=q) out.getMatchedSkills().add(name);
            else { Map<String,Object> item=new LinkedHashMap<>(); item.put("name",name);item.put("actual",p);item.put("required",q);out.getMissingSkills().add(item); }
        }
        if(Double.valueOf(1).equals(category)) out.getReasons().add("岗位类别符合求职意向");
        if(Double.valueOf(1).equals(city)) out.getReasons().add("工作城市符合偏好");
        if(Double.valueOf(1).equals(salary)) out.getReasons().add("最高月薪满足期望");
        if(!out.getMatchedSkills().isEmpty()) out.getReasons().add("满足部分岗位技能要求");
        Double content=ContentScorer.content(skill,category,city,salary);
        double wc=content==null?0:(cf==null?0.8:0.6),wf=cf==null?0:(content==null?0.625:0.25),wh=(content==null?(cf==null?1.0:0.375):(cf==null?0.2:0.15));
        double recommendation=wc*(content==null?0:content)+wf*(cf==null?0:cf)+wh*hot;
        out.setRawContent(content); out.setRawRecommendation(recommendation);
        out.setMatchScore(round(content==null?null:100*content,1));
        out.setRecommendationScore(round(100*recommendation,1));
        out.setStrategy(cf!=null?"HYBRID":content==null?"HOT_FALLBACK":"CONTENT_HOT");
        out.setFallbackReason(cf!=null?null:content==null?"NO_AVAILABLE_CONTENT":"NO_VALID_NEIGHBORS");
        out.getComponents().put("content",round(content,4));out.getComponents().put("collaborative",round(cf,4));out.getComponents().put("hot",round(hot,4));
        out.getEffectiveWeights().put("content",wc);out.getEffectiveWeights().put("collaborative",wf);out.getEffectiveWeights().put("hot",wh);
        return out;
    }
}
