/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package javax.resource.spi;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Designates a JavaBean as an <code>ActivationSpec</code>. This annotation may
 * be placed on a JavaBean. A JavaBean annotated with the Activation annotation
 * is not required to implement the {@link ActivationSpec ActivationSpec}
 * interface.
 * 
 * <p>The ActivationSpec JavaBean contains the configuration information pertaining
 * to inbound connectivity from an EIS instance. A resource adapter capable of
 * message delivery to message endpoints must provide an JavaBean class
 * implementing the {@link ActivationSpec ActivationSpec} interface or annotate
 * a JavaBean with the <code>Activation</code> annotation for each supported
 * endpoint message listener type.
 * 
 * <p>The ActivationSpec JavaBean has a set of configurable properties specific to
 * the messaging style and the message provider.
 * 
 * <p>Together with the messageListener annotation element, this annotation
 * specifies information about a specific message listener type supported by the
 * messaging resource adapter.
 * 
 * @since 1.6
 * @version Java EE Connector Architecture 1.6
 */

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Activation {

	/**
	 * Indicates the message listener type(s) associated with this activation.
	 * 
	 * @return The Java types of the Message Listener interface this
	 *         activation-spec is associated with.
	 */
	Class[] messageListeners();
}
