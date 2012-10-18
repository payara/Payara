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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.orb.admin.cli;

import java.beans.PropertyVetoException;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import java.util.List;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.internal.api.Target;
import org.glassfish.hk2.api.ServiceLocator;


@Service(name="delete-iiop-listener")
@PerLookup
@I18n("delete.iiop.listener")
@ExecuteOn(value={RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType(value={CommandTarget.CLUSTER,CommandTarget.CONFIG,
    CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE } )
public class DeleteIiopListener implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DeleteIiopListener.class);

    @Param(name="listener_id", primary=true)
    String listener_id;

    @Param( name="target", optional=true,
        defaultValue=SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target ;

    @Inject
    ServiceLocator services ;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        final Target targetUtil = services.getService(Target.class ) ;
        final Config config = targetUtil.getConfig(target) ;
        ActionReport report = context.getActionReport();
        IiopService iiopService = config.getExtensionByType(IiopService.class);

        if(!isIIOPListenerExists(iiopService)) {
            report.setMessage(localStrings.getLocalString("delete.iiop.listener" +
                ".notexists", "IIOP Listener {0} does not exist.", listener_id));
            report.setActionExitCode(ExitCode.FAILURE);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<IiopService>() {
                @Override
                public Object run(IiopService param) throws PropertyVetoException,
                        TransactionFailure {
                    List<IiopListener> listenerList = param.getIiopListener();
                    for (IiopListener listener : listenerList) {
                        String currListenerId = listener.getId();
                        if (currListenerId != null && currListenerId.equals
                                (listener_id)) {
                            listenerList.remove(listener);
                            break;
                        }
                    }
                    return listenerList;
                }
            }, iiopService);
            report.setActionExitCode(ExitCode.SUCCESS);
        } catch(TransactionFailure e) {
            String actual = e.getMessage();
            report.setMessage(localStrings.getLocalString(
                "delete.iiop.listener.fail", "failed", listener_id, actual));
            report.setActionExitCode(ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private boolean isIIOPListenerExists(IiopService iiopService) {
        for (IiopListener listener : iiopService.getIiopListener()) {
            String currListenerId = listener.getId();
            if (currListenerId != null && currListenerId.equals(listener_id)) {
                return true;
            }
        }
        return false;
    }
}
