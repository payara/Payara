/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.monitoring.model;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

/**
 * An {@link SeriesAnnotation} is meta data linked to a {@link SeriesDataset} by having the same {@link Series} and
 * {@link #instance} and pointing to a {@link #time} that is in the range of {@link SeriesDataset}.
 * 
 * The meta data attached is a list of key-value pairs.
 * 
 * @author Jan Bernitt
 * @since 5.201
 */
public final class SeriesAnnotation implements Serializable, Iterable<Entry<String, String>> {

    private final long time;
    private final Series series;
    private final String instance;
    private final long value;
    private final boolean keyed;
    private final String[] attrs;

    public SeriesAnnotation(long time, Series series, String instance, long value, boolean keyed, String[] attrs) {
        this.time = time;
        this.series = series;
        this.instance = instance;
        this.value = value;
        this.keyed = keyed && attrs.length >= 2;
        this.attrs = attrs;
        if (attrs.length % 2 == 1) {
            throw new IllegalArgumentException(
                    "Annotation attributes always must be given in pairs but got: " + Arrays.toString(attrs));
        }
    }

    public long getTime() {
        return time;
    }

    public long getValue() {
        return value;
    }

    public Series getSeries() {
        return series;
    }

    public String getInstance() {
        return instance;
    }

    public boolean isKeyed() {
        return keyed;
    }

    /**
     * @return By convention the first attribute is the key, null if not defined
     */
    public String getKeyAttribute() {
        return isKeyed() ? attrs[1] : null;
    }

    public int getAttriuteCount() {
        return attrs.length / 2;
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        final String[] elems = attrs;
        return new Iterator<Entry<String,String>>() {
            int i = 0;
            @Override
            public boolean hasNext() {
                return i < elems.length;
            }

            @Override
            public Entry<String, String> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more attribute available.");
                }
                return new AbstractMap.SimpleImmutableEntry<>(elems[i++], elems[i++]);
            }
        };
    }

    @Override
    public int hashCode() {
        return series.hashCode() ^ instance.hashCode() ^ (int) time;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SeriesAnnotation && equalTo((SeriesAnnotation) obj);
    }

    public boolean equalTo(SeriesAnnotation other) {
        return time == other.time && series.equalTo(other.series) && instance.equals(other.instance);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(instance).append(' ').append(series.toString()).append('=').append(value).append(' ').append(time);
        str.append(":[");
        for (int i = 0; i < attrs.length; i+=2) {
            str.append("\n\t").append(attrs[i]).append('=').append(attrs[i+1]);
        }
        str.append("\n]");
        return str.toString();
    }
}