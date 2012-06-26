package org.glassfish.contextpropagation.adaptors;
import org.junit.ComparisonFailure;
import org.junit.runner.notification.StoppedByUserException;

public abstract class TestableThread extends Thread {
  final Throwable[] throwableHolder = new Throwable[1];

  public TestableThread() { super(); }

  @SuppressWarnings("serial")
  public synchronized void startJoinAndCheckForFailures() {
    start();
    try {
      join();
    } catch (InterruptedException e) {
      throwableHolder[0] = e;
    }
    if (throwableHolder[0] != null) {
      if (throwableHolder[0] instanceof ComparisonFailure) {
        throw (ComparisonFailure) throwableHolder[0];
      } else {
        throw (StoppedByUserException) new StoppedByUserException() {
          @Override public String getMessage() {
            return throwableHolder[0].getMessage();
          }
        }.initCause(throwableHolder[0]);
      }
    }
  }

  @Override
  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      throwableHolder[0] = t;
    }
  }

  protected abstract void runTest() throws Exception;
}
