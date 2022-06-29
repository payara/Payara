/*
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.examples;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;

/**
 * Resource for people, using ManagedExecutorService.
 *
 * @author mertcaliskan, Petr Aubrecht
 */
@Stateless
@Path("person")
public class PersonResource {

    @Resource(lookup = "java:app/concurrent/MESPayara")
    private ManagedExecutorService mes;

    @Inject
    private PersonDao personDao;

    @GET
    @Produces("application/json")
    public List<Person> all() {
        return personDao.getAll();
    }

    @POST
    @Consumes("application/json")
    public void save(Person person) {
        personDao.save(person);
    }

    @POST
    @Path("create-demo")
    @Consumes("application/json")
    public void createDemo(Person person) throws InterruptedException, ExecutionException {
        Future<?> future1 = mes.submit(() -> {
            Person p1 = new Person("John", "Doe");
            personDao.save(p1);
        });
        Future<?> future2 = mes.submit(() -> {
            Person p2 = new Person("Jane", "Doe");
            personDao.save(p2);
        });
        future1.get();
        future2.get();
    }

    @PUT
    @Consumes("application/json")
    public void update(Person person) {
        personDao.update(person);
    }

    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) {
        Person person = personDao.find(id);
        personDao.delete(person);
    }
}
