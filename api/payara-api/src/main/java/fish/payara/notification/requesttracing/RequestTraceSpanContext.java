/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.notification.requesttracing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *Span state that is copied across boundaries and stores a reference to the parent trace
 * 
 * @author andrew pielage
 * @author jonathan coustick
 * @since 5.183
 */
public class RequestTraceSpanContext implements Serializable, io.opentracing.SpanContext {

    private static final long serialVersionUID = 20180803L;

    private final UUID spanId;
    private UUID traceId;
    private final Map<String, String> baggageItems;

    protected RequestTraceSpanContext() {
        spanId = UUID.randomUUID();
        traceId = UUID.randomUUID();
        baggageItems = new HashMap<>();
    }

    protected RequestTraceSpanContext(UUID traceId) {
        spanId = UUID.randomUUID();
        this.traceId = traceId;
        baggageItems = new HashMap<>();
    }

    public RequestTraceSpanContext(UUID traceId, UUID parentId) {
        spanId = parentId;
        this.traceId = traceId;
        baggageItems = new HashMap<>();
    }

    public RequestTraceSpanContext(UUID traceId, UUID parentId, Map<String, String> baggageItems) {
        spanId = parentId;
        this.traceId = traceId;
        this.baggageItems = new HashMap<>(baggageItems);
    }

    public UUID getSpanId() {
        return spanId;
    }

    public UUID getTraceId() {
        return traceId;
    }

    public void setTraceId(UUID traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toTraceId() {
        return traceId.toString();
    }

    @Override
    public String toSpanId() {
        return spanId.toString();
    }

    public void addBaggageItem(String name, String value) {
        if (value != null) {
            // Escape any quotes
            baggageItems.put(name, value.replaceAll("\"", "\\\""));
        } else {
            baggageItems.put(name, value);
        }
    }

    public Map<String, String> getBaggageItems() {
        return baggageItems;
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return getBaggageItems().entrySet();
    }

}
