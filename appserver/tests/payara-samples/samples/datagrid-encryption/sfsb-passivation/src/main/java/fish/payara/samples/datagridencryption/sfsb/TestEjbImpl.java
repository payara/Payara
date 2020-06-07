/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.datagridencryption.sfsb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Stateful;

/**
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@Stateful
public class TestEjbImpl implements TestEjb, Serializable {

    List<String> items;

    public TestEjbImpl() {
        items = new ArrayList<>();
    }

    @PostConstruct
    public void postConstruct () {
        System.out.println("##### New EJB #####");
    }

    @Override
    public void addItem(String item) {
        items.add(item);
    }

    @Override
    public void removeItem(String item) {
        items.remove(item);
    }

    @Override
    public String getItems() {
        String allItems = "";
        if (!items.isEmpty()) {
            for (String item : items) {
                allItems += item + ",";
            }
            allItems = allItems.substring(0, allItems.length() - 1);
        }
        return allItems;
    }

    @PrePassivate
    private void prePassivate() {
        System.out.println("##### Passivating... #####");
        System.out.println(getItems());
        System.out.println("##### Finished Passivating... #####");
    }

    @PostActivate
    private void postActivate() {
        System.out.println("##### Reactivating... #####");
        System.out.println(getItems());
        System.out.println("##### Finished Reactivating... #####");
    }
}
