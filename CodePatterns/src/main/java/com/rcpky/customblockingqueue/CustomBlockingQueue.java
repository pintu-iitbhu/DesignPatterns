package com.rcpky.customblockingqueue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CustomBlockingQueue<T> {
    private final int capacity;
    private final Queue<T> queue= new LinkedList<>();

    // Lock for mutual exclusion
    private ReentrantLock lock = new ReentrantLock();

    // Condition variables to manage thread waiting/signaling
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();


    public CustomBlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        lock.lockInterruptibly();
        try{

            while (queue.size() == capacity) {
                System.out.println("Queue is full....waiting.....");
                notFull.await();
            }

            queue.add(item);
            System.out.println("[Producer] Added item. Size: " + queue.size());

            notEmpty.signalAll();
        }finally {
            lock.unlock();
        }

    }

    public  T take() throws InterruptedException {

        lock.lockInterruptibly();
        try{
            while (queue.size() == 0) {
                System.out.println("  [Consumer] Buffer empty. Waiting...");
                notEmpty.await();
            }
            T item = queue.remove();
            System.out.println("  [Consumer] Removed item. Size: " + queue.size());
            notFull.signalAll();
            return item;
        }finally {
            lock.unlock();
        }

    }
}
