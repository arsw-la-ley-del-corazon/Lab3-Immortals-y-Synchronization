package edu.eci.arsw.immortals;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Smoke test that verifies the ImmortalManager lifecycle methods run without
 * throwing exceptions and that a positive total health is observed after a
 * short execution.
 */
final class ManagerSmokeTest {
  @Test void startsAndStops() throws Exception {
    var m = new ImmortalManager(8, "ordered", 100, 10);
    m.start();
    Thread.sleep(50);
    m.pause();
    long sum = m.totalHealth();
    m.resume();
    m.stop();
    assertTrue(sum > 0);
  }
}
