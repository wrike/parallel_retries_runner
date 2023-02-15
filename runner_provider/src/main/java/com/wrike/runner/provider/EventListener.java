package com.wrike.runner.provider;

/**
 * Notifies about events in {@link JUnitPlatformProvider}
 *
 * @author daniil.shylko on 14.12.2022
 */
public interface EventListener {

    default void parallelRetryStarted() {
    }

    default void parallelRetryFinished() {
    }

    default void sequentialRetryStartedDueToModeSpecified() {
    }

    default void sequentialRetryStartedDueToOneRetryLeft() {
    }

    default void sequentialRetryStartedBecauseAllRetriesCantStartAtTheSameTime(int retriesCount, int failedTestsCount, int threadCount) {
    }

    default void sequentialRetryStartedDueToTooManyFailedTests(int failedTestsCount, int failedTestsThreshold) {
    }

}
