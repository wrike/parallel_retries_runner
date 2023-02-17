package com.wrike.qaa.allure.runner.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author daniil.shylko on 09.02.2023
 */
@ExtendWith(MockitoExtension.class)
class AllureRunnerListenerTest {

    @Mock
    private AllureParallelRetryState allureParallelRetryState;

    @Test
    void checkRunnerListenerChangesStateOnParallelRetryStart() {
        AllureRunnerListener allureRunnerListener = new AllureRunnerListener(allureParallelRetryState);

        allureRunnerListener.parallelRetryStarted();

        Mockito.verify(allureParallelRetryState).parallelRetryStarted();
        Mockito.verifyNoMoreInteractions(allureParallelRetryState);
    }

    @Test
    void checkRunnerListenerChangesStateOnParallelRetryFinished() {
        AllureRunnerListener allureRunnerListener = new AllureRunnerListener(allureParallelRetryState);

        allureRunnerListener.parallelRetryStarted();
        Mockito.clearInvocations(allureParallelRetryState);
        allureRunnerListener.parallelRetryFinished();

        Mockito.verify(allureParallelRetryState).parallelRetryFinished();
        Mockito.verifyNoMoreInteractions(allureParallelRetryState);
    }

}
