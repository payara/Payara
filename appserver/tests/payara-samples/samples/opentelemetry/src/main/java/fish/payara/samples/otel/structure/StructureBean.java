/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *  its licensees are provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.samples.otel.structure;

import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TimerService;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.transaction.Transactional;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless
public class StructureBean {

    @EJB
    private FlattyCounter flatty;

    @Transactional(Transactional.TxType.REQUIRED)
    public void transactional() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Retry(maxRetries = 2, delay = 10)
    public void flaky() {
        if (!flatty.attemptFlaky()) {
            throw new IllegalStateException("Flaky method failure");
        }
    }

    public void startTimer() {
        try {
            TimerService timerService = (TimerService)
                new InitialContext().lookup("java:comp/TimerService");
            timerService.createSingleActionTimer(100, new TimerConfig("structure", false));
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Timeout
    public void timeoutHandler(Timer timer) {
        // timer executed
    }
}
