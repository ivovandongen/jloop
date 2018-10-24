package nl.ivovandongen.jloop;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The Loop interface exposes methods.
 */
public interface Loop {

    /**
     * Post a message to be processed on this {@link Looper}
     * <p>
     * May be called from any thread.
     *
     * @param runnable the runnable to process
     */
    void post(Runnable runnable);

    /**
     * Post a delayed task. Get's executed (approximately) after the given delay.
     * <p>
     * Can't be guaranteed to be executed exactly at the given time as previous tasks
     * might overlap with the requested time.
     *
     * @param runnable the runnable to execute
     * @param delay    the delay. If null or <= 0, scheduled immediately
     * @param timeUnit the time unit of the delay. Use {@link TimeUnit#MILLISECONDS} as the most precise.
     *                 If null, scheduled immediately
     */
    void postDelayed(Runnable runnable, Long delay, TimeUnit timeUnit);

    /**
     * Post a message to be processed and get a Future to deal with the result.
     *
     * @param callable the callable to process
     * @param <T>      the result type
     * @return the future that will hold the result
     */
    <T> CompletableFuture<T> ask(Callable<T> callable);
}
