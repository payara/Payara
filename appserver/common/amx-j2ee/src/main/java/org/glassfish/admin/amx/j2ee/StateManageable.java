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

 package org.glassfish.admin.amx.j2ee;

import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;

 public interface StateManageable
 {
 	/**
	 	This state indicates that the SMO has been requested to start,
	 	and is in the process of starting. On entering this state an SMO may generate
	 	an event whose type value is "STATE". Event notification of the STARTING state
	 	is optional for all managed objects that implement StateManageable.
 	 */
 	public static final int	STATE_STARTING	= 0;
 	
 	/**
	 	This is the normal running state for an SMO. This state indicates
	 	that the SMO is operational. On entering this state an SMO must generate an
	 	event whose type value is "STATE". Event notification of the RUNNING state is
	 	required for all managed objects that implement StateManageable.
 	 */
 	public static final int	STATE_RUNNING	= 1;
 	
 	/**
		This state indicates that the SMO has been requested to stop,
	 	and is in the process of stopping. On entering this state an SMO may generate
	 	an event whose type value is "STATE". Event notification of the STOPPING state
	 	is optional for all managed objects that implement StateManageable.
 	 */
 	public static final int	STATE_STOPPING	= 2;
 	
 	/**
		This state indicates that the StateManageable Object has stopped
	 	and can be restarted. On entering this state an SMO must generate an event
	 	whose type value is "STATE". Event notification of the STOPPED state is
	 	required by all managed objects that implement StateManageable.
 	 */
 	public static final int	STATE_STOPPED	= 3;
 	
 	/**
		This state indicates that the StateManageable Object is in a failed
	 	state and intervention is required to restore the managed object. On entering
	 	this state an SMO must generate an event whose type value is "STATE". Event
	 	notification of the FAILED state is required by all managed objects that
	 	implement StateManageable.
 	 */
 	public static final int	STATE_FAILED	= 4;
 	
 	/**
 	The current state of this SMO. The SMO can be in one of the following states:
 	<ul>
	 	<li>#STATE_STARTING </li>
	 	<li>#STATE_RUNNING </li>
	 	<li>#STATE_STOPPING </li>
	 	<li>#STATE_STOPPED </li>
	 	<li>#STATE_FAILED </li>
 	</ul>
		<p>
		Note that the Attribute name is case-sensitive
		"state" as defined by JSR 77.
 	*/
 	@ManagedAttribute
 	public int		getstate();
 	
 	/**
 		The time that the managed object was started represented as a
 		long which value is the number of milliseconds since
 		January 1, 1970, 00:00:00.
 		<p>
		Note that the Attribute name is case-sensitive
		"startTime" as defined by JSR 77.
 	 */
 	@ManagedAttribute
 	public long	getstartTime();
 	
 	/**
 		Starts the SMO. This operation can be invoked only when the SMO
 		is in the STOPPED state. It causes the SMO to go into the STARTING
 		state initially, and if it completes successfully, the SMO will be in
 		the RUNNING state. Note that start() is not called on any of the child
 		SMOs that are registered with this SMO; it is the responsibility of the
 		calling application to start the child SMO if this is required.
 	 */
 	@ManagedOperation
 	public void	start();
 	
 	/**
 		Starts the SMO. This operation can only be invoked when the SMO is in the
 		STOPPED state. It causes the SMO to go into the STARTING state initially,
 		and if it completes successfully, the SMO will be in the RUNNING state.
 		startRecursive() is called on all the child SMOs registered with this SMO
 		that are in the STOPPED state. Stops the SMO. This operation can only be
 		invoked when the SMO is in the RUNNING or STARTING state. It causes stop()
 		to be called on all the child SMOs registered with this SMO that are in the
 		RUNNING or STARTING state. It causes the SMO to go into the STOPPING state
 		initially, and if it completes successfully, the SMO and all the child SMOs
 		will be in the STOPPED state. There is no stopRecursive() operation because
 		it is mandatory if an SMO is in the STOPPED state, that all its child SMOs
 		must also be in the STOPPED state.
 	 */
 	@ManagedOperation
 	public void	startRecursive();
 	
 	/**
 		Stops the SMO. This operation can only be invoked when the SMO is in
 		the RUNNING or STARTING state. It causes stop() to be called on all the
 		child SMOs registered with this SMO that are in the RUNNING or STARTING
 		state. It causes the SMO to go into the STOPPING state initially,
 		and if it completes successfully, the SMO and all the child SMOs will be
 		in the STOPPED state. There is no stopRecursive() operation because it is
 		mandatory if an SMO is in the STOPPED state, that all its child SMOs must
 		also be in the STOPPED state.
 	 */
 	@ManagedOperation
 	public void	stop();
 };
 
