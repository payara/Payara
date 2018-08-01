/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.openid.api;

import static java.util.Objects.isNull;

/**
 * Prompt specifies whether the Authorization Server prompts the End-User for
 * re-authentication and consent.
 *
 * @author Gaurav Gupta
 */
public enum PromptType {

    /**
     * The Authorization Server must not display any authentication or consent 
     * user interface pages. An error is returned if an End-User is not already 
     * authenticated or the Client does not have pre-configured consent for the 
     * requested Claims.
     * ErrorCode : login_required, interaction_required
     */
    NONE,

    /**
     * The Authorization Server SHOULD prompt the End-User for reauthentication.
     * If it cannot reauthenticate the End-User, it MUST return an error.
     * ErrorCode : login_required
     * 
     */
    LOGIN,

    /**
     * The Authorization Server SHOULD prompt the End-User for consent before 
     * returning information to the Client. If it cannot obtain consent, it must
     * return an error.
     * ErrorCode : consent_required
     * 
     */
    CONSENT,

    /**
     * The Authorization Server SHOULD prompt the End-User to select a user 
     * account. If it cannot obtain an account selection choice made by the 
     * end-user, it must return an error.
     * ErrorCode : account_selection_required
     * 
     */
    SELECT_ACCOUNT;

    public static PromptType fromString(String key) {
        return isNull(key) || key.trim().isEmpty() ? null : valueOf(key.toUpperCase());
    }

}
