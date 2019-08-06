package fish.payara.monitoring.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An immutable structured representation of a data series key parsed into its {@link #tags} and {@link #metric} name.
 * 
 * @author Jan Bernitt
 */
public final class Series implements Comparable<Series>, Serializable {

    private final String metric;
    private final String[] tags;
    private final String[] values;

    public Series(String key) {
        String[] parts = key.split("[ ,;]+");
        this.tags = new String[parts.length - 1];
        this.values = new String[tags.length];
        this.metric = parts[parts.length - 1].intern();
        for (int i = 0; i < parts.length - 1; i++) {
            int eqIndex = parts[i].indexOf('=');
            tags[i] = parts[i].substring(0, eqIndex).intern();
            values[i] = parts[i].substring(eqIndex + 1).intern();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Series && compareTo((Series) obj) == 0;
    }

    @Override
    public int hashCode() {
        // not including the tags is "good enough" to avoid to many collisions for this domain
        return metric.hashCode() ^ Arrays.hashCode(values);
    }

    @Override
    public int compareTo(Series other) {
        int res = metric.compareTo(other.metric);
        if (res != 0) {
            return res;
        }
        res = Integer.compare(tags.length, other.tags.length);
        if (res != 0) {
            return res;
        }
        for (int i = 0; i < tags.length; i++) {
            int tagIndex = other.indexOf(tags[i]);
            if (tagIndex < 0) {
                return -1;
            }
            res = values[i].compareTo(other.values[tagIndex]);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }

    private int indexOf(String tag) {
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals(tag)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < tags.length; i++) {
            str.append(tags[i]).append('=').append(values[i]).append(' ');
        }
        str.append(metric);
        return str.toString();
    }
}
