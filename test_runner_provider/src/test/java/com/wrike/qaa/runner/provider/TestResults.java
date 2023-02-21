package com.wrike.qaa.runner.provider;

import org.junit.platform.engine.TestExecutionResult.Status;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Test results for simple assertions checks in tests
 *
 * @author daniil.shylko on 10.02.2023
 */
class TestResults {

    private final String displayName;
    private final Map<Status, Long> statusCounts;

    public TestResults(String displayName, List<Status> results) {
        this.displayName = displayName;
        this.statusCounts = results.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestResults that = (TestResults) o;
        return Objects.equals(displayName, that.displayName)
                && Objects.equals(statusCounts, that.statusCounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, statusCounts);
    }
}
