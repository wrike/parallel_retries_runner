package com.wrike.runner.provider.util;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.wrike.runner.provider.util.RunResult.mergeResults;

/**
 * Stores results of runs for tests.
 *
 * @author daniil.shylko on 30.11.2022
 */
public class ExecutionRecorder implements TestExecutionListener {

    /**
     * Stores results of runs
     *
     * <p>
     * If you run tests 2 times sequentially, map will have the next structure:
     * {0=[results for the first run], 1=[results for the second run]}
     * </p>
     * <p>
     * If you run test and then retry each test in parallel twice (initial run count = 3), map will have the next structure:
     * {0=[results for the first run], 1=[some results for the second run], 2=[some results for the second run]}
     * You need to merge results for keys 2 and 3 to get all tests for the parallel retry.
     * Number of entries for parallel retry is equal to number of times each test were run.
     * </p>
     * <p>
     * Keep in mind, that for parallel retry some keys can be skipped.
     * For example, for run count = 4 you can get the next result:
     * {0=[results for the first run], 3=[results for the second run]}
     * In this case, 1 and 2 keys absent and all results were written to the key 3.
     * </p>
     */
    private final Map<Integer, RunResult> runCountToResults = new ConcurrentHashMap<>();
    private final AtomicInteger runCount = new AtomicInteger(-1);

    /**
     * Returns map, where keys are number of run and values are results for sequential runs
     *
     * @return map with the results
     */
    public Map<Integer, RunResult> getResultsForSequentialRuns() {
        return new ConcurrentHashMap<>(runCountToResults);
    }

    /**
     * Returns map, where keys are number of run and values are results for run with parallel retries.
     * Merges the results of parallel retry into one result.
     *
     * @param numbersOfParallelRetryResults numbers of parallel retries which will be merged into one result
     * @return map with the results
     */
    public Map<Integer, RunResult> getResultsForParallelRetry(List<Integer> numbersOfParallelRetryResults) {
        if (numbersOfParallelRetryResults.size() < 2) {
            throw new IllegalArgumentException("At least 2 results should be merged, but you provided " + numbersOfParallelRetryResults.size());
        }
        Map<Integer, RunResult> results = getResultsForSequentialRuns();
        RunResult mergedRunsResults = mergeResults(numbersOfParallelRetryResults.stream()
                .map(results::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        numbersOfParallelRetryResults.forEach(results::remove);
        int minRunNumber = numbersOfParallelRetryResults.stream()
                .min(Integer::compareTo)
                .orElseThrow(() -> new IllegalStateException("Can't find min run number!"));
        results.put(minRunNumber, mergedRunsResults);
        return results;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        runCount.getAndIncrement();
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        boolean isTest = testIdentifier.isTest();
        if (isTest) {
            runCountToResults.compute(runCount.get(), (count, runResult) -> {
                RunResult result = runResult;
                if (runResult == null) {
                    result = new RunResult();
                }
                result.addFinishedResult(testIdentifier, testExecutionResult);
                return result;
            });
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        boolean isTest = testIdentifier.isTest();
        if (isTest) {
            runCountToResults.compute(runCount.get(), (count, runResult) -> {
                RunResult result = runResult;
                if (runResult == null) {
                    result = new RunResult();
                }
                result.addSkippedResult(testIdentifier, reason);
                return result;
            });
        }
    }
}
