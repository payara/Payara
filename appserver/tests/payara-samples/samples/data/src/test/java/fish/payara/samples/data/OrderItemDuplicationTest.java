/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
package fish.payara.samples.data;

import fish.payara.samples.data.entity.Order;
import fish.payara.samples.data.entity.OrderItem;
import fish.payara.samples.data.repo.OrderItems;
import fish.payara.samples.data.repo.Orders;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// Helpers kept inside the test class to avoid relying on Jakarta Data
// generated queries for the assertions we make about row counts.

/**
 * Reproducer for the Parent-Child duplication issue reported against
 * Jakarta Data 1.0 on Payara 7.2026.1: persisting a single child entity
 * (OrderItem) attached to an existing parent (Order) was producing two rows
 * in the OrderItem table, sharing every value except the primary key.
 *
 * The expected outcome is that exactly one OrderItem row exists per
 * {@code save} / {@code add-and-merge} call, regardless of the path used
 * (repository {@code save}, parent collection + cascade, or merge).
 */
@RunWith(Arquillian.class)
public class OrderItemDuplicationTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "order-item-duplication-test.war")
                .addPackages(true, "fish.payara.samples.data.entity")
                .addPackages(true, "fish.payara.samples.data.repo")
                .addAsResource("META-INF/persistence.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @PersistenceContext(unitName = "samples-dataPU")
    private EntityManager em;

    @Resource
    private UserTransaction utx;

    @Inject
    private Orders orders;

    @Inject
    private OrderItems orderItems;

    private void clearDatabase() throws Exception {
        utx.begin();
        em.createQuery("DELETE FROM OrderItem").executeUpdate();
        em.createQuery("DELETE FROM Order").executeUpdate();
        utx.commit();
    }

    private UUID createOrder(String customerName) throws Exception {
        utx.begin();
        Order parent = Order.of(UUID.randomUUID(), customerName);
        orders.save(parent);
        utx.commit();
        return parent.id;
    }

    private long countItemsForOrder(UUID orderId) {
        return em.createQuery(
                        "SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId",
                        Long.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
    }

    private List<OrderItem> findItemsForOrder(UUID orderId) {
        return em.createQuery(
                        "SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId",
                        OrderItem.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    @After
    public void teardown() throws Exception {
        try {
            if (utx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                utx.rollback();
            }
        } catch (Exception ignore) {
            // best-effort cleanup
        }
    }

    /**
     * Saves a single OrderItem through the Jakarta Data repository for an
     * already persisted Order. Exactly one OrderItem row must exist.
     */
    @Test
    public void singleChildSaveProducesSingleRow() throws Exception {
        clearDatabase();
        UUID orderId = createOrder("Alice");

        utx.begin();
        Order parent = orders.findById(orderId).orElseThrow();
        OrderItem item = OrderItem.of(UUID.randomUUID(), parent, "Widget", 1, new BigDecimal("9.99"));
        orderItems.save(item);
        utx.commit();

        utx.begin();
        long total = countItemsForOrder(orderId);
        List<OrderItem> persisted = findItemsForOrder(orderId);
        utx.commit();

        assertEquals("Saving 1 OrderItem must create exactly 1 row", 1L, total);
        assertEquals(1, persisted.size());
        assertNotNull(persisted.get(0).id);
        assertEquals("Widget", persisted.get(0).productName);
    }

    /**
     * Adds a single OrderItem to the parent's collection and merges the
     * parent. Cascade=ALL on the parent must not cause a second row.
     */
    @Test
    public void cascadeFromParentProducesSingleRow() throws Exception {
        clearDatabase();
        UUID orderId = createOrder("Bob");

        utx.begin();
        Order managed = em.find(Order.class, orderId);
        OrderItem item = OrderItem.of(UUID.randomUUID(), managed, "Gadget", 2, new BigDecimal("4.50"));
        managed.items.add(item);
        em.merge(managed);
        utx.commit();

        utx.begin();
        long total = countItemsForOrder(orderId);
        utx.commit();

        assertEquals("Cascading 1 OrderItem from parent must create exactly 1 row", 1L, total);
    }

    /**
     * Saves several distinct children in sequence. Each save must add exactly
     * one row; the total must equal the number of save calls.
     */
    @Test
    public void multipleSequentialChildSavesMatchExactCount() throws Exception {
        clearDatabase();
        UUID orderId = createOrder("Carol");

        final int childrenToCreate = 3;
        for (int i = 0; i < childrenToCreate; i++) {
            utx.begin();
            Order parent = orders.findById(orderId).orElseThrow();
            OrderItem item = OrderItem.of(UUID.randomUUID(), parent, "Item-" + i, 1, new BigDecimal("1.00"));
            orderItems.save(item);
            utx.commit();
        }

        utx.begin();
        long total = countItemsForOrder(orderId);
        utx.commit();

        assertEquals("Each save must create exactly 1 row", (long) childrenToCreate, total);
    }
}
