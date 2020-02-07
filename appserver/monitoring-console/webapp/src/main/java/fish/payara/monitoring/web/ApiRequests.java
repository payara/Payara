/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.web;

import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.web.ApiResponses.AlertFrame;
import fish.payara.monitoring.web.ApiResponses.SeriesData;

/**
 * Types used in the web API to map requests.
 * 
 * @see ApiResponses
 * 
 * @author Jan Bernitt
 */
@SuppressWarnings("squid:S1104")
public final class ApiRequests {

    public enum DataType { POINTS, WATCHES, ALERTS, ANNOTATIONS }

    /**
     * A container for a full request consisting of one or more {@link SeriesQuery}s.
     */
    public static final class SeriesRequest {

        public SeriesQuery[] queries;

        public SeriesRequest() {
            // from JSON
        }

        SeriesRequest(String series) {
            this(new SeriesQuery(series));
        }

        SeriesRequest(SeriesQuery...queries) {
            this.queries = queries;
        }

    }

    /**
     * A query for a particular {@link Series} or set of {@link Series} by giving its {@link #series} name or pattern
     * and the {@link #instances} to include in the result data.
     */
    public static final class SeriesQuery {

        /**
         * The name or pattern of the series (* can be used as wild-card for tag values)
         */
        public String series;
        /**
         * What instances to include in the result, an empty or null set includes all available sets
         */
        public String[] instances;
        /**
         * When {@link DataType#ALERTS} is contained alerts will only contains the most recent {@link AlertFrame}.
         * When {@link DataType#POINTS} is contained {@link SeriesData} will only contain the most recent point.
         */
        public DataType[] truncate;

        public DataType[] exclude;

        public SeriesQuery() {
            // from JSON
        }

        SeriesQuery(String series, String... instances) {
            this.series = series;
            this.instances = instances;
            this.truncate = new DataType[] { DataType.ALERTS };
            this.exclude = new DataType[0];
        }

        public boolean excludes(DataType type) {
            return contains(exclude, type);
        }

        public boolean truncates(DataType type) {
            return contains(truncate, type);
        }

        private static boolean contains(DataType[] set, DataType type) {
            for (DataType t : set) {
                if (t == type) {
                    return true;
                }
            }
            return false;
        }
    }

}
