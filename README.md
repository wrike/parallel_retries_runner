## Description

It's an implementation of parallel retries for Java + Maven + JUnit 5 stack.

WARNING: Parallel retries mode ignores JUnit 5 synchronisation mechanisms,
which are set up via @Execution, @ResourceLock and @Isolated annotations.

This project contains three following modules:

- **test_runner_provider** — provides modified JUnitPlatformProvider from [maven-surefire-plugin](https://github.com/apache/maven-surefire/),
which allows you to retry tests in parallel.
- **allure_test_runner_provider** — provides Allure integration for JUnitPlatformProvider.
- **example_project** — the project with set up JUnitPlatformProvider and Allure integration.

## How to set up parallel retries

To add support of parallel retries, you need to specify **test_runner_provider** dependency in the dependencies
of **maven-surefire-plugin**.

If you use Allure, add **allure_test_runner_provider** dependency. It fixes the order of parallel retries in the Allure report.
By default, Allure sorts results by start time. Changing the start time for not passed tests to the time when retries
were started will guarantee that test will be marked as passed if it passed at least once.

To set up retries you can specify the following configuration parameters:

| Parameter Name               | Default Value | Description      |
|------------------------------|---------------|------------------|
| ```rerunFailingTestsCount``` | 0             | Count of retries |

And next properties:

| Property Name                                                   | Default Value     | Description                                                                                  |
|-----------------------------------------------------------------|-------------------|----------------------------------------------------------------------------------------------|
| ```junit.jupiter.execution.parallel.config.fixed.parallelism``` | 1                 | Count of threads. JUnit5 uses the same value.                                                |
| ```retry.mode```                                                | sequential        | Set to 'parallel' to retry tests in parallel. Otherwise, tests will be retried sequentially. |
| ```retry.parallel.failed.tests.threshold```                     | Integer.MAX_VALUE | If failed test count is more than the threshold, tests will be retried sequentially.         |

Tests will be retried in parallel if all the following criteria are fulfilled:
- ```retry.mode=parallel```
- More than 1 retry left
- The count of failed tests in this run is less or equal to ```retry.parallel.failed.tests.threshold```
- All retry attempts of one test can be started sentimentally (```retriesLeft * failedTestsCount <= junit.jupiter.execution.parallel.config.fixed.parallelism```)
Otherwise, we can try to retry each test once sequentially and then try to retry the remaining attempts in parallel.

Here you can see an example of configuration:
```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0-M7</version>
            <dependencies>
                <dependency>
                    <groupId>com.wrike</groupId>
                    <artifactId>test_runner_provider</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </dependency>
                <!-- add this dependency if you use allure -->
                <dependency>
                    <groupId>com.wrike</groupId>
                    <artifactId>allure_test_runner_provider</artifactId>
                    <version>1.0-SNAPSHOT</version>
                </dependency>
            </dependencies>
            <configuration>
                <!-- https://issues.apache.org/jira/browse/SUREFIRE-1614 -->
                <forkNode implementation="org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory"/>
                <rerunFailingTestsCount>3</rerunFailingTestsCount>
                <systemPropertyVariables>
                    <junit.jupiter.execution.parallel.enabled>true</junit.jupiter.execution.parallel.enabled>
                    <junit.jupiter.execution.parallel.mode.default>concurrent</junit.jupiter.execution.parallel.mode.default>
                    <junit.jupiter.execution.parallel.mode.classes.default>concurrent</junit.jupiter.execution.parallel.mode.classes.default>
                    <junit.jupiter.execution.parallel.config.strategy>fixed</junit.jupiter.execution.parallel.config.strategy>
                    <junit.jupiter.execution.parallel.config.fixed.parallelism>10</junit.jupiter.execution.parallel.config.fixed.parallelism>
                    <!-- you can specify this property if you use allure -->
                    <allure.results.directory>target/allure-results</allure.results.directory>
                    <retry.mode>parallel</retry.mode>
                    <retry.parallel.failed.tests.threshold>10</retry.parallel.failed.tests.threshold>
                </systemPropertyVariables>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## How to build this project

To build the project, use the following command:
```shell
mvn clean install
```

To run tests in the **example_project** module, use the following command:
```shell
mvn clean test -pl example_project
```

To build and open the Allure report for tests from example_project
you can use one of the Allure report plugins for your CI or run the following Allure CLI command:
```shell
allure generate example_project/target/allure-results && allure open
```
