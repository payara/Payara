/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */

package fish.payara.microprofile.metrics;

import java.util.Arrays;
import java.util.List;
import org.eclipse.microprofile.metrics.MetricRegistry;

public class Constants {

    // HTTP Headers
    public final static String ACCEPT_HEADER = "Accept";
    public final static String ACCEPT_HEADER_JSON = "application/json";
    public final static String ACCEPT_HEADER_TEXT = "text/plain";

    //Content Types
    public final static String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";
    public final static String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    // HTTP Methods
    public final static String METHOD_GET = "GET";
    public final static String METHOD_OPTIONS = "OPTIONS";

    // Registry Names
    private final static String[] REGISTRY_NAMES_ARRAY = {
        MetricRegistry.Type.BASE.getName(),
        MetricRegistry.Type.VENDOR.getName(),
        MetricRegistry.Type.APPLICATION.getName()
    };
    public final static List<String> REGISTRY_NAMES_LIST = Arrays.asList(REGISTRY_NAMES_ARRAY);

    // Dropwizard Histogram, Meter, or Timer Constants
    public final static String COUNT = "count";
    public final static String MEAN_RATE = "meanRate";
    public final static String ONE_MINUTE_RATE = "oneMinRate";
    public final static String FIVE_MINUTE_RATE = "fiveMinRate";
    public final static String FIFTEEN_MINUTE_RATE = "fifteenMinRate";
    public final static String MAX = "max";
    public final static String MEAN = "mean";
    public final static String MIN = "min";
    public final static String STD_DEV = "stddev";
    public final static String MEDIAN = "p50";
    public final static String PERCENTILE_75TH = "p75";
    public final static String PERCENTILE_95TH = "p95";
    public final static String PERCENTILE_98TH = "p98";
    public final static String PERCENTILE_99TH = "p99";
    public final static String PERCENTILE_999TH = "p999";
    
    public final static String QUANTILE = "quantile";
    public final static String RATE = "_rate_";
    public final static String ONE_MIN_RATE = "_one_min_rate_";
    public final static String FIVE_MIN_RATE = "_five_min_rate_";
    public final static String FIFTEEN_MIN_RATE = "_fifteen_min_rate_";

    //Appended Units for prometheus
    public final static String APPENDED_SECONDS = "_seconds";
    public final static String APPENDED_BYTES = "_bytes";
    public final static String APPENDED_PERCENT = "_percent";

    //Conversion factors
    public final static double NANOSECOND_CONVERSION = 0.000000001;
    public final static double MICROSECOND_CONVERSION = 0.000001;
    public final static double MILLISECOND_CONVERSION = 0.001;
    public final static double SECOND_CONVERSION = 1;
    public final static double MINUTE_CONVERSION = 60;
    public final static double HOUR_CONVERSION = 3600;
    public final static double DAY_CONVERSION = 86400;
    public final static double BYTE_CONVERSION = 1;
    public final static double KILOBYTE_CONVERSION = 1024;
    public final static double MEGABYTE_CONVERSION = 1048576;
    public final static double GIGABYTE_CONVERSION = 1073741824;
    public final static double BIT_CONVERSION = 0.125;
    public final static double KILOBIT_CONVERSION = 125;
    public final static double MEGABIT_CONVERSION = 125000;
    public final static double GIGABIT_CONVERSION = 1.25e+8;
    public final static double KIBIBIT_CONVERSION = 128;
    public final static double MEBIBIT_CONVERSION = 131072;
    public final static double GIBIBIT_CONVERSION = 1.342e+8;
}
