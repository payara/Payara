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
package fish.payara.monitoring.web;

import static java.util.stream.StreamSupport.stream;

import java.util.Map;
import java.util.TreeMap;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fish.payara.monitoring.store.MonitoringDataStore;
import fish.payara.monitoring.store.Point;
import fish.payara.monitoring.store.Series;
import fish.payara.monitoring.store.SeriesSlidingWindow;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class MonitoringConsoleResouce {

    @Inject
    private MonitoringDataStore data;

    @GET
    @Path("/points/newest")
    public Map<String, Long> getMostRecentPoints() {
        Map<String, Long> res = new TreeMap<>();
        for (SeriesSlidingWindow slidingWindow : data.selectAllSeriesWindow()) {
            res.put(slidingWindow.getSeries().toString(), slidingWindow.last());
        }
        return res;
    }

    @GET
    @Path("/points")
    public Map<String, Point[]> getSlidingWindows() {
        Map<String, Point[]> res = new TreeMap<>();
        for (SeriesSlidingWindow slidingWindow : data.selectAllSeriesWindow()) {
            res.put(slidingWindow.getSeries().toString(), slidingWindow.snapshot());
        }
        return res;
    }

    @GET
    @Path("/points/series/{series}")
    public Point[] getSlidingWindow(@PathParam("series") String series) {
        return data.selectSlidingWindow(new Series(series)).snapshot();
    }

    @GET
    @Path("/points/series/")
    public String[] getSlidingWindowSeries() {
        return stream(data.selectAllSeriesWindow().spliterator(), false)
                .map(window -> window.getSeries().toString()).toArray(String[]::new);
    }
}
