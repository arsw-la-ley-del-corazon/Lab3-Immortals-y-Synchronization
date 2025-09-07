package edu.eci.arsw.demos;

import java.util.concurrent.Executors;

import edu.eci.arsw.core.BankAccount;
import edu.eci.arsw.core.TransferService;

/**
 * Demo that intentionally runs a naive transfer from A->B and B->A to produce a
 * deadlock scenario for observation purposes.
 */
public final class DeadlockDemo {
  private DeadlockDemo() {
  }

  /**
   * Runs the deadlock demo for a short period.
   *
   * @throws Exception if interrupted
   */
  public static void run() throws Exception {
    var a = new BankAccount(1, 1000);
    var b = new BankAccount(2, 1000);
    try (var exec = Executors.newFixedThreadPool(2)) {
      exec.submit(() -> TransferService.transferNaive(a, b, 10));
      exec.submit(() -> TransferService.transferNaive(b, a, 10));
      System.out.println("Running DeadlockDemo: observe threads waiting on each other (Ctrl+C to stop).");
      Thread.sleep(30_000);
    }
  }
}
