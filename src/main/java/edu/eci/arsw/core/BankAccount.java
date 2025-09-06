package edu.eci.arsw.core;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple bank account abstraction used by the transfer demos and services.
 * <p>
 * Exposes an explicit {@link ReentrantLock} for external coordination.
 */
public final class BankAccount {
  private final long id;
  private long balance;
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Creates a bank account with an id and an initial balance.
   *
   * @param id account identifier
   * @param initial initial balance
   */
  public BankAccount(long id, long initial) {
    this.id = id;
    this.balance = initial;
  }

  /**
   * Returns the account id.
   *
   * @return account id
   */
  public long id() {
    return id;
  }

  /**
   * Returns the current balance. Note: not synchronized; callers should ensure
   * appropriate locking if required.
   *
   * @return balance
   */
  public long balance() {
    return balance;
  }

  /**
   * Returns the account lock for external coordination.
   *
   * @return account lock
   */
  public ReentrantLock lock() {
    return lock;
  }

  // Internal helpers used by TransferService while holding locks
  void depositInternal(long amount) {
    balance += amount;
  }

  void withdrawInternal(long amount) {
    balance -= amount;
  }
}
