package edu.eci.arsw.core;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility service providing several strategies to transfer money between
 * {@link BankAccount} instances. Methods demonstrate naive (deadlock-prone),
 * ordered (deadlock-free) and tryLock-based transfer strategies.
 */
public final class TransferService {
  /**
   * Naive transfer that locks source then destination without ordering. This can
   * deadlock under certain interleavings.
   *
   * @param from source account
   * @param to destination account
   * @param amount amount to transfer
   */
  public static void transferNaive(BankAccount from, BankAccount to, long amount) {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    var a = from.lock();
    var b = to.lock();
    a.lock();
    try {
      sleepALittle();
      b.lock();
      try {
        withdrawDeposit(from, to, amount);
      } finally {
        b.unlock();
      }
    } finally {
      a.unlock();
    }
  }

  /**
   * Ordered transfer that locks accounts by their id ordering to prevent
   * deadlocks.
   *
   * @param from source account
   * @param to destination account
   * @param amount amount to transfer
   */
  public static void transferOrdered(BankAccount from, BankAccount to, long amount) {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    BankAccount first = from.id() < to.id() ? from : to;
    BankAccount second = from.id() < to.id() ? to : from;
    first.lock().lock();
    try {
      second.lock().lock();
      try {
        withdrawDeposit(from, to, amount);
      } finally {
        second.lock().unlock();
      }
    } finally {
      first.lock().unlock();
    }
  }

  /**
   * Tries to acquire locks using tryLock with a timeout and retries until the
   * provided deadline. Throws InterruptedException if the deadline is reached
   * before acquiring both locks.
   *
   * @param from source account
   * @param to destination account
   * @param amount amount to transfer
   * @param maxWait maximum duration to wait for acquiring locks
   * @throws InterruptedException if the transfer could not acquire locks in time
   */
  public static void transferTryLock(BankAccount from, BankAccount to, long amount, Duration maxWait)
      throws InterruptedException {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    ReentrantLock a = from.lock();
    ReentrantLock b = to.lock();
    long deadline = System.nanoTime() + maxWait.toNanos();
    while (System.nanoTime() < deadline) {
      if (a.tryLock(10, TimeUnit.MILLISECONDS)) {
        try {
          if (b.tryLock(10, TimeUnit.MILLISECONDS)) {
            try {
              withdrawDeposit(from, to, amount);
              return;
            } finally {
              b.unlock();
            }
          }
        } finally {
          a.unlock();
        }
      }
      Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
    }
    throw new InterruptedException("transferTryLock timed out");
  }

  /**
   * Internal helper that performs the withdraw and deposit assuming locks are
   * already held.
   */
  private static void withdrawDeposit(BankAccount from, BankAccount to, long amount) {
    if (from.balance() < amount)
      throw new IllegalArgumentException("Insufficient funds");
    from.withdrawInternal(amount);
    to.depositInternal(amount);
  }

  private static void sleepALittle() {
    try {
      Thread.sleep(5);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}
