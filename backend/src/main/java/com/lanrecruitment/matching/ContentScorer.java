package com.lanrecruitment.matching;

import java.util.*;

/** Pure M2 scoring functions. Inputs must contain only canonical SKILL tag IDs. */
public final class ContentScorer {
    private ContentScorer() {}
    public static final String VERSION = "content-hot-v1";
    public static final double[] DEFAULT_WEIGHTS = {0.50, 0.20, 0.15, 0.15};

    public static final class Requirement {
        public final int level, importance;
        public Requirement(int level, int importance) {
            if (level < 1 || level > 3 || importance < 1 || importance > 3) throw new IllegalArgumentException("skill values must be 1..3");
            this.level = level; this.importance = importance;
        }
    }
    public static Double skill(Map<Long, Integer> student, Map<Long, Requirement> job) {
        if (student.isEmpty() || job.isEmpty()) return null;
        double dot=0, pNorm=0, qNorm=0, covered=0, importance=0;
        for (Integer p:student.values()) {
            if (p == null || p < 1 || p > 3) throw new IllegalArgumentException("student skill must be 1..3");
            pNorm += p*p;
        }
        for (Map.Entry<Long, Requirement> entry:job.entrySet()) {
            Requirement q=entry.getValue(); int p=student.getOrDefault(entry.getKey(),0);
            dot += p*q.level; qNorm += q.level*q.level;
            covered += q.importance*Math.min((double)p/q.level,1); importance += q.importance;
        }
        double cosine=pNorm==0 || qNorm==0 ? 0 : dot/(Math.sqrt(pNorm)*Math.sqrt(qNorm));
        return Math.min(1,Math.max(0,0.5*cosine+0.5*covered/importance));
    }
    public static Double content(Double skill, Double category, Double city, Double salary) {
        Double[] values={skill,category,city,salary}; double sum=0, weight=0;
        for (int i=0;i<values.length;i++) if (values[i]!=null) {
            if (!Double.isFinite(values[i]) || values[i]<0 || values[i]>1) throw new IllegalArgumentException("dimension must be 0..1");
            sum+=values[i]*DEFAULT_WEIGHTS[i]; weight+=DEFAULT_WEIGHTS[i];
        }
        return weight==0 ? null : sum/weight;
    }
    public static Double salary(Integer expected, Integer maximum, Boolean negotiable) {
        if (expected==null || expected<=0 || maximum==null || Boolean.TRUE.equals(negotiable)) return null;
        return Math.min(1.0, Math.max(0.0, (double) maximum/expected));
    }
    public static double hot(long distinctApplicants, long maxApplicants, double publishedDays) {
        if (distinctApplicants<0 || maxApplicants<distinctApplicants) throw new IllegalArgumentException("invalid counts");
        double pop=maxApplicants==0 ? 0 : Math.log1p(5.0*distinctApplicants)/Math.log1p(5.0*maxApplicants);
        return 0.8*pop+0.2*Math.exp(-Math.max(0,publishedDays)/30.0);
    }
    public static double recommendation(Double content,double hot) {
        return content==null ? hot : 0.8*content+0.2*hot;
    }
}
