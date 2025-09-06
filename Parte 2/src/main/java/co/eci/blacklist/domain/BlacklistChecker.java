package co.eci.blacklist.domain;

import co.eci.blacklist.infrastructure.HostBlackListsDataSourceFacade;
import co.eci.blacklist.labs.part2.ThreadLifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.lang.Thread;

public class BlacklistChecker {

    private static final Logger logger = Logger.getLogger(BlacklistChecker.class.getName());

    private final HostBlackListsDataSourceFacade facade;
    private final Policies policies;

    public BlacklistChecker(HostBlackListsDataSourceFacade facade, Policies policies) {
        this.facade = Objects.requireNonNull(facade);
        this.policies = Objects.requireNonNull(policies);
    }

    public MatchResult checkHost(String ip, int nThreads) throws InterruptedException {
        final long start = System.currentTimeMillis();
        final int totalServers = facade.getRegisteredServersCount();
        final int threads = Math.max(1, Math.min(nThreads, totalServers));

        // BLACK_LIST_ALARM_COUNT Viene de policies.
        final int threshold = Math.max(1, policies.getAlarmCount());
        // Thread Safe
        final AtomicInteger found = new AtomicInteger(0);
        final AtomicInteger checked = new AtomicInteger(0);  
        final AtomicBoolean stop = new AtomicBoolean(false); 
        final List<Integer> matches = Collections.synchronizedList(new ArrayList<>());

        final int chunk = (int) Math.ceil(totalServers / (double) threads);

        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads;t++){
            final int startIndex = t * chunk;
            final int endIndex = Math.min(totalServers, startIndex + chunk);
            if (startIndex >= endIndex){
                workers[t] = null;
                continue;
            }
            workers[t] = new Thread(() -> {
                for (int i = startIndex; i < endIndex && !stop.get(); i++){
                    boolean hit = facade.isInBlackListServer(i, ip);
                    checked.incrementAndGet();

                    if (hit) {
                        int c = found.incrementAndGet();
                        matches.add(i);
                        if (c >= threshold) {  // Parada temprana
                            stop.set(true); // Señal de parada
                            break;  // Se detiene el hilo
                        }
                    }
                }
            }, "Blacklist-Worker" + t);
            workers[t].start();
        }

        for (Thread w: workers) if (w != null) w.join();

        boolean trustworthy = found.get() < threshold;

        logger.info("Checked Blacklists :" +checked.get() + " of " + totalServers);
        if (trustworthy) {
            facade.reportAsTrustworthy(ip);
        } else {
            facade.reportAsNotTrustworthy(ip);
        }

        long elapsed = System.currentTimeMillis() - start;

        return new MatchResult(
                ip,
                trustworthy,
                List.copyOf(matches),
                checked.get(),
                totalServers,
                elapsed,
                threads
        );
    }
}


          
        

        


