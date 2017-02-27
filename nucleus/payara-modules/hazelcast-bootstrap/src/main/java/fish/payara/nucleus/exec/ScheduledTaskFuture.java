/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.exec;

import com.hazelcast.scheduledexecutor.IScheduledFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Steve Millidge <Payara Services Limited>
 * @param <V> Type of the object
 */
public class ScheduledTaskFuture<V extends Object> implements Future {
    
    private IScheduledFuture<V> wrappee;
    
    public ScheduledTaskFuture(IScheduledFuture<V> future) {
        wrappee = future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return wrappee.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return wrappee.isCancelled();
    }

    @Override
    public boolean isDone() {
        return wrappee.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        Object result = wrappee.get();
        // as we got the result dispose of the IScheduledFuture
        wrappee.dispose();
        return result;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Object result = wrappee.get();
        // as we got the result dispose of the IScheduledFuture
        wrappee.dispose();
        return result;
    }


    
}
