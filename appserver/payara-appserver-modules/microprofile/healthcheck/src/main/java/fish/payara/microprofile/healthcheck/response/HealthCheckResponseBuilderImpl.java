/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2020] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.microprofile.healthcheck.response;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import static org.glassfish.common.util.StringHelper.isEmpty;

/**
 * Base Implementation of HealthCheckResponseBuilder.
 * @author Andrew Pielage
 */
public class HealthCheckResponseBuilderImpl extends HealthCheckResponseBuilder {

    private String name;
    private Status status;
    private final Optional<Map<String, Object>> data = Optional.of(new HashMap<>());
    
    @Override
    public HealthCheckResponseBuilder name(String name) {
        // If the provided string isn't empty or null, set it as the name, otherwise throw an exception.
        if (!isEmpty(name)) {
            this.name = name;
            return this;
        } else {
            throw new IllegalArgumentException("Healthcheck name is null or empty");
        }
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, String value) {
        // If the provided string isn't empty or null, enter it into the Map, otherwise throw an exception.
        if (!isEmpty(key)) {
            data.get().put(key, value);
            return this;
        } else {
            throw new IllegalArgumentException("Healthcheck key is null or empty");
        }
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, long value) {
        // If the provided string isn't empty or null, enter it into the Map, otherwise throw an exception.
        if (!isEmpty(key)) {
            data.get().put(key, value);
            return this;
        } else {
            throw new IllegalArgumentException("Healthcheck key is null or empty");
        }
    }

    @Override
    public HealthCheckResponseBuilder withData(String key, boolean value) {
        // If the provided string isn't empty or null, enter it into the Map, otherwise throw an exception.
        if (!isEmpty(key)) {
            data.get().put(key, value);
            return this;
        } else {
            throw new IllegalArgumentException("Healthcheck key is null or empty");
        }
    }

    @Override
    public HealthCheckResponseBuilder up() {
        this.status = Status.UP;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder down() {
        this.status = Status.DOWN;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder status(boolean up) {
        if (up) {
            this.status = Status.UP;
        } else {
            this.status = Status.DOWN;
        }
        
        return this;
    }

    @Override
    public HealthCheckResponse build() {
        validate();
        // Just use the basic HealthCheckResponse implementation
        HealthCheckResponse healthCheckResponse = new HealthCheckResponseImpl(name, status, data);
        return healthCheckResponse;
    }
    
    private void validate() {
        if (isEmpty(name)) {
            throw new IllegalArgumentException("Healthcheck name is not defined");
        }
        if (status == null) {
            throw new IllegalArgumentException(String.format("Healthcheck [%s] status is not defined", name));
        }
    }
    
}