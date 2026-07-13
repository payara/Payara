/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.microprofile.openapi.impl.model;

import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;

import java.util.Map;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

public class PathsImpl extends ExtensibleTreeMap<PathItem, Paths> implements Paths {

    private static final long serialVersionUID = -3876996963579977405L;

    public PathsImpl() {
        super();
    }

    public PathsImpl(Map<String, ? extends PathItem> items) {
        super(items);
    }

    @Override
    public Paths addPathItem(String name, PathItem item) {
        if (item != null) {
            put(name, item);
        }
        return this;
    }

    @Override
    public void removePathItem(String name) {
        remove(name);
    }

    @Override
    public Map<String, PathItem> getPathItems() {
        return ModelUtils.readOnlyView(this);
    }

    @Override
    public void setPathItems(Map<String, PathItem> items) {
        clear();
        putAll(items);
    }

    public static void merge(Paths from, Paths to, boolean override) {
        if (from == null || to == null) {
            return;
        }
        from.getPathItems().entrySet().forEach(entry -> {
            if (!to.hasPathItem(entry.getKey())) {
                to.addPathItem(entry.getKey(), entry.getValue());
            } else {
                ModelUtils.merge(entry.getValue(), to.getPathItem(entry.getKey()), override);
            }
        });
    }

}