package nl.ivovandongen.jloop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link Looper} enables a message {@link Loop} on the current thread or (may be a main run loop that can
 * be used application wide).
 *
 */
public class Looper implements Loop {
    private static final Logger LOGGER = LoggerFactory.getLogger(Looper.class);
    private static final int MAX_BACKLOG = 5;

    // Static state

    private static ThreadLocal<Looper> current = new ThreadLocal<>();
    private static ThreadLocal<Long> threadId = new ThreadLocal<>();
    private static AtomicReference<Looper> main = new AtomicReference<>();

    /**
     * @return the current looper for the current thread if {@link Looper#prepare()} was called.
     */
    public static Looper get() {
        return current.get();
    }

    /**
     * Prepares a {@link Looper} for the current thread.
     *
     * @return the {@link Looper}
     */
    public static synchronized Looper prepare() {
        if (current.get() != null) {
            throw new IllegalStateException("Looper already prepared for thread");
        }

        Looper looper = new Looper();
        current.set(looper);
        threadId.set(Thread.currentThread().getId());
        return looper;
    }

    /**
     * @return the Main {@link Looper} if set
     */
    public static Looper getMainLooper() {
        return main.get();
    }

    /**
     * Prepares a {@link Looper} on the current thread and sets it as the main {@link Looper}.
     *
     * @return the {@link Looper}
     */
    public static synchronized Looper prepareMainLooper() {
        if (main.get() != null) {
            throw new IllegalStateException("Main looper already prepared");
        }

        Looper looper = prepare();
        main.set(looper);
        return looper;
    }

    // Instance state

    private boolean running = true;
    private final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private final ConcurrentSkipListMap<Long, Message> delayedMessages = new ConcurrentSkipListMap<>();

    /**
     * Blocks until either {@link Looper#shutdown()} is called or the {@link Thread} is
     * interrupted.
     * <p>
     * May only be called on the Thread where it was created.
     */
    public void run() {
        assertCorrectThread();

        try {
            Message message;
            while (running) {
                if (delayedMessages.isEmpty()) {
                    // Blocking wait on a message as we have nothing to do
                    message = messages.take();
                } else {
                    // Check if any messages are already due
                    Map.Entry<Long, Message> first = delayedMessages.firstEntry();
                    long timeMs = System.currentTimeMillis();

                    // Message is not due yet, block till it is
                    if (first.getKey() > timeMs) {
                        message = messages.poll(first.getKey() - timeMs, TimeUnit.MILLISECONDS);
                        // If we timed out, go to next iteration
                        if (message == null) {
                            continue;
                        }
                    } else {
                        // Message is due, handle it
                        message = first.getValue();
                        delayedMessages.remove(first.getKey());
                    }
                }

                try {
                    message.execute();
                } catch (Exception e) {
                    LOGGER.error("Could not execute message", e);
                    halt();
                }

                if (messages.size() > MAX_BACKLOG) {
                    LOGGER.warn("Loop backlogged. %s messages in queue", messages.size());
                }
            }
        } catch (InterruptedException e) {
            LOGGER.info("Loop interrupted");
        }
        LOGGER.info("Loop stopping");
    }

    /**
     * Processes at most 1 message (if any messages are queued) and returns.
     * May only be called on the Thread where it was created.
     */
    public void runOnce() {
        assertCorrectThread();

        Message message = messages.poll();
        if (message != null) {
            message.execute();
        }
    }

    @Override
    public void post(final Runnable runnable) {
        messages.add(new RunnableMessage(runnable));
    }

    @Override
    public void postDelayed(final Runnable runnable, final Long delay, final TimeUnit timeUnit) {
        if (delay == null || delay <= 0 || timeUnit == null) {
            post(runnable);
            return;
        }

        delayedMessages.put(
                System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delay, timeUnit),
                new RunnableMessage(runnable)
        );

        // Wake up the loop to make sure it is not blocking until a new message arrives
        messages.add(new RunnableMessage(() -> {
            //NOOP
        }));
    }

    @Override
    public <T> CompletableFuture<T> ask(final Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        messages.add(new CallableMessage<T>(callable, future));
        return future;
    }

    /**
     * Shuts down the looper (if running).
     * <p>
     * May be called from any thread.
     */
    public void shutdown() {
        messages.add(new ShutdownMessage(this));
    }

    /**
     * Shuts down the looper immediately.
     * <p>
     * Only callable from {@link Message}
     */
    void halt() {
        assertCorrectThread();
        running = false;
    }

    // For testing purposes
    static void reset() {
        main.set(null);
        current.set(null);
    }

    private void assertCorrectThread() {
        if (Thread.currentThread().getId() != threadId.get()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Loop was prepared for Thread %s and cannot be started on Thread %s",
                            threadId.get(),
                            Thread.currentThread().getId()
                    )
            );
        }
    }
}
