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
//Portions Copyright [2018] [Payara Foundation]

package org.glassfish.admin.amx.util.stringifier;

/**
	Convert an object to a String.  The intent of this is to provide a flexible means
	to control the string representation of an Object. The toString() routine has many
	issues, including:
	- appropriateness for end-user viewing (within a CLI for example)
	- an object may not have implemented a toString() method
	- the output of toString() may simply be unacceptable (eg class@eebc1933)
	- it may be desirable to have many variations on the output
	- modifying toString() requires modifying the orignal class; a Stringifier
	or many of them can exist independently, making it easy to apply many different
	types of formatting to the same class.

	The intended use is generally to have a separate class implement Stringifier, rather
	than the class to be stringified.
 */
public interface Stringifier
{
	/**
		Produce a String representation of an object.  The actual output has no
		other semantics; each Stringifier may choose to target a particular type
		of user.
		<p>
		The resulting String should be suitable for display to a user.

		@param object	the Object for which a String should be produced
	 */
	String	stringify( Object object );
}

