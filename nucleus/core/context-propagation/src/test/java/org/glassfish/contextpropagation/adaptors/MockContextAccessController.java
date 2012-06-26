package org.glassfish.contextpropagation.adaptors;

import org.glassfish.contextpropagation.bootstrap.ContextAccessController;
import org.glassfish.contextpropagation.internal.AccessControlledMap;
import org.glassfish.contextpropagation.internal.AccessControlledMap.ContextAccessLevel;

public class MockContextAccessController extends
    ContextAccessController {

  @Override
  public boolean isAccessAllowed(String key, AccessControlledMap.ContextAccessLevel type) {
    if (type == ContextAccessLevel.READ && isEveryoneAllowedToRead(key)) {
      return true; // First do a quick check for read access
    }
    return true;
  }

  @Override
  public boolean isEveryoneAllowedToRead(String key) {
    return false;
  }

}
