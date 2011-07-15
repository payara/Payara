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

/**
 * This class models the various events that occur during the processing of the
 * <code>WorkContext</code>s associated with a <code>Work</code> instance. This
 * interface may be implemented by a <code>WorkContext</code> instance to
 * receive notifications from the <code>WorkManager</code> when the
 * <code>WorkContext</code> is set as the execution context of the
 * <code>Work</code> instance it is associated with.
 * <p>
 * 
 * When a <code>WorkManager</code> sets up the execution context of a
 * <code>Work</code> instance that implements <code>WorkContextProvider</code>,
 * the <code>WorkManager</code> must make the relevant lifecycle notifications
 * if an <code>WorkContext</code> instance implements this interface.
 * <p>
 * 
 * When a <code>Work</code> instance is submitted to the Connector
 * <code>WorkManager</code> using one of the methods that passes in a
 * <code>WorkListener</code> as a parameter, the <code>WorkManager</code> must
 * send <code>Work</code> related notifications to the <code>WorkListener</code>
 * and <code>WorkContext</code> setup related notifications to this interface.
 * <p>
 * 
 * The possible error conditions that might occur during associating an
 * <code>WorkContext</code> with a <code>Work</code> instance is captured in
 * {@link WorkContextErrorCodes}.
 * <p>
 * 
 * @since 1.6
 * @version Java EE Connector Architecture 1.6
 */

public interface WorkContextLifecycleListener {

	/**
	 * Invoked when the <code>WorkContext</code> instance was successfully set
	 * as the execution context for the <code>Work</code> instance.
	 * 
	 * @since 1.6
	 */
	void contextSetupComplete();

	/**
	 * Invoked when the <code>WorkContext</code> instance was set as the
	 * execution context for the <code>Work</code> instance it was associated
	 * with.
	 * 
	 * @param errorCode
	 *            One of the error-codes defined in or subclasses of
	 *            {@link WorkContextErrorCodes WorkContextErrorCodes}
	 * @since 1.6
	 * @see WorkContextErrorCodes
	 */
	void contextSetupFailed(String errorCode);
}
