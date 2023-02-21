package com.wrike.qaa.runner.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.wrike.qaa.runner.provider.RetryMode.getRetryModeByName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author daniil.shylko on 02.12.2022
 */
class RetryModeTest {

    @ParameterizedTest(name = "{displayName} for {0}")
    @EnumSource(RetryMode.class)
    @DisplayName("Check retry mode is parsed correctly")
    void checkRetryModeIsParsedCorrectly(RetryMode retryMode) {
        assertThat(getRetryModeByName(retryMode.getRetryModeString()))
                .as("Check retry mode %s is parsed from string", retryMode)
                .contains(retryMode);
    }

    @Test
    void checkRetryModeForWrongString() {
        assertThat(getRetryModeByName("wrong value"))
                .as("Check retry mode is sequential by default")
                .isEmpty();
    }

}
