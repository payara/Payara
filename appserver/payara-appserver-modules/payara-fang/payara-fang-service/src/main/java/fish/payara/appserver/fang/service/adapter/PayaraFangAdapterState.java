/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.appserver.fang.service.adapter;

/**
 *
 * @author Andrew Pielage
 */
public enum PayaraFangAdapterState {
    NOT_LOADED("state.notLoaded", "Payara Fang is registered in the config but not loaded yet"),
    LOADING("state.loading", "Payara Fang is loading"),
    LOADED("state.loaded", "Payara Fang is loaded"),
    UNINITIALISED("state.uninitialised", "Payara Fang has not been initialised yet"),
    REGISTERING("state.registering", "Payara Fang is being registered as a system application"),
    NOT_REGISTERED("state.notRegistered", "Payara Fang is not registered in the config"),
    RECONFIGURING("state.reconfiguring", "Payara Fang system-application entry is being reconfigured"),
    WELCOME_TO("status.welcometo", "Welcome to ");
    
    private final String desc;
    private final String i18nKey;
    
    private PayaraFangAdapterState(String i18nKey, String desc) {
	this.i18nKey = i18nKey;
        this.desc = desc;
    }

    /**
     *	This is the key that should be used to retrieve the localised message from a properties file.
     */
    public String getI18NKey() {
	return i18nKey;
    }
    
    @Override
    public String toString() {
        return (desc);
    }
}
