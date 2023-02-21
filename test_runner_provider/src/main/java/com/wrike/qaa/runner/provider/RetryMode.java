package com.wrike.qaa.runner.provider;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author daniil.shylko on 21.11.2022
 */
enum RetryMode {

    SEQUENTIAL("sequential"),
    PARALLEL("parallel");

    RetryMode(String retryModeString) {
        this.retryModeString = retryModeString;
    }

    private final String retryModeString;

    public String getRetryModeString() {
        return retryModeString;
    }

    private static final Map<String, RetryMode> retryModeNamesToEnumValues = Arrays.stream(RetryMode.values())
            .collect(Collectors.toMap(RetryMode::getRetryModeString, Function.identity()));

    public static Optional<RetryMode> getRetryModeByName(String retryModeName) {
        return Optional.ofNullable(retryModeNamesToEnumValues.get(retryModeName));
    }

    public static Set<String> getAvailableRetryModes() {
        return retryModeNamesToEnumValues.keySet();
    }

}
