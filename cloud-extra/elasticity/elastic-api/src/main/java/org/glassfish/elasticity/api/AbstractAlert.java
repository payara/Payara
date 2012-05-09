/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.api;

import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.component.Habitat;

import org.glassfish.elasticity.config.serverbeans.AlertConfig;

import javax.inject.Inject;

/**
 * An Alert typically uses some metrics and determines the Alert's state.
 * The ElasticityEngine will then use this Alert's state to execute a set
 * of Actions.
 *
 * @author Mahesh.Kannan@Oracle.Com
 *
 */
@Contract
public abstract class AbstractAlert<C extends AlertConfig>
    implements Runnable {

	public enum AlertState {OK, ALARM, NO_DATA};

    @Inject
    private Habitat services;

    private C config;

    private AlertContext ctx;

	/**
	 * Called by the framework when the rule is instantiated.
	 *
	 * @param config
	 */
	public final void init(C config) {
        this.config = config;
        initialize();
    }

    protected void initialize() {

    }
	
	/**
	 * Execute this rule and return the state of this rule
	 * 
	 * @return the rule's state
	 */
	public abstract AlertState execute(AlertContext<C> ctx);

    public void run() {
        execute(ctx);
    }
	
}
