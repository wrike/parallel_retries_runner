package com.wrike.allure.runner.provider;

import io.qameta.allure.model.TestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author daniil.shylko on 09.02.2023
 */
@ExtendWith(MockitoExtension.class)
class ParallelRetryTestLifecycleListenerTest {

    private static final long DEFAULT_START_TIME = -1L;

    @Mock
    private AllureParallelRetryState allureParallelRetryState;

    private TestResult testResult;

    @BeforeEach
    void prepare() {
        testResult = new TestResult().setStart(DEFAULT_START_TIME);
    }

    @Test
    void checkStartTimeWillNotBeChangedIfParallelRetryIsOff() {
        Mockito.when(allureParallelRetryState.isParallelRetryInProgress()).thenReturn(false);
        ParallelRetryTestLifecycleListener listener = new ParallelRetryTestLifecycleListener(allureParallelRetryState);

        listener.beforeTestStop(testResult);

        assertThat(testResult)
                .as("Check start time of test result was not changed")
                .extracting(TestResult::getStart)
                .isEqualTo(DEFAULT_START_TIME);
    }

    @Test
    void checkStartTimeWillBeChangedIfParallelRetryIsOnAndTestNotPassed() {
        testResult.setStatus(FAILED);
        Mockito.when(allureParallelRetryState.isParallelRetryInProgress()).thenReturn(true);
        ParallelRetryTestLifecycleListener listener = new ParallelRetryTestLifecycleListener(allureParallelRetryState);

        listener.beforeTestStop(testResult);

        assertThat(testResult)
                .as("Check start time of test result was changed")
                .extracting(TestResult::getStart)
                .isNotEqualTo(DEFAULT_START_TIME);
    }

    @Test
    void checkStartTimeWillNotBeChangedIfParallelRetryIsOnAndTestPassed() {
        testResult.setStatus(PASSED);
        Mockito.when(allureParallelRetryState.isParallelRetryInProgress()).thenReturn(true);
        ParallelRetryTestLifecycleListener listener = new ParallelRetryTestLifecycleListener(allureParallelRetryState);

        listener.beforeTestStop(testResult);

        assertThat(testResult)
                .as("Check start time of test result was not changed")
                .extracting(TestResult::getStart)
                .isEqualTo(DEFAULT_START_TIME);
    }

}
