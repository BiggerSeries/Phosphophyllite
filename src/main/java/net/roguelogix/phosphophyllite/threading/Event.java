package net.roguelogix.phosphophyllite.threading;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Event {
    private final ArrayList<Runnable> callbacks = new ArrayList<>();
    private volatile boolean wasTriggered = false;
    
    public boolean ready() {
        return wasTriggered;
    }
    
    @SuppressWarnings("unused")
    public synchronized void join() {
        if (wasTriggered) {
            return;
        }
        try {
            wait();
        } catch (InterruptedException ignored) {
        }
    }
    
    @SuppressWarnings("unused")
    public synchronized boolean join(int timeout) {
        if (wasTriggered) {
            return true;
        }
        try {
            wait(timeout);
        } catch (InterruptedException ignored) {
        }
        
        return wasTriggered;
    }
    
    public synchronized void trigger() {
        if (wasTriggered) {
            return;
        }
        wasTriggered = true;
        callbacks.forEach(Runnable::run);
        notifyAll();
    }
    
    public synchronized void registerCallback(Runnable runnable) {
        if (wasTriggered) {
            runnable.run();
            return;
        }
        callbacks.add(runnable);
    }
    
    @Override
    protected void finalize() {
        trigger();
    }
}
