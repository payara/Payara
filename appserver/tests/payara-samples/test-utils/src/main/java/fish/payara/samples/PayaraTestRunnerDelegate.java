/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.samples;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * A class used by all Payara test runners to utilise the custom annotations.
 *
 * @author Matt Gill
 */
public class PayaraTestRunnerDelegate extends BlockJUnit4ClassRunner {
    private static final String PAYARA_VERSION_PROPERTY_NAME = "payara.version";

    private boolean skipEntireClass;

    private final PayaraVersion version;
    private final boolean isPayaraMicro;

    public PayaraTestRunnerDelegate(Class<?> klass) throws InitializationError {
        super(klass);

        this.version = new PayaraVersion(System.getProperty(PAYARA_VERSION_PROPERTY_NAME));

        final SincePayara sinceAnnotation = klass.getAnnotation(SincePayara.class);
        if (sinceAnnotation != null) {

            // Get versions to compare
            PayaraVersion since = new PayaraVersion(sinceAnnotation.value());

            if (version.isValid() && since.isValid()) {
                skipEntireClass |= !version.isAtLeast(since);
            }
        }

        this.isPayaraMicro = ServerOperations.isMicro();
        if (klass.getAnnotation(NotMicroCompatible.class) != null) {
            skipEntireClass |= this.isPayaraMicro;
        }
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {

        List<FrameworkMethod> result = new ArrayList<>();

        if (skipEntireClass) {
            return result;
        }

        for (FrameworkMethod testMethod : super.computeTestMethods()) {

            // If the SinceVersion exists and the current Payara version is not greater than
            // or equal to it
            final SincePayara sincePayaraAnnotation = testMethod.getAnnotation(SincePayara.class);
            final PayaraVersion sinceVersion = new PayaraVersion(sincePayaraAnnotation != null? sincePayaraAnnotation.value() : null);
            if (version.isValid() && sinceVersion.isValid() && !version.isAtLeast(version)) {
                continue;
            }

            // If the test is not micro compatible and micro is enabled
            if (this.isPayaraMicro && testMethod.getAnnotation(NotMicroCompatible.class) != null) {
                continue;
            }

            result.add(testMethod);
        }

        return result;
    }

    @Override
    protected void collectInitializationErrors(List<Throwable> errors) {
        // Do nothing - this class is for test calculation only
    }
}
