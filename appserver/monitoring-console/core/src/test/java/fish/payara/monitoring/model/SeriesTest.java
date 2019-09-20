package fish.payara.monitoring.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        assertEquals("SimpleName", series.metric);
        assertFalse(series.isPattern());
    }

    @Test
    public void wildCardName() {
        Series series = new Series("*");
        assertEquals("*", series.toString());
        assertEquals(0, series.tagCount());
        assertEquals("*", series.metric);
        assertTrue(series.isPattern());
    }

    @Test
    public void namespacedName() {
        Series series = new Series("ns:namespace NamespacedName");
        assertEquals("ns:namespace NamespacedName", series.toString());
        assertEquals(1, series.tagCount());
        assertEquals("ns", series.key(0));
        assertEquals("namespace", series.value(0));
        assertEquals("NamespacedName", series.metric);
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
        assertEquals("GroupedName", series.metric);
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
        assertEquals("WildCardName", series.metric);
        assertTrue(series.isPattern());
    }

    @Test
    public void wildCardMatchesAnyTagOrMetricName() {
        assertTrue(new Series("*").matches(new Series("AnyName")));
        assertTrue(new Series("ns:* Foo").matches(new Series("ns:bar Foo")));
        assertTrue(new Series("ns:x @:* Foo").matches(new Series("ns:x @:bar Foo")));
        assertFalse(new Series("@:* Foo").matches(new Series("ns:bar Foo")));
        assertFalse(new Series("ns:* Foo").matches(new Series("ns:x @:bar Foo")));
        assertFalse(new Series("ns:x @:* Foo").matches(new Series("ns:x Foo")));
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
}
