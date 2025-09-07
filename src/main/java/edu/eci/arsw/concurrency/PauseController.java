package edu.eci.arsw.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple pause/resume controller that allows threads to await while the
 * controller is paused. Uses a ReentrantLock and Condition for correct
 * synchronization and interruptibility.
 */
public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  // Condition to signal changes in the number of currently paused threads.
  private final Condition pausedCountChanged = lock.newCondition();
  private volatile boolean paused = false;
  // number of threads currently blocked in awaitIfPaused()
  private int pausedThreads = 0;

  /**
   * Puts the controller into paused state. Threads calling
   * {@link #awaitIfPaused()} will block until {@link #resume()} is called.
   */
  public void pause() {
    lock.lock();
    try {
      paused = true;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Resumes execution and signals waiting threads.
   */
  public void resume() {
    lock.lock();
    try {
      paused = false;
      unpaused.signalAll();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns whether the controller is currently paused.
   *
   * @return true when paused
   */
  public boolean paused() {
    return paused;
  }

  /**
   * Causes the calling thread to wait while the controller is paused.
   *
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public void awaitIfPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (paused) {
        // register as paused and notify any waiter interested on paused count
        pausedThreads++;
        try {
          pausedCountChanged.signalAll();
          unpaused.await();
        } finally {
          // on resume (or interruption) unregister and notify
          pausedThreads--;
          pausedCountChanged.signalAll();
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Waits until at least {@code expected} threads are paused or the timeout
   * elapses. Returns true if the expected number was reached, false if timed out.
   */
  public boolean waitForAllPaused(int expected, long timeoutMillis) throws InterruptedException {
    long nanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    lock.lockInterruptibly();
    try {
      while (pausedThreads < expected) {
        if (nanos <= 0)
          return false;
        nanos = pausedCountChanged.awaitNanos(nanos);
      }
      return true;
    } finally {
      lock.unlock();
    }
  }

  /** Returns the current number of threads known to be paused. */
  public int pausedCount() {
    lock.lock();
    try {
      return pausedThreads;
    } finally {
      lock.unlock();
    }
  }
}
