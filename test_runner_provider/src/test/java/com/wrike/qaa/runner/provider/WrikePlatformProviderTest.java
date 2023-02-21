package com.wrike.qaa.runner.provider;

import com.wrike.qaa.runner.provider.dummy.ProviderDummy2Test;
import com.wrike.qaa.runner.provider.dummy.ProviderDummyTest;
import com.wrike.qaa.runner.provider.dummy.ProviderRetryDummyTest;
import com.wrike.qaa.runner.provider.util.ExecutionRecorder;
import com.wrike.qaa.runner.provider.util.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.wrike.qaa.runner.provider.ProviderMockUtils.*;
import static com.wrike.qaa.runner.provider.RetryMode.PARALLEL;
import static com.wrike.qaa.runner.provider.RetryMode.SEQUENTIAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.platform.engine.TestExecutionResult.Status.*;
import static org.mockito.Mockito.times;

/**
 * @author daniil.shylko on 29.11.2022
 */
class WrikePlatformProviderTest {

    @Test
    void checkAllGivenTestsToRunAreInvoked() throws TestSetFailedException {
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);

        ExecutionRecorder executionRecorder = runAllTestsInClasses(
                List.of(),
                environmentProvider,
                ProviderDummyTest.class, ProviderDummy2Test.class
        );

        Map<Integer, RunResult> results = executionRecorder.getResultsForSequentialRuns();
        assertThat(results)
                .as("Check test were ran only once")
                .hasSize(1);

        checkRunResultsStatuses(
                results,
                0,
                List.of(
                        new TestResults("testAlwaysPass1()", List.of(SUCCESSFUL)),
                        new TestResults("testAlwaysPass2()", List.of(SUCCESSFUL)),
                        new TestResults("testAlwaysFail1()", List.of(FAILED)),
                        new TestResults("testAlwaysPass1_2()", List.of(SUCCESSFUL)),
                        new TestResults("testAlwaysFail2()", List.of(FAILED)),
                        new TestResults("testAlwaysSkipped2()", List.of(ABORTED))
                )
        );

        Map<TestIdentifier, List<String>> skippedTests = results.get(0).getSkippedTests();
        assertThat(skippedTests)
                .as("Check skipped tests")
                .extractingFromEntries(e -> e.getKey().getDisplayName(), Map.Entry::getValue)
                .containsExactlyInAnyOrder(Tuple.tuple("testDisabled()", List.of("void com.wrike.qaa.runner.provider.dummy.ProviderDummyTest.testDisabled() is @Disabled")));
    }

    @Test
    void checkTestsAreRetriedInSequentialModeByDefault() throws TestSetFailedException {
        EventListener eventListener = Mockito.mock(EventListener.class);
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);
        Mockito.when(environmentProvider.getRetryMode()).thenReturn(SEQUENTIAL);
        Mockito.when(environmentProvider.getThreadCount()).thenReturn(20);
        Mockito.when(environmentProvider.getRerunFailingTestsCount()).thenReturn(2);
        Mockito.when(environmentProvider.getFailedTestsThresholdForParallelRetry()).thenReturn(10);

        ExecutionRecorder executionRecorder = runAllTestsInClasses(
                List.of(eventListener),
                environmentProvider,
                ProviderRetryDummyTest.class
        );

        Mockito.verify(eventListener, times(2)).sequentialRetryStartedDueToModeSpecified();
        Mockito.verifyNoMoreInteractions(eventListener);

        Map<Integer, RunResult> results = executionRecorder.getResultsForSequentialRuns();
        checkListsOfExecutedTests(results, List.of(
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()",
                        "testAlwaysPass()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testAlwaysFail()"
                )
        ));
    }

    @Test
    void checkTestsAreRetriedInSequentialModeIfOnlyOneRetryLeft() throws TestSetFailedException {
        EventListener eventListener = Mockito.mock(EventListener.class);
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);
        Mockito.when(environmentProvider.getRetryMode()).thenReturn(PARALLEL);
        Mockito.when(environmentProvider.getThreadCount()).thenReturn(20);
        Mockito.when(environmentProvider.getRerunFailingTestsCount()).thenReturn(1);
        Mockito.when(environmentProvider.getFailedTestsThresholdForParallelRetry()).thenReturn(10);

        ExecutionRecorder executionRecorder = runAllTestsInClasses(
                List.of(eventListener),
                environmentProvider,
                ProviderRetryDummyTest.class
        );

        Mockito.verify(eventListener).sequentialRetryStartedDueToOneRetryLeft();
        Mockito.verifyNoMoreInteractions(eventListener);

        Map<Integer, RunResult> results = executionRecorder.getResultsForSequentialRuns();
        checkListsOfExecutedTests(results, List.of(
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()",
                        "testAlwaysPass()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()"
                )
        ));
    }

    @Test
    void checkTestsAreRetriedInSequentialModeIfAllRetriesCantStartAtTheSameTime() throws TestSetFailedException {
        EventListener eventListener = Mockito.mock(EventListener.class);
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);
        Mockito.when(environmentProvider.getRetryMode()).thenReturn(PARALLEL);
        Mockito.when(environmentProvider.getThreadCount()).thenReturn(7);
        Mockito.when(environmentProvider.getRerunFailingTestsCount()).thenReturn(2);
        Mockito.when(environmentProvider.getFailedTestsThresholdForParallelRetry()).thenReturn(10);

        ExecutionRecorder executionRecorder = runAllTestsInClasses(
                List.of(eventListener),
                environmentProvider,
                ProviderRetryDummyTest.class
        );

        Mockito.verify(eventListener).sequentialRetryStartedBecauseAllRetriesCantStartAtTheSameTime(2, 4, 7);
        Mockito.verify(eventListener).sequentialRetryStartedDueToOneRetryLeft();
        Mockito.verifyNoMoreInteractions(eventListener);

        Map<Integer, RunResult> results = executionRecorder.getResultsForSequentialRuns();
        checkListsOfExecutedTests(results, List.of(
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()",
                        "testAlwaysPass()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testAlwaysFail()"
                )
        ));
    }

    @Test
    void checkTestsAreRetriedInSequentialModeIfTooManyTestsFailed() throws TestSetFailedException {
        EventListener eventListener = Mockito.mock(EventListener.class);
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);
        Mockito.when(environmentProvider.getRetryMode()).thenReturn(PARALLEL);
        Mockito.when(environmentProvider.getThreadCount()).thenReturn(7);
        Mockito.when(environmentProvider.getRerunFailingTestsCount()).thenReturn(2);
        Mockito.when(environmentProvider.getFailedTestsThresholdForParallelRetry()).thenReturn(3);
        ExecutionRecorder executionRecorder = runAllTestsInClasses(
                List.of(eventListener),
                environmentProvider,
                ProviderRetryDummyTest.class
        );

        Mockito.verify(eventListener).sequentialRetryStartedDueToTooManyFailedTests(4, 3);
        Mockito.verify(eventListener).sequentialRetryStartedDueToOneRetryLeft();
        Mockito.verifyNoMoreInteractions(eventListener);

        Map<Integer, RunResult> results = executionRecorder.getResultsForSequentialRuns();
        checkListsOfExecutedTests(results, List.of(
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()",
                        "testAlwaysPass()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testAlwaysFail()"
                )
        ));
    }

    @Test
    void checkTestsAreRetriedInParallelModeIfAllCriteriaAreFulfilled() throws TestSetFailedException {
        EventListener eventListener = Mockito.mock(EventListener.class);
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);
        Mockito.when(environmentProvider.getRetryMode()).thenReturn(PARALLEL);
        Mockito.when(environmentProvider.getThreadCount()).thenReturn(8);
        Mockito.when(environmentProvider.getRerunFailingTestsCount()).thenReturn(2);
        Mockito.when(environmentProvider.getFailedTestsThresholdForParallelRetry()).thenReturn(10);

        ExecutionRecorder executionRecorder = runAllTestsInClasses(
                List.of(eventListener),
                environmentProvider,
                ProviderRetryDummyTest.class
        );

        Mockito.verify(eventListener).parallelRetryStarted();
        Mockito.verify(eventListener).parallelRetryNextPackStarted();
        Mockito.verify(eventListener).parallelRetryFinished();
        Mockito.verifyNoMoreInteractions(eventListener);

        Map<Integer, RunResult> results = executionRecorder.getResultsForParallelRetry(List.of(1, 2));
        checkListsOfExecutedTests(results, List.of(
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()",
                        "testAlwaysPass()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()"
                )
        ));

        checkRunResultsStatuses(
                results,
                1,
                List.of(
                        new TestResults("testPassesOnTheThirdAttempt()", List.of(FAILED, SUCCESSFUL)),
                        new TestResults("testPassesOnTheThirdAttempt2()", List.of(FAILED, SUCCESSFUL)),
                        new TestResults("testPassesOnTheSecondAttempt()", List.of(FAILED, SUCCESSFUL)),
                        new TestResults("testAlwaysFail()", List.of(FAILED, FAILED))
                )
        );
    }

    @Test
    void checkParallelRetriesStartsAfterSuccessfulSequentialRetry() throws TestSetFailedException {
        EventListener eventListener = Mockito.mock(EventListener.class);
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);
        Mockito.when(environmentProvider.getRetryMode()).thenReturn(PARALLEL);
        Mockito.when(environmentProvider.getThreadCount()).thenReturn(5);
        Mockito.when(environmentProvider.getRerunFailingTestsCount()).thenReturn(5);
        Mockito.when(environmentProvider.getFailedTestsThresholdForParallelRetry()).thenReturn(10);

        ExecutionRecorder executionRecorder = runAllTestsInClasses(
                List.of(eventListener),
                environmentProvider,
                ProviderRetryDummyTest.class
        );

        Mockito.verify(eventListener).sequentialRetryStartedBecauseAllRetriesCantStartAtTheSameTime(5, 4, 5);
        Mockito.verify(eventListener).sequentialRetryStartedBecauseAllRetriesCantStartAtTheSameTime(4, 3, 5);
        Mockito.verify(eventListener).parallelRetryStarted();
        Mockito.verify(eventListener).parallelRetryNextPackStarted();
        Mockito.verify(eventListener).parallelRetryFinished();
        Mockito.verifyNoMoreInteractions(eventListener);

        Map<Integer, RunResult> results = executionRecorder.getResultsForParallelRetry(List.of(3, 4, 5));
        checkListsOfExecutedTests(results, List.of(
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()",
                        "testAlwaysPass()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testPassesOnTheSecondAttempt()",
                        "testAlwaysFail()"
                ),
                List.of(
                        "testPassesOnTheThirdAttempt()",
                        "testPassesOnTheThirdAttempt2()",
                        "testAlwaysFail()"
                ),
                List.of(
                        "testAlwaysFail()"
                )
        ));

        checkRunResultsStatuses(
                results,
                3,
                List.of(new TestResults("testAlwaysFail()", List.of(FAILED, FAILED, FAILED)))
        );
    }

    @Test
    void checkBrokenEventListenerDoesNotFailExecution() {
        EventListener brokenEventListener = getBrokenEventListener();
        EnvironmentProvider environmentProvider = Mockito.mock(EnvironmentProvider.class);
        Mockito.when(environmentProvider.getRetryMode()).thenReturn(PARALLEL);
        Mockito.when(environmentProvider.getThreadCount()).thenReturn(5);
        Mockito.when(environmentProvider.getRerunFailingTestsCount()).thenReturn(5);
        Mockito.when(environmentProvider.getFailedTestsThresholdForParallelRetry()).thenReturn(10);

        assertThatCode(() -> runAllTestsInClasses(
                List.of(brokenEventListener),
                environmentProvider,
                ProviderRetryDummyTest.class
        )).doesNotThrowAnyException();
    }

    private EventListener getBrokenEventListener() {
        return (EventListener) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{EventListener.class}, (proxy, method, instance) -> {
            IllegalStateException expectedException = new IllegalStateException("It's expected exception");
            expectedException.setStackTrace(new StackTraceElement[]{});
            throw expectedException;
        });
    }

    private void checkRunResultsStatuses(Map<Integer, RunResult> results, int runNumber, List<TestResults> expectedTestResults) {
        Map<TestIdentifier, List<TestExecutionResult>> retriedTests = results.get(runNumber).getFinishedTests();
        List<TestResults> actualTestResults = retriedTests.entrySet().stream()
                .map(retriedTestEntry -> new TestResults(
                        retriedTestEntry.getKey().getDisplayName(),
                        retriedTestEntry.getValue().stream()
                                .map(TestExecutionResult::getStatus)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
        assertThat(expectedTestResults)
                .as("Check test results statuses")
                .containsExactlyInAnyOrderElementsOf(actualTestResults);
    }

    private void checkListsOfExecutedTests(Map<Integer, RunResult> results, List<List<String>> executedTestsRuns) {
        assertThat(results)
                .as("Check test were executed %d times", executedTestsRuns.size())
                .hasSize(executedTestsRuns.size());

        IntStream.range(0, executedTestsRuns.size())
                .forEach(testRunNumber -> {
                    Map<TestIdentifier, List<TestExecutionResult>> runResults = results.get(testRunNumber).getFinishedTests();
                    assertThat(runResults)
                            .as("Check %d run results", testRunNumber)
                            .extractingFromEntries(e -> e.getKey().getDisplayName())
                            .containsExactlyInAnyOrderElementsOf(executedTestsRuns.get(testRunNumber));
                });
    }

    private ExecutionRecorder runAllTestsInClasses(
            List<EventListener> eventListeners,
            EnvironmentProvider environmentProvider,
            Class<?>... testClasses
    ) throws TestSetFailedException {
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider(
                providerParametersMock(),
                launcher,
                environmentProvider,
                eventListeners
        );

        ExecutionRecorder executionRecorder = new ExecutionRecorder();
        launcher.registerTestExecutionListeners(executionRecorder);

        TestsToRun testsToRun = newTestsToRun(testClasses);
        invokeProvider(provider, testsToRun);
        return executionRecorder;
    }

}
