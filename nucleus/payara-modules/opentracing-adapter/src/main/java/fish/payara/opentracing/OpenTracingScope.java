/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
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
 *  file and include the License.
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
package fish.payara.opentracing;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import io.opentracing.Span;

/**
 * Implementation of Scope from OpenTracing.
 * 
 * @author jonathan coustick
 * @since 5.183
 */
public class OpenTracingScope implements io.opentracing.Scope {

    private Span currentSpan;
    private Map<Span, Boolean> allSpans = new LinkedHashMap<>();
    
    @Override
    public void close() {
        Iterator<Span> keys = allSpans.keySet().iterator();
        while (keys.hasNext()){
            Span span = keys.next();
            if (allSpans.get(span)){
                span.finish();
            }
            // Prevent scope holding on a reference to old spans
            keys.remove();
        }
        
        currentSpan = null;
    }

    @Override
    public Span span() {
        return currentSpan;
    }
    
    // Package private - used only by ScopeManager
    void setSpan(Span span, Boolean finishOnClose){
        allSpans.put(span, finishOnClose);
        currentSpan = span;
    }
    
    void removeSpan(Span span){
        allSpans.remove(span);
        if (span == currentSpan) {
            currentSpan = null;
        }
    }
    
}
