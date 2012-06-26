package org.glassfish.contextpropagation.adaptors;

import org.glassfish.contextpropagation.bootstrap.ThreadLocalAccessor;
import org.glassfish.contextpropagation.internal.AccessControlledMap;

public class MockThreadLocalAccessor implements ThreadLocalAccessor {
  private ThreadLocal<AccessControlledMap> mapThreadLocal = new ThreadLocal<AccessControlledMap>();

  @Override
  public void set(AccessControlledMap contextMap) {
    mapThreadLocal.set(contextMap);    
  }

  @Override
  public AccessControlledMap get() {
    //MockLoggerAdapter.debug("Thread: " + Thread.currentThread().getId() + " map: " + mapThreadLocal.get());
    return mapThreadLocal.get();
  }

}
