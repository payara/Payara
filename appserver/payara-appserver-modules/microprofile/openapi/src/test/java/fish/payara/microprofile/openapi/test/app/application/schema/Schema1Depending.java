/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.test.app.application.schema;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Class, which depends on Schema2Simple. "1" in the name ensures this class is
 * processed BEFORE Schema2Simple.
 *
 * @author Petr Aubrecht <petr@aubrecht.net>
 */
@Schema(name = "Schema1Depending", description = "Schema1Depending data", implementation = Schema1Depending.class)
public class Schema1Depending {

    @Schema(name = "id", example = "1", description = "ID of the partner", implementation = Integer.class)
    private Integer id;

    @Schema(name = "legal", example = "GmbH", description = "Legal of Company", enumeration = {"GmbH", "AG"}, implementation = String.class)
    private String legal;

    @Schema(required = true, name = "schema2Simple", description = "Schema2Simple description GHANGED", implementation = Schema2Simple.class)
    private Schema2Simple schema2Simple;

    @Schema(required = true, ref = "#/components/schemas/Schema2Simple")
    private Schema2Simple schema2SimpleRef;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLegal() {
        return legal;
    }

    public void setLegal(String legal) {
        this.legal = legal;
    }

    public Schema2Simple getSchema2SimpleRef() {
        return schema2SimpleRef;
    }

    public void setSchema2SimpleRef(Schema2Simple schema2SimpleRef) {
        this.schema2SimpleRef = schema2SimpleRef;
    }

    public Schema2Simple getSchema2Simple() {
        return schema2Simple;
    }

    public void setSchema2Simple(Schema2Simple schema2Simple) {
        this.schema2Simple = schema2Simple;
    }

}
