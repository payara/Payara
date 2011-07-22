/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDLGPL_1_1.html
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

package com.sun.enterprise.transaction.config;

import com.sun.enterprise.config.serverbeans.Config;
import org.glassfish.config.support.DefaultComponentUpgrade;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.xml.stream.XMLStreamReader;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default configuration upgrade for Transaction Service.
 * @author Jerome Dochez
 */
@Service(name = "transaction-service")
public class DefaultConfigUpgrade implements DefaultComponentUpgrade {
    @Override
    public void apply(final XMLStreamReader parser, Config defaultConfig) {
        try {
            ConfigSupport.apply(new SingleConfigCode<Config>() {
                @Override
                public Object run(Config config) throws PropertyVetoException, TransactionFailure {
                    TransactionService ts = config.createChild(TransactionService.class);
                    config.getContainers().add(ts);
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attr = parser.getAttributeLocalName(i);
                        String val = parser.getAttributeValue(i);
                        if (attr.equals("tx-log-dir")) {
                            ts.setTxLogDir(val);
                        }
                        if (attr.equals("automatic-recovery")) {
                            ts.setAutomaticRecovery(val);
                        }
                    }
                    return null;
                }
            }, defaultConfig);
        } catch (TransactionFailure ex) {
            Logger.getLogger(DefaultConfigUpgrade.class.getName()).log(
                    Level.SEVERE, "Failure create TransactionService config object", ex);
        }
    }
}