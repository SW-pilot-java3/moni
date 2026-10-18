package com.moni.api.domain.report.daily.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 5분 통계(stat_*) 12개 행을 1시간 버킷으로 다시 묶을 때 쓰는 집계 규칙.
 * avg 컬럼은 "평균의 평균", max/min 컬럼은 "최댓값/최솟값 중 대표값", sum 컬럼만 그대로 합산한다.
 * 단순 합산을 쓰면 avg/max 컬럼이 원 단위를 벗어난 값(예: CPU 사용률 700%)이 되므로 반드시 구분해야 한다.
 */
final class HourlyStatAggregator {

    private HourlyStatAggregator() {
    }

    static Double avgOfAvg(List<Double> values) {
        List<Double> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return present.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    static Long avgOfAvgLong(List<Long> values) {
        List<Long> present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return null;
        }
        return Math.round(present.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    static Double maxOfMax(List<Double> values) {
        return values.stream().filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }

    static Long minOfMin(List<Long> values) {
        return values.stream().filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
    }

    static Integer sum(List<Integer> values) {
        return values.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }
}
