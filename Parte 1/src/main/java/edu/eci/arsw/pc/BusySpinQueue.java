package edu.eci.arsw.pc;

import java.util.LinkedList;
import java.util.Queue;

public class BusySpinQueue<T> {
    private final Queue<T> queue;
    private final int capacity;

    public BusySpinQueue(int capacity) {
        this.queue = new LinkedList<>();
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait(); //// mientras el buffer está lleno
        }
        queue.add(item);
        notifyAll(); // notifica consumidores
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // mientras el buffer está vacío
        }
        T item = queue.poll();
        notifyAll(); // notifica productores
        return item;
    }
}