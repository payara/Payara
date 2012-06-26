package org.glassfish.contextpropagation.adaptors;

import org.glassfish.contextpropagation.bootstrap.LoggerAdapter;
import org.junit.Test;

public class MockLoggerAdapter implements LoggerAdapter {
  // TODO TIP: Change the Level constant to control what is logged, use null to reduce output to a minimum
  static final Level LOGGING_LEVEL = null; // Level.WARN; 

  @Override
  public boolean isLoggable(Level level) {
    return _isLoggable(level);
  }

  @Override
  public void log(Level level, MessageID messageID, Object... args) {
    System.out.println(format(messageID.defaultMessage, args));

  }

  private String format(String defaultMessage, Object... args) {
    String formatString = defaultMessage.replaceAll("%([0-9]*)", "%$1\\$s"); // $1 refers to the group %1 is equivalent to %1$s
    return String.format(formatString, args);
  }

  @Override
  public void log(Level level, Throwable t, MessageID messageID, Object... args) {
    log(level, messageID, args);
    t.printStackTrace();
  }
  
  @Test
  public void testFormat() {
    debug(format("arg 1:%1, arg2: %2", "one", "two"));
  }
  
  private static boolean _isLoggable(Level level) {
    return LOGGING_LEVEL != null && level.ordinal() <= LOGGING_LEVEL.ordinal();
  }
  
  public static void debug(String s) {
    if (_isLoggable(Level.DEBUG)) System.out.println(s);
  }

}
