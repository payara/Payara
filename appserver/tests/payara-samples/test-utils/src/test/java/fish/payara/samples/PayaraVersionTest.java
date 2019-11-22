/*
 *  Copyright 2018 Payara Services Ltd
 */
package fish.payara.samples;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.jcip.annotations.NotThreadSafe;

/**
 * Tests the PayaraVersion class' methods.
 * @author Mark Wareham
 * @author Matt Gill
 */
@NotThreadSafe //due to changing of system property used by other tests
public class PayaraVersionTest {

    // Validity tests

    @Test
    public void when_null_string_expect_not_valid_version() {
        assertFalse(new PayaraVersion(null).isValid());
    }

    @Test
    public void when_empty_string_expect_not_valid_version() {
        assertFalse(new PayaraVersion("").isValid());
    }

    @Test
    public void when_starts_with_text_expect_not_valid_version() {
        assertFalse(new PayaraVersion("hello").isValid());
    }

    @Test
    public void when_ends_with_snapshot_expect_valid_version() {
        assertTrue(new PayaraVersion("5.192-SNAPSHOT").isValid());
    }

    // isAtLeast tests

    @Test
    public void when_5_192_expect_is_at_least_5_191() {
        assertTrue(new PayaraVersion("5.192").isAtLeast(new PayaraVersion("5.191")));
    }

    @Test
    public void when_5_192_SNAPSHOT_expect_is_at_least_5_191() {
        assertTrue(new PayaraVersion("5.192-SNAPSHOT").isAtLeast(new PayaraVersion("5.191")));
    }

    @Test
    public void when_5_191_RC1_expect_is_not_at_least_5_191() {
        assertFalse(new PayaraVersion("5.191.RC1").isAtLeast(new PayaraVersion("5.191")));
    }

    @Test
    public void when_5_192_RC1_expect_is_at_least_5_191() {
        assertTrue(new PayaraVersion("5.192.RC1").isAtLeast(new PayaraVersion("5.191")));
    }

    @Test
    public void when_5_191_SNAPSHOT_expect_is_not_at_least_5_191() {
        assertFalse(new PayaraVersion("5.191-SNAPSHOT").isAtLeast(new PayaraVersion("5.191")));
    }

    @Test
    public void when_5_191_expect_is_at_least_5_191() {
        assertTrue(new PayaraVersion("5.191").isAtLeast(new PayaraVersion("5.191")));
    }

    @Test
    public void when_4_191_expect_is_at_least_191() {
        assertTrue(new PayaraVersion("4.1.2.191").isAtLeast(new PayaraVersion("191")));
    }

    @Test
    public void when_4_191_expect_is_not_at_least_192() {
        assertFalse(new PayaraVersion("4.1.2.191").isAtLeast(new PayaraVersion("192")));
    }

    @Test
    public void when_5_191_expect_is_at_least_191() {
        assertTrue(new PayaraVersion("5.191").isAtLeast(new PayaraVersion("191")));
    }

    @Test
    public void when_5_191_expect_is_not_at_least_192() {
        assertFalse(new PayaraVersion("5.191").isAtLeast(new PayaraVersion("192")));
    }

    @Test
    public void when_5_191_SNAPSHOT_expect_is_not_at_least_191() {
        assertFalse(new PayaraVersion("5.191-SNAPSHOT").isAtLeast(new PayaraVersion("191")));
    }

    @Test
    public void when_5_191_RC1_expect_is_not_at_least_191() {
        assertFalse(new PayaraVersion("5.191.RC1").isAtLeast(new PayaraVersion("191")));
    }

    @Test
    public void when_4_191_expect_is_not_at_least_5() {
        assertFalse(new PayaraVersion("4.1.2.191").isAtLeast(new PayaraVersion("5")));
    }

}
