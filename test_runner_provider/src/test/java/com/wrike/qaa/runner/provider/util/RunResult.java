package com.wrike.qaa.runner.provider.util;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents results of one tests' run attempt
 *
 * @author daniil.shylko on 30.11.2022
 */
public class RunResult {

    private final Map<TestIdentifier, List<TestExecutionResult>> finishedTests = new ConcurrentHashMap<>();
    private final Map<TestIdentifier, List<String>> skippedTests = new ConcurrentHashMap<>();

    public Map<TestIdentifier, List<TestExecutionResult>> getFinishedTests() {
        return finishedTests;
    }

    public Map<TestIdentifier, List<String>> getSkippedTests() {
        return skippedTests;
    }

    public void addFinishedResult(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        addResult(finishedTests, testIdentifier, testExecutionResult);
    }

    public void addSkippedResult(TestIdentifier testIdentifier, String reason) {
        addResult(skippedTests, testIdentifier, reason);
    }

    private <T> void addResult(Map<TestIdentifier, List<T>> container, TestIdentifier testIdentifier, T result) {
        container.compute(testIdentifier, (name, results) -> {
            List<T> resultList = results;
            if (resultList == null) {
                resultList = new ArrayList<>();
            }
            resultList.add(result);
            return resultList;
        });
    }

    public static RunResult mergeResults(List<RunResult> results) {
        RunResult mergedResult = new RunResult();
        Map<TestIdentifier, List<TestExecutionResult>> finishedTests = mergeResults(results.stream()
                .map(RunResult::getFinishedTests));
        Map<TestIdentifier, List<String>> skippedTests = mergeResults(results.stream()
                .map(RunResult::getSkippedTests));
        mergedResult.finishedTests.putAll(finishedTests);
        mergedResult.skippedTests.putAll(skippedTests);
        return mergedResult;
    }

    private static <T> Map<TestIdentifier, List<T>> mergeResults(Stream<Map<TestIdentifier, List<T>>> results) {
        return results
                .flatMap(runResults -> runResults.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (values1, values2) -> {
                    values1.addAll(values2);
                    return values1;
                }));
    }

}
