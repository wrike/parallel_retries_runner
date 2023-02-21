package com.wrike.qaa.runner.provider.dummy;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author daniil.shylko on 30.11.2022
 */
public class ProviderRetryDummyTest {

    private static AtomicInteger ATTEMPT_COUNTER = new AtomicInteger(1);
    private static AtomicInteger ATTEMPT_COUNTER1 = new AtomicInteger(1);
    private static AtomicInteger ATTEMPT_COUNTER2 = new AtomicInteger(1);

    /**
     * Call this method before each launcher invoking
     */
    public static void resetCounts() {
        ATTEMPT_COUNTER = new AtomicInteger(1);
        ATTEMPT_COUNTER1 = new AtomicInteger(1);
        ATTEMPT_COUNTER2 = new AtomicInteger(1);
    }

    @Test
    void testPassesOnTheThirdAttempt() {
        assertEquals(3, ATTEMPT_COUNTER.getAndIncrement(), "Test passes only on the third attempt");
    }

    @Test
    void testPassesOnTheThirdAttempt2() {
        assertEquals(3, ATTEMPT_COUNTER1.getAndIncrement(), "Test passes only on the third attempt");
    }

    @Test
    void testPassesOnTheSecondAttempt() {
        assertEquals(2, ATTEMPT_COUNTER2.getAndIncrement(), "Test passes only on the second attempt");
    }

    @Test
    void testAlwaysFail() {
        fail("This test always fail");
    }

    @Test
    void testAlwaysPass() {
        //this test always pass
    }
}
