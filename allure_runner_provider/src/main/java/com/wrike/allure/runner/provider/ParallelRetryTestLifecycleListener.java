package com.wrike.allure.runner.provider;

import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.TestResult;

import static com.wrike.allure.runner.provider.AllureParallelRetryState.getAllureParallelRetryState;
import static io.qameta.allure.model.Status.PASSED;

/**
 * Allure Report sorts retries by start time.
 * Changing the start time for not passed tests to the time when retries were started
 * will guarantee that test will be marked as passed if it passed at least once.
 *
 * @author daniil.shylko on 22.11.2022
 */
public class ParallelRetryTestLifecycleListener implements TestLifecycleListener {

    private final AllureParallelRetryState allureParallelRetryState;

    public ParallelRetryTestLifecycleListener() {
        this(getAllureParallelRetryState());
    }

    ParallelRetryTestLifecycleListener(AllureParallelRetryState allureParallelRetryState) {
        this.allureParallelRetryState = allureParallelRetryState;
    }

    @Override
    public void beforeTestStop(TestResult result) {
        if (allureParallelRetryState.isParallelRetryInProgress()
                && result.getStatus() != PASSED) {
            result.setStart(allureParallelRetryState.getParallelRetryCurrentPackStartTime());
        }
    }

}
