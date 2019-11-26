/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.security.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * {@link FileIdentityStoreDefinition} annotation defines configuration of
 * dynamically created File Realm and The value of file & assignGroups parameter
 * can be overwritten via mp config properties.
 *
 * @author Gaurav Gupta
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface FileIdentityStoreDefinition {

    /**
     * Required. The realm name, created dynamically during CDI scanning.
     *
     * @return
     */
    String value() default "file";

    /**
     * Optional. The location of file to store user credentials locally. If no
     * file name defined then by default realm name is used as file name.
     *
     * The file location can also be configured by using MicroProfile Config
     * property "payara.security.file".
     *
     * @return
     */
    String file() default "";

    /**
     * Optional. Users are assigned membership to these groups for the purposes
     * of authorization decisions.
     *
     * The assign groups can also be configured by using MicroProfile Config
     * property "payara.security.file.assignGroups".
     *
     * @return
     */
    String[] assignGroups() default {};

    /**
     * Optional. The JAAS Context of File realm.
     *
     * The JAAS Context can also be configured by using MicroProfile Config
     * property "payara.security.file.jaasContext".
     *
     * @return
     */
    String jaasContext() default "fileRealm";

    /**
     * The MicroProfile Config key for the file location is
     * <code>{@value}</code>
     */
    String STORE_MP_FILE = "payara.security.file";

    /**
     * The MicroProfile Config key for the assign groups is
     * <code>{@value}</code>
     */
    String STORE_MP_FILE_GROUPS = "payara.security.file.assignGroups";

    /**
     * The MicroProfile Config key for the jaas context is <code>{@value}</code>
     */
    String STORE_MP_FILE_JAAS_CONTEXT = "payara.security.file.jaasContext";

}
