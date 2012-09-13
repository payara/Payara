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
package javax.jms;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Annotation used to define a Java Message Service (JMS) connection
 *  factory and be registered with JNDI. The <code>JMSConnectionFactory</code>
 *  may be configured by setting the annotation elements for commonly used
 *  <code>JMSConnectionFactory</code> properties.  Additional standard and
 *  vendor-specific properties may be specified using the <code>properties
 *  </code> element. Once defined, a connection factory may be referenced by
 *  a component using the <code>lookup</code> element of the
 *  <code>Resource</code> annotation.
 *
 *  @see javax.annotation.Resource
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JMSConnectionFactoryDefinition {

    /**
     *  Description of this JMS connection factory.
     */
    String description() default "";

    /**
     *  JNDI name of the JMS connection factory being defined.
     */
    String name();

    /**
     *  JMS connection factory implementation class name which implements:
     *  <code>javax.jms.ConnectionFactory</code>
     *  or <code>javax.jms.QueueConnectionFactory</code>
     *  or <code>javax.jms.TopicConnectionFactory</code>
     */
    String className();

    /**
     *  Resource adapter name.
     */
    String resourceAdapterName() default "";

    /**
     *  User name to use for connection authentication.
     */
    String user() default "";

    /**
     *  Password to use for connection authentication.
     */
    String password() default "";

    /**
     *  Client id to use for connection.
     */
    String clientId() default "";

    /**
     *  JMS connection factory property.  This may be a vendor-specific
     *  property or a less commonly used <code>ConnectionFactory</code>
     *  property.
     *  <p>
     *  Properties are specified using the format:
     *  <i>propertyName=propertyValue</i> with one property per array element.
     */
    String[] properties() default {};

    /**
     *  Sets the maximum time in seconds that to wait while attempting to
     *  connect to the resource. A value of zero specifies that
     *  the timeout is the default system timeout if there is one; otherwise,
     *  it specifies that there is no timeout.
     *  <p>
     *  Default is vendor-specific.
     */
    int connectionTimeout() default 0;

    /**
     *  Set to <code>false</code> if connections should not participate
     *  in transactions.
     *  <p>
     *  Default is to enlist in a transaction when one is active or becomes
     *  active.
     */
    boolean transactional() default true;

    /**
     *  Number of connections that should be created when a connection pool
     *  is initialized.
     *  <p>
     *  Default is vendor-specific
     */
    int initialPoolSize() default -1;

    /**
     *  Maximum number of connections that should be concurrently allocated
     *  for a connection pool.
     *  <p>
     *  Default is vendor-specific.
     */
    int maxPoolSize() default -1;

    /**
     *  Minimum number of connections that should be concurrently allocated
     *  for a connection pool.
     *  <p>
     *  Default is vendor-specific.
     */
    int minPoolSize() default -1;

    /**
     *  The number of seconds that a physical connection should remain unused
     *  in the pool before the connection is closed for a connection pool.
     *  <p>
     *  Default is vendor-specific
     */
    int maxIdleTime() default -1;
}

