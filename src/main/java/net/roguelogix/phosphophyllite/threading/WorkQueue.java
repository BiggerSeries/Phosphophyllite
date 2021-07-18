package net.roguelogix.phosphophyllite.threading;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
public class WorkQueue {
    private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
    
    private final ArrayList<DequeueThread> dequeueThreads = new ArrayList<>();
    
    public WorkQueue() {
        // workaround for FML issue
        // triggers the classloading here instead of the finalizer
        dequeueThreads.forEach(DequeueThread::finish);
    }
    
    private static class DequeueThread implements Runnable {
        private final WeakReference<Queue<Runnable>> queue;
        private final AtomicBoolean stop = new AtomicBoolean(false);
        
        public DequeueThread(Queue<Runnable> queue, String name) {
            this.queue = new WeakReference<>(queue);
            Thread thread = new Thread(this);
            if(name != null){
                thread.setName(name);
            }
            thread.setDaemon(true); // just, because, shouldn't be necessary, but just because
            thread.start();
        }
        
        public void run() {
            while (!stop.get()) {
                Queue<Runnable> queue = this.queue.get();
                if (queue == null) {
                    return;
                }
                Runnable nextItem = queue.poll();
                if (nextItem == null) {
                    synchronized (queue) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                    continue;
                }
                try {
                    nextItem.run();
                } catch (Throwable ignored) {
                }
            }
            
        }
        
        public void finish() {
            stop.set(true);
        }
    }
    
    public WorkQueue addProcessingThread() {
        return addProcessingThreads(1, null);
    }
    
    public WorkQueue addProcessingThreads(int threads) {
        return addProcessingThreads(threads, null);
    }
    
    public WorkQueue addProcessingThreads(int threads, String name) {
        for (int i = 0; i < threads; i++) {
            dequeueThreads.add(new DequeueThread(queue, name == null ? null : name + i));
        }
        return this;
    }
    
    public void finish() {
        dequeueThreads.forEach(DequeueThread::finish);
        synchronized (queue) {
            queue.notifyAll();
        }
    }
    
    @Override
    protected void finalize() {
        finish();
    }
    
    private static class WorkItem implements Runnable {
        final Event waitEvent = new Event();
        final Event readyEvent = new Event();
        final Runnable work;
        final AtomicLong unTriggeredWaitEvents = new AtomicLong(Long.MAX_VALUE);
        
        WorkItem(final Queue<Runnable> queue, final Runnable work, final Event[] waitEvents) {
            this.work = work;
            if (waitEvents.length == 0) {
                queue.add(this);
                synchronized (queue) {
                    queue.notify();
                }
                return;
            }
            unTriggeredWaitEvents.set(waitEvents.length);
            for (Event event : waitEvents) {
                if (event == null) {
                    synchronized (readyEvent) {
                        if (unTriggeredWaitEvents.decrementAndGet() == 0) {
                            readyEvent.trigger();
                        }
                    }
                }
                assert event != null;
                event.registerCallback(() -> {
                    synchronized (readyEvent) {
                        if (unTriggeredWaitEvents.decrementAndGet() == 0) {
                            readyEvent.trigger();
                        }
                    }
                });
            }
            readyEvent.registerCallback(() -> {
                queue.add(this);
                synchronized (queue) {
                    queue.notify();
                }
            });
        }
        
        @Override
        public void run() {
            try {
                work.run();
            } finally {
                waitEvent.trigger();
            }
        }
    }
    
    public Event enqueue(Runnable runnable, Set<Event> events) {
        return this.enqueue(runnable, (Event[]) events.toArray());
    }
    
    public Event enqueue(Runnable runnable, List<Event> events) {
        return this.enqueue(runnable, (Event[]) events.toArray());
    }
    
    public Event enqueue(Runnable runnable, Event... events) {
        WorkItem item = new WorkItem(queue, runnable, events);
        return item.waitEvent;
    }
    
    public void runOne() {
        if (!dequeueThreads.isEmpty()) {
            return;
        }
        Runnable toRun = queue.poll();
        if (toRun != null) {
            toRun.run();
        }
    }
    
    public void runAll() {
        if (!dequeueThreads.isEmpty()) {
            return;
        }
        Runnable toRun;
        while ((toRun = queue.poll()) != null) {
            toRun.run();
        }
    }
}
