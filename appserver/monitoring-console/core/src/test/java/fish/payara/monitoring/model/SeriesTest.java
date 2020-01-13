package fish.payara.monitoring.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests the basic correctness of the {@link Series} parsing and access methods.
 *
 * @author Jan Bernitt
 */
public class SeriesTest {

    @Test
    public void simpleName() {
        Series series = new Series("SimpleName");
        assertEquals("SimpleName", series.toString());
        assertEquals(0, series.tagCount());
        assertEquals("SimpleName", series.getMetric());
        assertFalse(series.isPattern());
    }

    @Test
    public void wildCardName() {
        Series series = new Series("*");
        assertEquals("*", series.toString());
        assertEquals(0, series.tagCount());
        assertEquals("*", series.getMetric());
        assertTrue(series.isPattern());
    }

    @Test
    public void namespacedName() {
        Series series = new Series("ns:namespace NamespacedName");
        assertEquals("ns:namespace NamespacedName", series.toString());
        assertEquals(1, series.tagCount());
        assertEquals("ns", series.key(0));
        assertEquals("namespace", series.value(0));
        assertEquals("NamespacedName", series.getMetric());
        assertFalse(series.isPattern());
    }

    @Test
    public void namespacedAndGroupedName() {
        Series series = new Series("ns:namespace @:group GroupedName");
        assertEquals("ns:namespace @:group GroupedName", series.toString());
        assertEquals(2, series.tagCount());
        assertEquals("ns", series.key(0));
        assertEquals("namespace", series.value(0));
        assertEquals("@", series.key(1));
        assertEquals("group", series.value(1));
        assertEquals("GroupedName", series.getMetric());
        assertFalse(series.isPattern());
    }

    @Test
    public void namespacedAndGroupedWithWildCardName() {
        Series series = new Series("ns:namespace @:* WildCardName");
        assertEquals("ns:namespace @:* WildCardName", series.toString());
        assertEquals(2, series.tagCount());
        assertEquals("ns", series.key(0));
        assertEquals("namespace", series.value(0));
        assertEquals("@", series.key(1));
        assertEquals("*", series.value(1));
        assertEquals("WildCardName", series.getMetric());
        assertTrue(series.isPattern());
    }

    @Test
    public void wildCardMatchesAnyTagOrMetricName() {
        assertTrue(new Series("*").matches(new Series("AnyName")));
        assertFalse(new Series("*").matches(new Series("ns:bar Foo")));
        assertFalse(new Series("*").matches(new Series("ns:bar @:Bar Foo")));
        assertTrue(new Series("ns:* Foo").matches(new Series("ns:bar Foo")));
        assertTrue(new Series("ns:x @:* Foo").matches(new Series("ns:x @:bar Foo")));
        assertFalse(new Series("@:* Foo").matches(new Series("ns:bar Foo")));
        assertFalse(new Series("ns:* Foo").matches(new Series("ns:x @:bar Foo")));
        assertFalse(new Series("ns:x @:* Foo").matches(new Series("ns:x Foo")));

        assertTrue(new Series("?:bar Foo").matches(new Series("ns:bar Foo")));
        assertTrue(new Series("?:* Foo").matches(new Series("ns:bar Foo")));
        assertTrue(new Series("?:* Foo").matches(new Series("foo:bar Foo")));
        assertTrue(new Series("?:* Foo").matches(new Series("ns:x foo:bar Foo")));
        assertTrue(new Series("?:* Foo").matches(new Series("Foo")));
        assertTrue(new Series("?:* *").matches(new Series("Foo")));
        assertTrue(new Series("ns:bar ?:* Foo").matches(new Series("ns:bar Foo")));
        assertTrue(new Series("ns:bar ?:* Foo").matches(new Series("ns:bar x:y Foo")));
        assertFalse(new Series("?:bar Foo").matches(new Series("Foo")));
        assertFalse(new Series("?:bar Foo").matches(new Series("ns:x Foo")));
        assertFalse(new Series("?:* Foo").matches(new Series("ns:bar Bar")));
        assertFalse(new Series("ns:bar ?:* Foo").matches(new Series("ns:x foo:bar Foo")));
    }

    @Test
    public void tagSeperatorsAreSpecialCharacters() {
        assertTrue(Series.isSpecialTagCharacter(' '));
        assertTrue(Series.isSpecialTagCharacter(','));
        assertTrue(Series.isSpecialTagCharacter(';'));
    }

    @Test
    public void tagAssignmentIsSpecialCharacter() {
        assertTrue(Series.isSpecialTagCharacter(':'));
    }

    @Test
    public void wildCardIsSpecialCharacter() {
        assertTrue(Series.isSpecialTagCharacter('*'));
    }

    @Test
    public void questionMarkIsSpecialCharacter() {
        assertTrue(Series.isSpecialTagCharacter('?'));
    }

    @Test
    public void illegalFormat() {
        try {
            assertNotNull(new Series("ns: system. cpu.load"));
        } catch (IllegalArgumentException e) {
            assertEquals("Malformed series key, `:` missing or misplaced in ns: system. cpu.load", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullArgument() {
        assertNotNull(new Series(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyArgument() {
        assertNotNull(new Series(""));
    }
}
