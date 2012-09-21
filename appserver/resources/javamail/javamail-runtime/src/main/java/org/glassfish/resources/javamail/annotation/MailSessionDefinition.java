/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resources.javamail.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by IntelliJ IDEA.
 * User: naman mehta
 * Date: 17/4/12
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Annotation used to define a container <code>MailSession</code> and
 * be registered with JNDI. The <code>MailSession</code> may be configured by
 * setting the annotation elements for commonly used <code>DataSource</code>
 * properties.  Additional standard and vendor-specific properties may be
 * specified using the <code>properties</code> element.
 * <p/>
 * <p/>
 * The data source will be registered under the name specified in the
 * <code>name</code> element. It may be defined to be in any valid
 * <code>Java EE</code> namespace, and will determine the accessibility of
 * the data source from other components.
 * </pre>
 * <p/>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MailSessionDefinition {

    /**
     * Description of this mail session
     */
    String description() default "";

    /**
     * JNDI name by which the mail session will be registered.
     */
    String name();

    /**
     * Store Protocol name
     */
    String storeProtocol() default "";

    /**
     * Store Protocol implementation class
     */
    String storeProtocolClass() default "";

    /**
     * Transport Protocol name
     */
    String transportProtocol() default "";

    /**
     * Transport Protocol implementation class
     */
    String transportProtocolClass() default "";

    /**
     * Host name for the mail server.
     */
    String host() default "";

    /**
     * User name to use for authentication.
     */
    String user() default "";

    /**
     * Password to use for authentication.
     */
    String password() default "";

    /**
     * from address for the user.
     */
    String from() default "";

    /**
     * Used to specify  Vendor specific properties and less commonly
     * Properties are specified using the format:
     * <i>propertyName=propertyValue</i>  with one property per array element.
     */
    String[] properties() default {};

}
