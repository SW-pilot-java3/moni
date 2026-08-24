package com.moni.api.domain.instance.stat.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

final class StatMathUtils {

    private StatMathUtils() {
    }

    static <T, F> List<Double> pairwise(List<T> items, Function<T, F> fieldExtractor,
                                         BiFunction<F, F, Double> calculator) {
        List<Double> results = new ArrayList<>();
        for (int i = 1; i < items.size(); i++) {
            F prev = fieldExtractor.apply(items.get(i - 1));
            F curr = fieldExtractor.apply(items.get(i));
            Double value = calculator.apply(prev, curr);
            if (value != null) {
                results.add(value);
            }
        }
        return results;
    }

    static Double avg(List<Double> values) {
        List<Double> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        return nonNull.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    static Double max(List<Double> values) {
        List<Double> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        return nonNull.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    static Double avgLong(List<Long> values) {
        List<Long> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        return nonNull.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    static Double round1(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}