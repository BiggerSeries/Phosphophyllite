package net.roguelogix.phosphophyllite.threading;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Event {
    private final ArrayList<Runnable> callbacks = new ArrayList<>();
    private final AtomicBoolean wasTriggered = new AtomicBoolean(false);
    
    public boolean ready() {
        return wasTriggered.get();
    }
    
    @SuppressWarnings("unused")
    public synchronized void join() {
        if (wasTriggered.get()) {
            return;
        }
        try {
            wait();
        } catch (InterruptedException ignored) {
        }
    }
    
    @SuppressWarnings("unused")
    public synchronized boolean join(int timeout) {
        if (wasTriggered.get()) {
            return true;
        }
        try {
            wait(timeout);
        } catch (InterruptedException ignored) {
        }
        
        return wasTriggered.get();
    }
    
    public synchronized void trigger() {
        if (wasTriggered.get()) {
            return;
        }
        wasTriggered.set(true);
        callbacks.forEach(Runnable::run);
        notifyAll();
    }
    
    public synchronized void registerCallback(Runnable runnable) {
        if (wasTriggered.get()) {
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
