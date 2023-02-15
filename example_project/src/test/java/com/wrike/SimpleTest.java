package com.wrike;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Run tests in this class via {@code maven test} to check how parallel retries work
 *
 * @author daniil.shylko on 09.02.2023
 */
@Epic("Test epic")
class SimpleTest {

    private static final AtomicInteger ATTEMPT_COUNTER = new AtomicInteger(1);

    @Test
    void testPassesOnTheThirdAttempt() {
        assertEquals(3, ATTEMPT_COUNTER.getAndIncrement(), "Test passes only on the third attempt");
    }

    @Test
    void testAlwaysPass() {
        //this test always pass
    }

}
