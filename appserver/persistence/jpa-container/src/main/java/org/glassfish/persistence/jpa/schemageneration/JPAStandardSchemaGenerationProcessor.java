/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.persistence.jpa.schemageneration;

import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import org.glassfish.api.deployment.DeploymentContext;

import java.util.HashMap;
import java.util.Map;


/**
 * Schema generation processor while using standard JPA based schema generation
 * @author Mitesh Meswani
 */
public class JPAStandardSchemaGenerationProcessor implements SchemaGenerationProcessor {
    private static final String SCHEMA_GENERATION_ACTION_PROPERTY = "javax.persistence.schema-generation-action"; //TODO Update the name once EclipseLink updates
    private static final String SCHEMA_GENERATION_ACTION_NONE = "none";

    @Override
    public void init(PersistenceUnitDescriptor pud, DeploymentContext deploymentContext) {
        // Nothing to init
    }

    @Override
    public Map<String, String> getOverridesForSchemaGeneration() {
        // No override is needed now. When we wire in taking schema generation overrides from deploy CLI, this method will return corresponidng overrides.
        return null;
    }

    @Override
    public Map<String, String> getOverridesForSuppressingSchemaGeneration() {
        Map<String, String> overrides = new HashMap<>();
        overrides.put(SCHEMA_GENERATION_ACTION_PROPERTY, SCHEMA_GENERATION_ACTION_NONE);
        return overrides;
    }

    @Override
    public boolean isContainerDDLExecutionRequired() {
        // DDL execution is done by JPA provider.
        return false;
    }

    @Override
    public void executeCreateDDL() {
        // We should never reach here as this processor returns false for isContainerDDLExecutionRequired()
        throw new UnsupportedOperationException();
    }
}
