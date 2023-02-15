package com.wrike.runner.provider.dummy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author daniil.shylko on 30.11.2022
 */
public class ProviderDummyTest {

    @Test
    void testAlwaysPass1() {
        //this test always pass
    }

    @Test
    void testAlwaysPass1_2() {
        //this test always pass
    }

    @Disabled
    @Test
    void testDisabled() {
        //this test is disabled
    }

    @Test
    void testAlwaysFail1() {
        fail("This test always fail");
    }
}
