package com.wrike.qaa.runner.provider;

import com.google.common.base.Suppliers;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides external properties. It's used to mock the environment in tests.
 *
 * @author daniil.shylko on 30.11.2022
 */
class EnvironmentProvider {

    /**
     * If it's not specified, tests will be retried sequentially
     */
    private static final String THREAD_COUNT_PROPERTY = "junit.jupiter.execution.parallel.config.fixed.parallelism";
    private static final String RETRY_MODE_PROPERTY = "retry.mode";
    /**
     * If there are more failed tests than a threshold, tests will be retried sequentially
     */
    private static final String FAILED_TESTS_THRESHOLD_PROPERTY = "retry.parallel.failed.tests.threshold";

    private static final int DEFAULT_FAILED_TESTS_THRESHOLD = Integer.MAX_VALUE;
    private static final RetryMode DEFAULT_RETRY_MODE = RetryMode.SEQUENTIAL;
    private static final int DEFAULT_THREAD_COUNT = 1;

    private final int rerunFailingTestsCount;

    public EnvironmentProvider(int rerunFailingTestsCount) {
        this.rerunFailingTestsCount = rerunFailingTestsCount;
    }

    public int getRerunFailingTestsCount() {
        return rerunFailingTestsCount;
    }

    public int getThreadCount() {
        return threadCountSupplier.get();
    }

    public RetryMode getRetryMode() {
        return retryModeSupplier.get();
    }

    public int getFailedTestsThresholdForParallelRetry() {
        return failedTestsThresholdForParallelRetrySupplier.get();
    }

    private static final Supplier<Integer> failedTestsThresholdForParallelRetrySupplier = Suppliers.memoize(() ->
            getIntegerPropertyOrDefault(FAILED_TESTS_THRESHOLD_PROPERTY, DEFAULT_FAILED_TESTS_THRESHOLD));

    private static final Supplier<Integer> threadCountSupplier = Suppliers.memoize(() ->
            getIntegerPropertyOrDefault(THREAD_COUNT_PROPERTY, DEFAULT_THREAD_COUNT));

    private static final Supplier<RetryMode> retryModeSupplier = Suppliers.memoize(() -> {
        String retryMode = System.getProperty(RETRY_MODE_PROPERTY);
        if (retryMode == null) {
            return DEFAULT_RETRY_MODE;
        }
        return RetryMode.getRetryModeByName(retryMode)
                .orElseThrow(() -> new IllegalStateException(String.format("Can not parse %s=%s to one of the following values %s",
                        RETRY_MODE_PROPERTY, retryMode, RetryMode.getAvailableRetryModes())));
    });

    private static int getIntegerPropertyOrDefault(String propertyName, int defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        return Optional.ofNullable(propertyValue)
                .map(Integer::parseInt)
                .orElse(defaultValue);
    }

}
