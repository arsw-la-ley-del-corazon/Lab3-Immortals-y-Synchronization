package edu.eci.arsw.concurrency;

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
  private volatile boolean paused = false;

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
      while (paused)
        unpaused.await();
    } finally {
      lock.unlock();
    }
  }
}
