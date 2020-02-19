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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

public final class Metric {

    public final Series series;
    public final Unit unit;

    public Metric(Series series) {
        this(series, Unit.COUNT);
    }

    public Metric(Series series, Unit unit) {
        this.series = series;
        this.unit = unit;
    }

    public Metric withUnit(Unit unit) {
        return new Metric(series, unit);
    }

    @Override
    public int hashCode() {
        return series.hashCode() ^ unit.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Metric && equalTo((Metric) obj);
    }

    public boolean equalTo(Metric other) {
        return series.equalTo(other.series) && unit == other.unit;
    }

    @Override
    public String toString() {
        return series + " unit:" + unit.toString();
    }

    public static Metric parse(String series, String unit) {
        return new Metric(new Series(series), Unit.fromShortName(unit));
    }

    public JsonObject toJSON() {
        return Json.createObjectBuilder()
                .add("series", series.toString())
                .add("unit", unit.toString())
                .build();
    }

    public static Metric fromJSON(JsonValue value) {
        if (value == null || value == JsonValue.NULL) {
            return null;
        }
        JsonObject obj = value.asJsonObject();
        return new Metric(new Series(obj.getString("series")), Unit.fromShortName(obj.getString("unit", "count")));
    }
}
