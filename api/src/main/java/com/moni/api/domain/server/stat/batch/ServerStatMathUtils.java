package com.moni.api.domain.server.stat.batch;

import java.util.List;
import java.util.Objects;

public final class ServerStatMathUtils {

    private ServerStatMathUtils() {
    }

    public static Double round1(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 10.0) / 10.0;
    }

    public static Double round3(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static Long computeDelta(Long prev, Long curr) {
        if (prev == null && curr == null) {
            return 0L;
        }
        if (prev == null) {
            return curr;
        }
        if (curr == null) {
            return 0L;
        }
        if (curr >= prev) {
            return curr - prev;
        }
        return curr;
    }

    public static Double computeDeltaDouble(Double prev, Double curr) {
        if (prev == null && curr == null) return 0.0;
        if (prev == null) return curr;
        if (curr == null) return 0.0;
        if (curr >= prev) return curr - prev;
        return curr;
    }

    public static Double avg(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        return nonNull.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static Double avgLong(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<Long> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }

        return nonNull.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public static Double avgInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<Integer> nonNull = values.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return null;
        }
        
        return nonNull.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    public static Double max(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().filter(Objects::nonNull).max(Double::compareTo).orElse(null);
    }

    public static Long maxLong(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().filter(Objects::nonNull).max(Long::compareTo).orElse(null);
    }

    public static Integer maxInt(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().filter(Objects::nonNull).max(Integer::compareTo).orElse(null);
    }
}
