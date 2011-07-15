/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/* Shifter.java
 * $Id: Shifter.java,v 1.3 2005/12/25 04:26:34 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:34 $
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. Tabs are preferred over spaces.
 * 2. In vi/vim -
 *		:set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *		1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *		2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = False.
 *		3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 */

package com.sun.enterprise.admin.jmx.remote.internal;


/**
 *
 * @author  <a href="mailto:Kedar.Mhaswade@sun.com">Kedar Mhaswade</a>
 * @since S1AS8.0
 * @version $Revision: 1.3 $
 */
public final class Shifter {
	
	private Object[] args;
	public Shifter(Object[] in) {
		if (in == null)
			throw new IllegalArgumentException("null array");
		this.args = new Object[in.length];
		System.arraycopy(in, 0, args, 0, in.length);
	}
	
	public void shiftRight(Object addition) {
		if (addition == null)
			throw new IllegalArgumentException ("Null argument");
		final Object[] tmp = new Object[args.length + 1];
		tmp[0] = addition;
		for (int i = 0 ; i < args.length ; i++) {
			tmp[i + 1] = args[i];
		}
		args = tmp;
	}
	
	public Object shiftLeft() {
		if (args.length == 0)
			throw new IllegalStateException("Can't Shift left, no elements");
		final Object ret = args[0];
		final Object[] tmp = new Object[args.length - 1];
		for (int i = 0 ; i < tmp.length ; i++) {
			tmp[i] = args[i + 1];
		}
		args = tmp;
		return ( ret );
	}
	
	public Object[] state() {
		return ( args );
	}
}
