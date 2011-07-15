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
 * This class models the possible error conditions that might occur during
 * associating an <code>WorkContext</code> with a <code>Work</code> instance.
 * 
 * <p>
 * This class is not designed as an Enumerated type (Enum), as the error codes
 * listed below could be expanded to accommodate custom error conditions for
 * custom <code>WorkContext</code> types.
 * 
 * @since 1.6
 * @version Java EE Connector Architecture 1.6
 */
public class WorkContextErrorCodes {

	/**
	 * Indicates that a <code>WorkContext</code> type, that was not specified as
	 * optional, passed in by the <code>Work</code> instance is not supported by
	 * the container.
	 * 
	 * @since 1.6
	 */
	public static final String UNSUPPORTED_CONTEXT_TYPE = "1";

	/**
	 * Indicates that there are more than one instance of a <code>WorkContext</code>
	 * type passed in by the <code>Work</code> instance.
	 * <p>
	 * 
	 * @since 1.6
	 */
	public static final String DUPLICATE_CONTEXTS = "2";

	/**
	 * Indicates a failure in recreating the <code>WorkContext</code> instance.
	 * For <code>TransactionContext</code> instances, the
	 * <code>WorkManager</code> must use this failure code when it should have
	 * used {@link WorkException#TX_RECREATE_FAILED} as the error code.
	 * 
	 * @since 1.6
	 */
	public static final String CONTEXT_SETUP_FAILED = "3";

	/**
	 * Indicates that the container cannot support recreating the
	 * <code>WorkContext</code> instance. For
	 * <code>TransactionContext</code> instances, the
	 * <code>WorkManager</code> must use this failure code when it should have
	 * used {@link WorkException#TX_CONCURRENT_WORK_DISALLOWED} as the error
	 * code.
	 * 
	 * @since 1.6
	 */
	public static final String CONTEXT_SETUP_UNSUPPORTED = "4";

}
