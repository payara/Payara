/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package javax.resource.spi.work;

import java.io.Serializable;

/**
 * This class serves as a standard mechanism for a resource adapter to propagate
 * an imported context from an enterprise information system to an application
 * server.
 * 
 * <p>
 * A <code>Work</code> instance, that implements the
 * <code>WorkContextProvider</code>, could provide a <code>List</code> of these
 * <code>WorkContext</code> instances (through the getWorkContexts() method),
 * and have them setup as the execution context by the <code>WorkManager</code>
 * when the <code>Work</code> instance gets executed.
 *
 * The resource adapter must not make any changes to the state of the 
 * <code>WorkContext</code> after the <code>Work</code> instance corresponding 
 * to that <code>WorkContext</code> has been submitted to the <code>WorkManager</code>.
 *
 * @since 1.6
 * @version Java EE Connector Architecture 1.6
 */

public interface WorkContext extends Serializable {
	/**
	 * Get the associated name of the <code>WorkContext</code>. This could be
	 * used by the WorkManager and the resource adapter for debugging purposes.
	 * <p>
	 * 
	 * @return the associated name of the <code>WorkContext</code>
	 */
	String getName();

	/**
	 * Get the brief description of the role played by the
	 * <code>WorkContext</code> and any other related debugging information.
	 * This could be used by the WorkManager and the resource adapter for
	 * debugging purposes.
	 * <p>
	 * 
	 * @return the associated description of the <code>WorkContext</code>
	 */
	String getDescription();

}
