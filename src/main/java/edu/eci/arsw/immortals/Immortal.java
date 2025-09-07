package edu.eci.arsw.immortals;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import edu.eci.arsw.concurrency.PauseController;

/**
 * Represents a single immortal participant in the Highlander simulation.
 * <p>
 * An Immortal fights with other immortals repeatedly, decreasing opponents' health
 * and increasing its own. The class is safe for concurrent use by its own
 * thread and external controllers (pause/stop) but relies on external
 * synchronization when comparing multiple immortals in fights.
 */
public final class Immortal implements Runnable {
  private final String name;
  private int health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;
  private volatile boolean running = true;

  /**
   * Constructs a new Immortal.
   *
   * @param name the unique name of the immortal, must not be null
   * @param health initial health value
   * @param damage damage value applied to opponents
   * @param population reference to the shared population list, must not be null
   * @param scoreBoard shared scoreboard used to record fights, must not be null
   * @param controller pause controller used to suspend/resume execution, must not be null
   */
  public Immortal(String name, int health, int damage, List<Immortal> population, ScoreBoard scoreBoard,
      PauseController controller) {
    this.name = Objects.requireNonNull(name);
    this.health = health;
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
  }

  /**
   * Returns this immortal's name.
   *
   * @return the immortal name
   */
  public String name() {
    return name;
  }

  /**
   * Returns the current health of the immortal.
   *
   * @return current health value
   */
  public synchronized int getHealth() {
    return health;
  }

  /**
   * Checks whether the immortal is alive and still running.
   *
   * @return true if alive and running, false otherwise
   */
  public boolean isAlive() {
    return getHealth() > 0 && running;
  }

  /**
   * Requests the immortal's thread to stop at the next convenient point.
   */
  public void stop() {
    running = false;
  }

  @Override
  /**
   * Main loop executed by the immortal's thread.
   * <p>
   * The loop picks opponents and performs fights according to the configured
   * fight mode; it cooperates with the {@link PauseController} to pause/resume.
   */
  public void run() {
    try {
      while (running) {
        controller.awaitIfPaused();
        if (!running)
          break;
        var opponent = pickOpponent();
        if (opponent == null)
          continue;
        String mode = System.getProperty("fight", "ordered");
        if ("naive".equalsIgnoreCase(mode))
          fightNaive(opponent);
        else
          fightOrdered(opponent);
        Thread.sleep(2);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Picks a random opponent from the population that is not this immortal.
   *
   * @return an opponent Immortal, or null if no opponent is available
   */
  private Immortal pickOpponent() {
    if (population.size() <= 1)
      return null;
    Immortal other;
    do {
      other = population.get(ThreadLocalRandom.current().nextInt(population.size()));
    } while (other == this);
    return other;
  }

  /**
   * Performs a naive (nested) fight acquiring locks in no particular order.
   *
   * @param other opponent immortal
   */
  private void fightNaive(Immortal other) {
    synchronized (this) {
      synchronized (other) {
        if (this.health <= 0 || other.health <= 0)
          return;
        other.health -= this.damage;
        this.health += this.damage / 2;
        scoreBoard.recordFight();
      }
    }
  }

  /**
   * Performs an ordered fight acquiring locks according to immortal names to
   * avoid deadlocks.
   *
   * @param other opponent immortal
   */
  private void fightOrdered(Immortal other) {
    Immortal first = this.name.compareTo(other.name) < 0 ? this : other;
    Immortal second = this.name.compareTo(other.name) < 0 ? other : this;
    synchronized (first) {
      synchronized (second) {
        if (this.health <= 0 || other.health <= 0)
          return;
        other.health -= this.damage;
        this.health += this.damage / 2;
        scoreBoard.recordFight();
      }
    }
  }
}
