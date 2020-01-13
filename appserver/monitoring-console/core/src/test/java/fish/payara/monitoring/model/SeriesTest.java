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
        assertMatchesPattern("*", "AnyName");
        assertNotMatchesPattern("*", "ns:bar Foo");
        assertNotMatchesPattern("*", "ns:bar @:Bar Foo");
        assertMatchesPattern("ns:* Foo", "ns:bar Foo");
        assertMatchesPattern("ns:x @:* Foo", "ns:x @:bar Foo");
        assertNotMatchesPattern("ns:x @:* Foo", "ns:y @:bar Foo");
        assertNotMatchesPattern("ns:x @:* Foo", "ns:x y:bar Foo");
        assertNotMatchesPattern("ns:x @:* Foo", "ns:x @:bar x:y Foo");
        assertNotMatchesPattern("ns:x @:* Foo", "ns:x Foo");
        assertNotMatchesPattern("@:* Foo", "ns:bar Foo");
        assertNotMatchesPattern("ns:* Foo", "ns:x @:bar Foo");

        assertMatchesPattern("?:bar Foo", "Foo");
        assertMatchesPattern("?:bar Foo", "ns:bar Foo");
        assertMatchesPattern("?:* Foo", "ns:bar Foo");
        assertMatchesPattern("?:* Foo", "foo:bar Foo");
        assertMatchesPattern("?:* Foo", "ns:x foo:bar Foo");
        assertMatchesPattern("?:* Foo", "Foo");
        assertMatchesPattern("?:* *", "Foo");
        assertMatchesPattern("ns:bar ?:* Foo", "ns:bar Foo");
        assertMatchesPattern("ns:bar ?:* Foo", "ns:bar x:y Foo");
        assertMatchesPattern("ns:bar ?:x Foo", "ns:bar Foo");
        assertMatchesPattern("ns:bar ?:y Foo", "ns:bar x:y Foo");
        assertNotMatchesPattern("?:bar Foo", "ns:x Foo");
        assertNotMatchesPattern("?:* Foo", "ns:bar Bar");
        assertNotMatchesPattern("ns:bar ?:* Foo", "ns:x foo:bar Foo");
    }

    private static void assertMatchesPattern(String pattern, String series) {
        Series p = new Series(pattern);
        assertTrue(p.isPattern());
        assertTrue(p.matches(new Series(series)));
    }

    private static void assertNotMatchesPattern(String pattern, String series) {
        Series p = new Series(pattern);
        assertTrue(p.isPattern());
        assertFalse(p.matches(new Series(series)));
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
