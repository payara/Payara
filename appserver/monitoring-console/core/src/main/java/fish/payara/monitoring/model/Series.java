/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * An immutable structured representation of a data series key parsed into its {@link #tags} and {@link #metric} name.
 * 
 * @author Jan Bernitt
 */
public final class Series implements Comparable<Series>, Serializable {

    public static final char TAG_ASSIGN = ':';
    public static final char TAG_SEPARATOR = '\u001F'; //As tag values can be any valid UTF-8 character, use a control character to separate tags
    private static final char[] TAG_SEPARATORS = { TAG_SEPARATOR, TAG_ASSIGN };

    private static final String SPLIT_PATTERN = "[" + Pattern.quote(new String(TAG_SEPARATORS)) + "]+";

    private final String metric;
    private final String[] tags;
    private final String[] values;

    public Series(String key) {
        String[] parts = key.split(SPLIT_PATTERN);
        this.tags = new String[parts.length - 1];
        this.values = new String[tags.length];
        this.metric = parts[parts.length - 1].intern();
        for (int i = 0; i < parts.length - 1; i++) {
            int eqIndex = parts[i].indexOf(TAG_ASSIGN);
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
            str.append(tags[i]).append(TAG_ASSIGN).append(values[i]).append(TAG_SEPARATOR);
        }
        str.append(metric);
        return str.toString();
    }

    public static boolean isSpecialTagCharacter(char c) {
        if (c == TAG_ASSIGN) {
            return true;
        }
        return TAG_SEPARATOR == c;
    }
}
