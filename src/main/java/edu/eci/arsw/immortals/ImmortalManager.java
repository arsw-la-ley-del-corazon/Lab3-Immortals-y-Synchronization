package edu.eci.arsw.immortals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.eci.arsw.concurrency.PauseController;

/**
 * Manages the lifecycle and coordination of a population of {@link Immortal}
 * instances. Provides start/pause/resume/stop operations and exposes read-only
 * snapshots and aggregated information such as total health.
 */
public final class ImmortalManager implements AutoCloseable {
  // Use CopyOnWriteArrayList to allow safe iteration during snapshots and
  // infrequent structural modifications (removing dead immortals).
  private final CopyOnWriteArrayList<Immortal> population = new CopyOnWriteArrayList<>();
  private final List<Future<?>> futures = new ArrayList<>();
  private final PauseController controller = new PauseController();
  private final ScoreBoard scoreBoard = new ScoreBoard();
  private ExecutorService exec;
  private ScheduledExecutorService cleaner;

  private final String fightMode;
  private final int initialHealth;
  private final int damage;

  /**
   * Constructs a manager that creates {@code n} immortals with default health
   * and damage read from system properties.
   *
   * @param n number of immortals to create
   * @param fightMode fight mode string passed to immortals
   */
  public ImmortalManager(int n, String fightMode) {
    this(n, fightMode, Integer.getInteger("health", 100), Integer.getInteger("damage", 10));
  }

  /**
   * Constructs a manager with explicit parameters.
   *
   * @param n number of immortals
   * @param fightMode fight mode for immortals (e.g. "ordered"|"naive")
   * @param initialHealth initial health for each immortal
   * @param damage damage per fight
   */
  public ImmortalManager(int n, String fightMode, int initialHealth, int damage) {
    this.fightMode = fightMode;
    this.initialHealth = initialHealth;
    this.damage = damage;
    for (int i = 0; i < n; i++) {
  population.add(new Immortal("Immortal-" + i, initialHealth, damage, population, scoreBoard, controller));
    }
  }

  /** Returns the configured initial health for each immortal. */
  public int initialHealth() {
    return initialHealth;
  }

  /** Returns the current configured initial population size at construction. */
  public int initialCount() {
    return population.size();
  }

  /**
   * Starts the simulation by submitting each immortal to a virtual-thread
   * executor. If a previous executor exists it will be stopped first.
   */
  public synchronized void start() {
    if (exec != null)
      stop();
    exec = Executors.newVirtualThreadPerTaskExecutor();
    futures.clear();
    for (Immortal im : population) {
      futures.add(exec.submit(im));
    }
    // Start a light-weight scheduled cleaner to remove dead immortals periodically.
    if (cleaner == null || cleaner.isShutdown()) {
      cleaner = new ScheduledThreadPoolExecutor(1);
      cleaner.scheduleAtFixedRate(() -> {
        try {
          population.removeIf(im -> !im.isAlive());
        } catch (Throwable t) {
          // ignore and continue
        }
      }, 500, 500, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Pauses the simulation; running immortals will block on the
   * {@link edu.eci.arsw.concurrency.PauseController}.
   */
  public void pause() {
    controller.pause();
  }

  /**
   * Resumes the simulation after a pause.
   */
  public void resume() {
    controller.resume();
  }

  /**
   * Requests all immortals to stop and shuts down the executor.
   */
  public void stop() {
    for (Immortal im : population)
      im.stop();
    if (exec != null) {
      exec.shutdownNow();
      try {
        exec.awaitTermination(2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      exec = null;
    }
    futures.clear();
    if (cleaner != null) {
      cleaner.shutdownNow();
      try {
        cleaner.awaitTermination(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      cleaner = null;
    }
  }

  /**
   * Counts immortals that are considered alive.
   *
   * @return number of alive immortals
   */
  public int aliveCount() {
    int c = 0;
    for (Immortal im : population)
      if (im.isAlive())
        c++;
    return c;
  }

  /**
   * Returns the aggregate health across the population.
   *
   * @return total health
   */
  public long totalHealth() {
    long sum = 0;
    for (Immortal im : population)
      sum += im.getHealth();
    return sum;
  }

  /**
   * Returns an unmodifiable snapshot of the population list.
   *
   * @return read-only snapshot list of immortals
   */
  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  /**
   * Returns the shared scoreboard instance.
   *
   * @return scoreboard
   */
  public ScoreBoard scoreBoard() {
    return scoreBoard;
  }

  /**
   * Returns the pause controller used by immortals.
   *
   * @return pause controller
   */
  public PauseController controller() {
    return controller;
  }

  @Override
  public void close() {
    stop();
  }
}
