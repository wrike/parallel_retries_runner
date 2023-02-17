package com.wrike.qaa.runner.provider;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.collect.Lists;
import com.wrike.qaa.runner.provider.util.ServiceLoaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.SurefireReflectionException;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.shared.lang3.StringUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.*;
import static org.apache.maven.surefire.api.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.RunMode.RERUN_TEST_AFTER_FAILURE;
import static org.apache.maven.surefire.api.testset.TestListResolver.optionallyWildcardFilter;
import static org.apache.maven.surefire.api.util.TestsToRun.fromClass;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isBlank;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * It's modified org.apache.maven.surefire.junitplatform.JUnitPlatformProvider from maven-surefire-plugin.
 *
 * @since 2.22.0
 */
public class JUnitPlatformProvider extends AbstractProvider {

    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(JUnitPlatformProvider.class);

    static final String CONFIGURATION_PARAMETERS = "configurationParameters";

    private final ProviderParameters parameters;

    private final Launcher launcher;

    private final Filter<?>[] filters;

    private final Map<String, String> configurationParameters;

    private final EnvironmentProvider environmentProvider;

    private final List<EventListener> eventListeners;

    public JUnitPlatformProvider(ProviderParameters parameters) {
        this(
                parameters,
                new LazyLauncher(),
                new EnvironmentProvider(parameters.getTestRequest().getRerunFailingTestsCount()),
                ServiceLoaderUtil.load(EventListener.class)
        );
    }

    JUnitPlatformProvider(
            ProviderParameters parameters,
            Launcher launcher,
            EnvironmentProvider environmentProvider,
            List<EventListener> eventListeners
    ) {
        this.parameters = parameters;
        this.launcher = launcher;
        filters = newFilters();
        configurationParameters = newConfigurationParameters();
        this.environmentProvider = environmentProvider;
        this.eventListeners = eventListeners;
    }

    @Override
    public Iterable<Class<?>> getSuites() {
        try {
            return scanClasspath();
        } finally {
            closeLauncher();
        }
    }

    @Override
    public RunResult invoke(Object forkTestSet)
            throws TestSetFailedException, ReporterException {
        ReporterFactory reporterFactory = parameters.getReporterFactory();
        final RunResult runResult;
        try {
            RunListenerAdapter adapter = new RunListenerAdapter(reporterFactory.createTestReportListener());
            adapter.setRunMode(NORMAL_RUN);
            startCapture(adapter);
            setupJunitLogger();
            if (forkTestSet instanceof TestsToRun) {
                invokeAllTests((TestsToRun) forkTestSet, adapter, reporterFactory);
            } else if (forkTestSet instanceof Class) {
                invokeAllTests(fromClass((Class<?>) forkTestSet), adapter, reporterFactory);
            } else if (forkTestSet == null) {
                invokeAllTests(scanClasspath(), adapter, reporterFactory);
            } else {
                throw new IllegalArgumentException(
                        "Unexpected value of forkTestSet: " + forkTestSet);
            }
        } finally {
            runResult = reporterFactory.close();
        }
        return runResult;
    }

    private static void setupJunitLogger() {
        Logger logger = Logger.getLogger("org.junit");
        if (logger.getLevel() == null) {
            logger.setLevel(WARNING);
        }
    }

    private TestsToRun scanClasspath() {
        TestPlanScannerFilter filter = new TestPlanScannerFilter(launcher, filters);
        ScanResult scanResult = parameters.getScanResult();
        TestsToRun scannedClasses = scanResult.applyFilter(filter, parameters.getTestClassLoader());
        return parameters.getRunOrderCalculator().orderTestClasses(scannedClasses);
    }

    private void invokeAllTests(TestsToRun testsToRun, RunListenerAdapter adapter, ReporterFactory reporterFactory) {
        try {
            execute(testsToRun, adapter);
        } finally {
            closeLauncher();
        }

        retryTests(adapter, reporterFactory);
    }

    /**
     * It retries tests until they passed or {@link EnvironmentProvider#getRerunFailingTestsCount()} attempts are completed.
     *
     * <p>
     * Tests will be retried in parallel if all the following criteria are fulfilled:
     *  <ol>
     *      <li>{@link EnvironmentProvider#getRetryMode()} == {@link RetryMode#PARALLEL}</li>
     *      <li>More than 1 retry left</li>
     *      <li>The count of failed tests in this run is less or equal to {@link EnvironmentProvider#getFailedTestsThresholdForParallelRetry()}</li>
     *      <li>All retry attempts of one test can be started sentimentally (retriesLeft * failedTestsCount <= {@link EnvironmentProvider#getThreadCount()})</li>
     *  </ol>
     * Otherwise, we can try to retry each test once sequentially and then try to retry the remaining attempts in parallel.
     *</p>
     */
    private void retryTests(RunListenerAdapter adapter, ReporterFactory reporterFactory) {
        int retriesLeft = environmentProvider.getRerunFailingTestsCount();
        try {
            while (retriesLeft > 0 && adapter.hasFailingTests()) {
                if (RetryMode.SEQUENTIAL.equals(environmentProvider.getRetryMode())) {
                    notifySafely(EventListener::sequentialRetryStartedDueToModeSpecified);
                    adapter = retryTestsWithRerunAfterFailureAndGetResults(adapter, reporterFactory);
                } else if (retriesLeft < 2) {
                    notifySafely(EventListener::sequentialRetryStartedDueToOneRetryLeft);
                    adapter = retryTestsWithRerunAfterFailureAndGetResults(adapter, reporterFactory);
                } else if (adapter.getFailures().size() > environmentProvider.getFailedTestsThresholdForParallelRetry()) {
                    int failedTestsCount = adapter.getFailures().size();
                    notifySafely(eventListener -> eventListener.sequentialRetryStartedDueToTooManyFailedTests(failedTestsCount, environmentProvider.getFailedTestsThresholdForParallelRetry()));
                    adapter = retryTestsWithRerunAfterFailureAndGetResults(adapter, reporterFactory);
                } else if (allParallelRetriesCantStartInTheSameTime(retriesLeft, adapter)) {
                    int failedTestsCount = adapter.getFailures().size();
                    int finalRetriesLeft = retriesLeft;
                    notifySafely(eventListener -> eventListener.sequentialRetryStartedBecauseAllRetriesCantStartAtTheSameTime(finalRetriesLeft, failedTestsCount, environmentProvider.getThreadCount()));
                    adapter = retryTestsWithRerunAfterFailureAndGetResults(adapter, reporterFactory);
                } else {
                    //tests will always run in one group due to the previous `if condition`
                    retryTestsWithAllAttemptsInParallel(retriesLeft, adapter, reporterFactory);
                    break;
                }
                retriesLeft--;
            }
        } finally {
            closeLauncher();
        }
    }

    private void notifySafely(Consumer<EventListener> eventToNotify) {
        eventListeners.forEach(eventListener -> {
            try {
                eventToNotify.accept(eventListener);
            } catch (Exception e) {
                LOG.error("Can't invoke listener", e);
            }
        });
    }

    private boolean allParallelRetriesCantStartInTheSameTime(int retriesLeft, RunListenerAdapter adapter) {
        return retriesLeft * adapter.getFailures().size() > environmentProvider.getThreadCount();
    }

    /**
     * Executes each test {@code retriesCount} times.
     * All attempts of one test will be triggered in parallel.
     */
    private void retryTestsWithAllAttemptsInParallel(int retriesCount, RunListenerAdapter adapter, ReporterFactory reporterFactory) {
        notifySafely(EventListener::parallelRetryStarted);
        List<LauncherDiscoveryRequest> discoveryRequests = buildLauncherDiscoveryRequestForRerunFailuresInParallel(adapter, retriesCount);
        discoveryRequests.forEach(retryDiscoveryRequest -> {
            notifySafely(EventListener::parallelRetryNextPackStarted);
            List<Thread> threads = new ArrayList<>();
            List<RunListenerAdapter> listeners = IntStream.range(0, retriesCount)
                    .mapToObj(i -> createRetryRunListenerAdapter(reporterFactory))
                    .collect(toList());
            listeners.forEach(listener -> threads.add(new Thread(() -> execute(retryDiscoveryRequest, listener))));
            threads.forEach(Thread::start);
            threads.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Test execution was interrupted!", e);
                }
            });
        });
        notifySafely(EventListener::parallelRetryFinished);
    }

    private RunListenerAdapter retryTestsWithRerunAfterFailureAndGetResults(RunListenerAdapter adapter, ReporterFactory reporterFactory) {
        LauncherDiscoveryRequest discoveryRequest = buildLauncherDiscoveryRequestForRerunFailures(adapter);
        RunListenerAdapter runListenerAdapter = createRetryRunListenerAdapter(reporterFactory);
        execute(discoveryRequest, runListenerAdapter);
        return runListenerAdapter;
    }

    private RunListenerAdapter createRetryRunListenerAdapter(ReporterFactory reporterFactory) {
        RunListenerAdapter runListenerAdapter = new RunListenerAdapter(reporterFactory.createTestReportListener());
        runListenerAdapter.setRunMode(RERUN_TEST_AFTER_FAILURE);
        return runListenerAdapter;
    }

    /**
     * Splits tests into groups in such a way that all invocations of one group {@code retriesCount} times
     * in parallel will fit into {@link EnvironmentProvider#getThreadCount()} threads.
     */
    private List<LauncherDiscoveryRequest> buildLauncherDiscoveryRequestForRerunFailuresInParallel(RunListenerAdapter adapter, int retriesCount) {
        int groupSize = environmentProvider.getThreadCount() / retriesCount;
        if (groupSize < 1) {
            throw new IllegalStateException(String.format("Thread count (%d) should be more or equals than retry count (%d) for parallel retries mode!", environmentProvider.getThreadCount(), retriesCount));
        }
        List<List<TestIdentifier>> groupedValues = Lists.partition(List.copyOf(adapter.getFailures().keySet()), groupSize);
        return groupedValues
                .stream()
                .map(selectors -> {
                    LauncherDiscoveryRequestBuilder builder = request()
                            .filters(filters)
                            .configurationParameters(configurationParameters);
                    builder.selectors(selectors.stream().map(identifier -> selectUniqueId(identifier.getUniqueId()))
                            .collect(Collectors.toList()));
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private void execute(LauncherDiscoveryRequest discoveryRequest, RunListenerAdapter runListenerAdapter) {
        launcher.execute(discoveryRequest, runListenerAdapter);
    }

    private void execute(TestsToRun testsToRun, RunListenerAdapter adapter) {
        if (testsToRun.allowEagerReading()) {
            List<DiscoverySelector> selectors = new ArrayList<>();
            testsToRun.iterator()
                    .forEachRemaining(c -> selectors.add(selectClass(c.getName())));

            LauncherDiscoveryRequestBuilder builder = request()
                    .filters(filters)
                    .configurationParameters(configurationParameters)
                    .selectors(selectors);

            launcher.execute(builder.build(), adapter);
        } else {
            testsToRun.iterator()
                    .forEachRemaining(c ->
                    {
                        LauncherDiscoveryRequestBuilder builder = request()
                                .filters(filters)
                                .configurationParameters(configurationParameters)
                                .selectors(selectClass(c.getName()));
                        launcher.execute(builder.build(), adapter);
                    });
        }
    }

    private void closeLauncher() {
        if (launcher instanceof AutoCloseable) {
            try {
                ((AutoCloseable) launcher).close();
            } catch (Exception e) {
                throw new SurefireReflectionException(e);
            }
        }
    }

    private LauncherDiscoveryRequest buildLauncherDiscoveryRequestForRerunFailures(RunListenerAdapter adapter) {
        LauncherDiscoveryRequestBuilder builder = request().filters(filters).configurationParameters(
                configurationParameters);
        // Iterate over recorded failures
        for (TestIdentifier identifier : new LinkedHashSet<>(adapter.getFailures().keySet())) {
            builder.selectors(selectUniqueId(identifier.getUniqueId()));
        }
        return builder.build();
    }

    private Filter<?>[] newFilters() {
        List<Filter<?>> filters = new ArrayList<>();

        getPropertiesList(TESTNG_GROUPS_PROP)
                .map(TagFilter::includeTags)
                .ifPresent(filters::add);

        getPropertiesList(TESTNG_EXCLUDEDGROUPS_PROP)
                .map(TagFilter::excludeTags)
                .ifPresent(filters::add);

        of(optionallyWildcardFilter(parameters.getTestRequest().getTestListResolver()))
                .filter(f -> !f.isEmpty())
                .filter(f -> !f.isWildcard())
                .map(TestMethodFilter::new)
                .ifPresent(filters::add);

        getPropertiesList(INCLUDE_JUNIT5_ENGINES_PROP)
                .map(EngineFilter::includeEngines)
                .ifPresent(filters::add);

        getPropertiesList(EXCLUDE_JUNIT5_ENGINES_PROP)
                .map(EngineFilter::excludeEngines)
                .ifPresent(filters::add);

        return filters.toArray(new Filter<?>[filters.size()]);
    }

    Filter<?>[] getFilters() {
        return filters;
    }

    private Map<String, String> newConfigurationParameters() {
        String content = parameters.getProviderProperties().get(CONFIGURATION_PARAMETERS);
        if (content == null) {
            return emptyMap();
        }
        try (StringReader reader = new StringReader(content)) {
            Map<String, String> result = new HashMap<>();
            Properties props = new Properties();
            props.load(reader);
            props.stringPropertyNames()
                    .forEach(key -> result.put(key, props.getProperty(key)));
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading " + CONFIGURATION_PARAMETERS, e);
        }
    }

    Map<String, String> getConfigurationParameters() {
        return configurationParameters;
    }

    private Optional<List<String>> getPropertiesList(String key) {
        String property = parameters.getProviderProperties().get(key);
        return isBlank(property) ? empty()
                : of(stream(property.split("[,]+"))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(toList()));
    }
}
