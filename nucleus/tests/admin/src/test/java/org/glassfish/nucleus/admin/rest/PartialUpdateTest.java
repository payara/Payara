/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.nucleus.admin.rest;

import java.util.HashMap;
import java.util.Map;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class PartialUpdateTest extends RestTestBase {
    @Test(enabled=false)
    // TODO: rework this to use something nucleus-friendly
    public void testPartialUpdate() {
        final String endpoint = JdbcTest.BASE_JDBC_CONNECTION_POOL_URL + "/DerbyPool";
        final String newDesc = generateRandomString();
        Map<String, String> origAttrs = getEntityValues(get(endpoint));
        Map<String, String> newAttrs = new HashMap<String, String>() {{
            put ("description", newDesc);
        }};
        post(endpoint, newAttrs);
        Map<String, String> updatedAttrs = getEntityValues(get(endpoint));
        assertEquals(newDesc, updatedAttrs.get("description"));
        assertEquals(origAttrs.get("driverClassname"), updatedAttrs.get("driverClassname"));
        assertEquals(origAttrs.get("resType"), updatedAttrs.get("resType"));
        assertEquals(origAttrs.get("validationClassname"), updatedAttrs.get("validationClassname"));
        assertEquals(origAttrs.get("datasourceClassname"), updatedAttrs.get("datasourceClassname"));
        assertEquals(origAttrs.get("name"), updatedAttrs.get("name"));
        assertEquals(origAttrs.get("transactionIsolationLevel"), updatedAttrs.get("transactionIsolationLevel"));
        assertEquals(origAttrs.get("initSql"), updatedAttrs.get("initSql"));
        assertEquals(origAttrs.get("sqlTraceListeners"), updatedAttrs.get("sqlTraceListeners"));
        assertEquals(origAttrs.get("validationTableName"), updatedAttrs.get("validationTableName"));
        assertEquals(origAttrs.get("resType"), updatedAttrs.get("resType"));
    }
}
