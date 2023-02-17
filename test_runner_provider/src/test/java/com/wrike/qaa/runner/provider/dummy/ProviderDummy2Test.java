package com.wrike.qaa.runner.provider.dummy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author daniil.shylko on 30.11.2022
 */
public class ProviderDummy2Test {

    @Test
    void testAlwaysPass2() {
        //this test always pass
    }

    @Test
    void testAlwaysFail2() {
        fail("This test always fail");
    }

    @Test
    void testAlwaysSkipped2() {
        assumeTrue(false);
    }
}
