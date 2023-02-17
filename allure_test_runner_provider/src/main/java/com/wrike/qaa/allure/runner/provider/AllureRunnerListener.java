package com.wrike.qaa.allure.runner.provider;

import com.wrike.qaa.runner.provider.EventListener;

/**
 * Listener for {@link AllureParallelRetryState} modifying
 *
 * @author daniil.shylko on 14.12.2022
 */
public class AllureRunnerListener implements EventListener {

    private final AllureParallelRetryState allureParallelRetryState;

    public AllureRunnerListener() {
        this(AllureParallelRetryState.getAllureParallelRetryState());
    }

    AllureRunnerListener(AllureParallelRetryState allureParallelRetryState) {
        this.allureParallelRetryState = allureParallelRetryState;
    }

    @Override
    public void parallelRetryStarted() {
        allureParallelRetryState.parallelRetryStarted();
    }

    @Override
    public void parallelRetryNextPackStarted() {
        allureParallelRetryState.parallelRetryNextPackStarted();
    }

    @Override
    public void parallelRetryFinished() {
        allureParallelRetryState.parallelRetryFinished();
    }

}
