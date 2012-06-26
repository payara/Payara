package org.glassfish.contextpropagation.adaptors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.glassfish.contextpropagation.bootstrap.LoggerAdapter;

public class RecordingLoggerAdapter implements LoggerAdapter {
  Level lastLevel;
  MessageID lastMessageID;
  Object[] lastArgs;
  Throwable lastThrowable;

  @Override
  public boolean isLoggable(Level level) {
    return true;
  }

  @Override
  public void log(Level level, MessageID messageID, Object... args) {
    lastLevel = level;
    lastMessageID = messageID;
    lastArgs = args;
    lastThrowable = null;
  }

  @Override
  public void log(Level level, Throwable t, MessageID messageID,
      Object... args) {
    lastLevel = level;
    lastMessageID = messageID;
    lastArgs = args;
    lastThrowable = t;
  }

  public void verify(Level level, Throwable t, MessageID messageID,
      Object... args) {
    assertEquals(lastLevel, level);
    assertEquals(lastThrowable, t);
    assertEquals(lastMessageID, messageID);
    assertArrayEquals(lastArgs, args);
    lastLevel = null;
    lastThrowable = null;
    lastMessageID = null;
    lastArgs = null;
  }

}
