package co.eci.blacklist.labs.part1;

public class CountMainThreads {
    public static void main(String[] args) throws InterruptedException {
        CountThread t1 = new CountThread(0, 99);
        CountThread t2 = new CountThread(99, 199);
        CountThread t3 = new CountThread(200, 299);

        t1.run();
        t2.run();
        t3.run();

        t1.join();
        t2.join();
        t3.join();
    }
}
