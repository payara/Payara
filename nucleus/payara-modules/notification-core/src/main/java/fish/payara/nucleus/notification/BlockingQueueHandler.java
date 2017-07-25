/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.notification;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Stores log records to be retrieved elsewhere
 * @author jonathan coustick
 */
public class BlockingQueueHandler extends Handler implements BlockingQueue<LogRecord> {

    ArrayBlockingQueue<LogRecord> queue;
    
    /**
     * Creates a BlockingQueueHandler that can store 1 LogRecord
     */
    public BlockingQueueHandler(){
        super();
        queue = new ArrayBlockingQueue<LogRecord>(1);
    }
    
    /**
     * Creates a BlockingQueueHandler that can store a set amount of LogRecords
     * @param size the maximum number of LogRecords that can be stored
     */
    public BlockingQueueHandler(int size){
        super();
        queue = new ArrayBlockingQueue<LogRecord>(size);
    }
    
    @Override
    public void publish(LogRecord record) {
        queue.add(record);
    }

    /**
     * Empties the queue of all elements
     */
    @Override
    public void flush() {
        queue.clear();
    }

    @Override
    public void close() throws SecurityException {
        setLevel(Level.OFF);
        
    }

    @Override
    public boolean add(LogRecord e) {
        return queue.add(e);
    }

    @Override
    public boolean offer(LogRecord e) {
        return queue.offer(e);
    }

    @Override
    public void put(LogRecord e) throws InterruptedException {
        queue.put(e);
        
    }

    @Override
    public boolean offer(LogRecord e, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(e, timeout, unit);
    }

    @Override
    public LogRecord take() throws InterruptedException {
        return queue.take();
    }
    
    @Override
    public LogRecord poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof LogRecord){
            return queue.contains(o);
        } else {
            return false;
        }
    }
    
    public boolean contains (LogRecord lr){
        return queue.contains(lr);
    }

    @Override
    public int drainTo(Collection<? super LogRecord> c) {
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super LogRecord> c, int maxElements) {
        return queue.drainTo(c, maxElements);
    }

    @Override
    public LogRecord remove() {
        return queue.remove();
    }

    @Override
    public LogRecord poll() {
        return queue.poll();
    }

    @Override
    public LogRecord element() {
        return queue.element();
    }

    @Override
    public LogRecord peek() {
        return queue.peek();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public Iterator<LogRecord> iterator() {
        return queue.iterator();
    }

    @Override
    public LogRecord[] toArray() {
        return (LogRecord[]) queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends LogRecord> c) {
        return queue.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public void clear() {
        queue.clear();
    }
    
}
