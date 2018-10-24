package nl.ivovandongen.jloop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that combines a {@link Thread} with a {@link Loop} and life-time methods.
 * Useful to create a looper on a separate thread.
 * Ensures after {@link LooperThread#start()} that the {@link Loop} is running.
 */
public class LooperThread implements Loop {
    private static final Logger LOGGER = LoggerFactory.getLogger(LooperThread.class);
    private Looper looper;

    /**
     * Start the loop. Blocks until the loop is running
     */
    public void start() {
        assert (looper == null);

        StartupLatch startupLatch = new StartupLatch();
        new Thread(() -> {
            looper = Looper.prepare();
            looper.post(startupLatch::started);
            looper.run();
        }).start();
        startupLatch.await();
    }

    /**
     * Stop the loop. Will not block.
     */
    public void stop() {
        if (looper != null) {
            looper.shutdown();
        }
        looper = null;
    }

    @Override
    public void post(final Runnable runnable) {
        ensureLooper();
        looper.post(runnable);
    }

    @Override
    public void postDelayed(final Runnable runnable, final Long delay, final TimeUnit timeUnit) {
        ensureLooper();
        looper.postDelayed(runnable, delay, timeUnit);
    }

    @Override
    public <T> CompletableFuture<T> ask(final Callable<T> callable) {
        ensureLooper();
        return looper.ask(callable);
    }

    private void ensureLooper() {
        if (looper == null) {
            LOGGER.error("No running looper");
            throw new IllegalStateException("No running looper");
        }
    }
}
