package edu.eci.arsw.immortals;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe scoreboard that counts the total number of fights recorded in
 * the simulation.
 */
public final class ScoreBoard {
  private final AtomicLong totalFights = new AtomicLong();

  /**
   * Records a fight occurrence incrementing the internal counter.
   */
  public void recordFight() {
    totalFights.incrementAndGet();
  }

  /**
   * Returns the total number of fights recorded so far.
   *
   * @return total fights
   */
  public long totalFights() {
    return totalFights.get();
  }
}
