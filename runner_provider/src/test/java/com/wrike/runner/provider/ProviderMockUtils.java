package com.wrike.runner.provider;

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

import com.wrike.runner.provider.dummy.ProviderRetryDummyTest;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.RunOrderCalculator;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * It's a modified part of refactored JUnitPlatformProviderTest from the surefire plugin
 *
 * @author daniil.shylko on 30.11.2022
 */
class ProviderMockUtils {

    /**
     * Providers are invoked one by one due to overriding System.in and System.out,
     * and {@link ProviderRetryDummyTest#resetCounts()}
     *
     * @param provider instance of {@link JUnitPlatformProvider}
     * @param testsToRun test classes to run
     * @throws TestSetFailedException if invocation failed
     */
    static synchronized void invokeProvider(JUnitPlatformProvider provider, TestsToRun testsToRun)
            throws TestSetFailedException {
        ProviderRetryDummyTest.resetCounts();
        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;
        try {
            provider.invoke(testsToRun);
        } finally {
            System.setOut(systemOut);
            System.setErr(systemErr);
        }
    }

    @SuppressWarnings("unchecked")
    static TestReportListener<TestOutputReportEntry> runListenerMock() {
        return mock(TestReportListener.class, withSettings().extraInterfaces(TestOutputReceiver.class));
    }

    static ProviderParameters providerParametersMock(Class<?>... testClasses) {
        return providerParametersMock(runListenerMock(), testClasses);
    }

    static ProviderParameters providerParametersMock(
            TestReportListener<TestOutputReportEntry> runListener, Class<?>... testClasses) {
        TestListResolver testListResolver = new TestListResolver("");
        return providerParametersMock(runListener, testListResolver, testClasses);
    }

    static ProviderParameters providerParametersMock(TestReportListener<TestOutputReportEntry> runListener,
                                                             TestListResolver testListResolver,
                                                             Class<?>... testClasses) {
        TestsToRun testsToRun = newTestsToRun(testClasses);

        ScanResult scanResult = mock(ScanResult.class);
        when(scanResult.applyFilter(any(), any())).thenReturn(testsToRun);

        RunOrderCalculator runOrderCalculator = mock(RunOrderCalculator.class);
        when(runOrderCalculator.orderTestClasses(any())).thenReturn(testsToRun);

        ReporterFactory reporterFactory = mock(ReporterFactory.class);
        when(reporterFactory.createTestReportListener()).thenReturn(runListener);

        TestRequest testRequest = mock(TestRequest.class);
        when(testRequest.getTestListResolver()).thenReturn(testListResolver);

        ProviderParameters providerParameters = mock(ProviderParameters.class);
        when(providerParameters.getScanResult()).thenReturn(scanResult);
        when(providerParameters.getRunOrderCalculator()).thenReturn(runOrderCalculator);
        when(providerParameters.getReporterFactory()).thenReturn(reporterFactory);
        when(providerParameters.getTestRequest()).thenReturn(testRequest);

        return providerParameters;
    }

    static TestsToRun newTestsToRun(Class<?>... testClasses) {
        List<Class<?>> classesList = Arrays.asList(testClasses);
        return new TestsToRun(new LinkedHashSet<>(classesList));
    }

}
