package com.wrike.qaa.allure.runner.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author daniil.shylko on 09.02.2023
 */
class AllureParallelRetryStateTest {

    @Test
    void checkDefaultState() {
        AllureParallelRetryState allureParallelRetryState = new AllureParallelRetryState();

        assertThat(allureParallelRetryState)
                .as("Check parallel retry is off by default")
                .extracting(AllureParallelRetryState::isParallelRetryInProgress)
                .isEqualTo(false);
        assertThat(allureParallelRetryState)
                .as("Check parallel retry current pack start time is not set")
                .extracting(AllureParallelRetryState::getParallelRetryCurrentPackStartTime)
                .isEqualTo(0L);
    }

    @Test
    void checkStateForStartedParallelRetry() {
        AllureParallelRetryState allureParallelRetryState = new AllureParallelRetryState();

        allureParallelRetryState.parallelRetryStarted();

        assertThat(allureParallelRetryState)
                .as("Check parallel retry is on")
                .extracting(AllureParallelRetryState::isParallelRetryInProgress)
                .isEqualTo(true);
    }

    @Test
    void checkStateForStartedNextPackParallelRetry() {
        AllureParallelRetryState allureParallelRetryState = new AllureParallelRetryState();

        allureParallelRetryState.parallelRetryNextPackStarted();

        assertThat(allureParallelRetryState)
                .as("Check parallel retry current pack start time is set")
                .extracting(AllureParallelRetryState::getParallelRetryCurrentPackStartTime)
                .isNotEqualTo(0L);
    }

    @Test
    void checkStateForFinishedParallelRetry() {
        AllureParallelRetryState allureParallelRetryState = new AllureParallelRetryState();

        allureParallelRetryState.parallelRetryStarted();
        allureParallelRetryState.parallelRetryFinished();

        assertThat(allureParallelRetryState)
                .as("Check parallel retry is off")
                .extracting(AllureParallelRetryState::isParallelRetryInProgress)
                .isEqualTo(false);
        assertThat(allureParallelRetryState)
                .as("Check parallel retry current pack start time is not set")
                .extracting(AllureParallelRetryState::getParallelRetryCurrentPackStartTime)
                .isEqualTo(0L);
    }

}
