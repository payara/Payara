/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.logging.jul.cfg;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Extended {@link Properties} class. Same as {@link Properties}, it uses hashcodes for providing
 * values for property names, but the difference is in storing content to a file or stream, which is
 * sorted by keys.
 * <p>
 * Also provides some methods for more comfortable work.
 * <p>
 * Warning: using other than {@link String} objects can cause unexpected behavior.
 *
 * @author David Matejcek
 */
// note: this class must not use anything from JUL or PJULE packages, because the dependency is exactly opposite.
public class SortedProperties extends Properties {

    private static final long serialVersionUID = -2007016845217920652L;

    /**
     * Creates an empty property table.
     */
    public SortedProperties() {
        super();
    }


    /**
     * Makes a copy of provided properties.
     *
     * @param properties
     */
    public SortedProperties(final Properties properties) {
        super();
        putAll(properties);
    }


    /**
     * @return a sorted {@link Enumeration} of keys.
     */
    // affects storage!
    @Override
    public Enumeration<Object> keys() {
        return Collections.enumeration(Collections.list(super.keys()).stream()
            .sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
    }


    /**
     * @return all property names used in the current configuration.
     */
    public SortedSet<String> getPropertyNames() {
        return keySet().stream().map(String::valueOf).collect(Collectors.toCollection(TreeSet::new));
    }


    /**
     * @return sorted synchronized set of entries.
     */
    // affects storage!
    @Override
    public synchronized Set<Map.Entry<Object, Object>> entrySet() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final Comparator<Map.Entry<Object, Object>> comparator = (x, z) -> {
            if (x.getKey() instanceof Comparable && z.getKey() instanceof Comparable) {
                final Comparable key1 = (Comparable) x.getKey();
                final Comparable key2 = (Comparable) z.getKey();
                return key1.compareTo(key2);
            }
            return Integer.compare(x.getKey().hashCode(), z.getKey().hashCode());
        };
        return Collections.synchronizedSet(//
            super.entrySet().stream().sorted(comparator).collect(Collectors.toCollection(LinkedHashSet::new)));
    }


    /**
     * Writes this property list (key and element pairs) in this {@code Properties} table
     * to the file in a format suitable for loading into a {@code Properties} table using
     * the {@link #loadFrom(File)} method.
     *
     * @param outputFile
     * @param comments
     * @throws IOException
     */
    public void store(final File outputFile, final String comments) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            store(os, comments);
        }
    }


    /**
     * Writes this property list (key and element pairs) in this {@code Properties} table
     * to the file in a format suitable for loading into a {@code Properties} table using
     * the {@link #loadFrom(InputStream)} method.
     *
     * @param comments
     * @return {@link ByteArrayInputStream}
     * @throws IOException
     */
    public ByteArrayInputStream toInputStream(final String comments) throws IOException {
        final ByteArrayOutputStream outputstream = new ByteArrayOutputStream(32768);
        this.store(outputstream, comments);
        return new ByteArrayInputStream(outputstream.toByteArray());
    }


    @Override
    public SortedProperties clone() {
        return (SortedProperties) super.clone();
    }


    /**
     * Loads a {@link SortedProperties} from a {@link File}.
     *
     * @param file
     * @return {@link SortedProperties}
     * @throws IOException
     */
    public static SortedProperties loadFrom(final File file) throws IOException {
        if (!file.canRead()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            return loadFrom(input);
        }
    }


    /**
     * Loads a {@link SortedProperties} from an {@link InputStream}.
     *
     * @param stream
     * @return {@link SortedProperties}
     * @throws IOException
     */
    public static SortedProperties loadFrom(final InputStream stream) throws IOException {
        final SortedProperties properties = new SortedProperties();
        properties.load(stream);
        return properties;
    }
}