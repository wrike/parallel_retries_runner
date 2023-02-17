package com.wrike.qaa.allure.runner.provider;

/**
 * Singleton, which represents the current state of parallel retry.
 *
 * @author daniil.shylko on 09.02.2023
 */
class AllureParallelRetryState {

    private static final AllureParallelRetryState INSTANCE = new AllureParallelRetryState();

    private volatile boolean isParallelRetryInProgress = false;
    /**
     * Current pack is a set of tests, which is retrying in parallel now
     */
    private volatile long parallelRetryCurrentPackStartTime = 0L;

    AllureParallelRetryState() {
    }

    public static AllureParallelRetryState getAllureParallelRetryState() {
        return INSTANCE;
    }

    public void parallelRetryStarted() {
        isParallelRetryInProgress = true;
    }

    public void parallelRetryNextPackStarted() {
        parallelRetryCurrentPackStartTime = getTimeRightBeforeTheCurrentPack();
    }

    public void parallelRetryFinished() {
        isParallelRetryInProgress = false;
        parallelRetryCurrentPackStartTime = 0L;
    }

    public boolean isParallelRetryInProgress() {
        return isParallelRetryInProgress;
    }

    public long getParallelRetryCurrentPackStartTime() {
        return parallelRetryCurrentPackStartTime;
    }

    /**
     * Subtracting one ensures that a failed test will have a start time earlier than a passed test
     */
    private static long getTimeRightBeforeTheCurrentPack() {
        return System.currentTimeMillis() - 1L;
    }

}
