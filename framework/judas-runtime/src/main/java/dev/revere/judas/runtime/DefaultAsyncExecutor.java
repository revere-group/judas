package dev.revere.judas.runtime;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared daemon thread-pool executor for async command handlers.
 */
public final class DefaultAsyncExecutor implements Executor {
    private final ExecutorService delegate;

    public DefaultAsyncExecutor() {
        this.delegate = Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "judas-async-" + this.counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Override
    public void execute(Runnable command) {
        this.delegate.execute(command);
    }
}
