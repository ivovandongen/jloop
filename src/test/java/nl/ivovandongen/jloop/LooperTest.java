package nl.ivovandongen.jloop;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Looper}.
 */
public class LooperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LooperTest.class);

    @After
    public void after() {
        Looper.reset();
    }

    @Test
    public void testMainLoop() throws InterruptedException {
        // Start a thread with the main loop
        CountDownLatch startLatch = new CountDownLatch(1);
        Thread mainThread = new Thread(() -> {
            Looper looper = Looper.prepareMainLooper();
            looper.post(startLatch::countDown);
            looper.run();
        });

        mainThread.start();

        // Wait for main thread start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        // Ensure we can access the main Looper from this thread
        assertNotNull(Looper.getMainLooper());

        // Test Looper#post
        CountDownLatch executeLatch = new CountDownLatch(1);
        Looper.getMainLooper().post(() -> {
            // Should be executed on the main thread
            assertEquals(mainThread.getId(), Thread.currentThread().getId());

            // Assert we have regular Looper access and it is the same
            // as the main looper
            assertNotNull(Looper.get());
            assertEquals(Looper.getMainLooper(), Looper.get());

            executeLatch.countDown();
        });

        // Assure the post is executed
        assertTrue(executeLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testMainLoopShutdown() throws InterruptedException {
        // Start a thread with the main loop
        CountDownLatch startLatch = new CountDownLatch(1);
        Thread mainThread = new Thread(() -> {
            Looper looper = Looper.prepareMainLooper();
            looper.post(startLatch::countDown);
            looper.run();
        });

        mainThread.start();

        // Wait for main thread start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        // Shutdown the thread
        Looper.getMainLooper().shutdown();

        // join the main thread to await shutdown
        mainThread.join();
    }

    @Test
    public void testAsk() throws InterruptedException, TimeoutException, ExecutionException {
        // Start a thread with the main loop
        CountDownLatch startLatch = new CountDownLatch(1);
        Thread mainThread = new Thread(() -> {
            Looper looper = Looper.prepareMainLooper();
            looper.post(startLatch::countDown);
            looper.run();
        });

        mainThread.start();

        // Wait for main thread start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        // Ensure we can access the main Looper from this thread
        assertNotNull(Looper.getMainLooper());

        // Test Looper#ask
        CompletableFuture<Boolean> result = Looper.getMainLooper().ask(() -> {
            // Should be executed on the main thread
            assertEquals(mainThread.getId(), Thread.currentThread().getId());

            return true;
        });

        // Assure the post is executed
        assertTrue(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testPostDelayed() throws InterruptedException {
        // Start a thread with the main loop
        CountDownLatch startLatch = new CountDownLatch(1);
        Thread mainThread = new Thread(() -> {
            Looper looper = Looper.prepareMainLooper();
            looper.post(startLatch::countDown);
            looper.run();
        });

        mainThread.start();

        // Wait for main thread start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        // Test Looper#post
        CountDownLatch executeLatch = new CountDownLatch(1);
        Looper.getMainLooper().postDelayed(() -> {
            LOGGER.info("Delayed task");
            executeLatch.countDown();
        }, 500L, TimeUnit.MILLISECONDS);

        // Assure the post is executed
        long start = System.currentTimeMillis();
        assertTrue(executeLatch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals(System.currentTimeMillis() - start, 500, 50);
    }


    @Test
    public void testPostDelayedOrder() throws InterruptedException {
        // Start a thread with the main loop
        CountDownLatch startLatch = new CountDownLatch(1);
        Thread mainThread = new Thread(() -> {
            Looper looper = Looper.prepareMainLooper();
            looper.post(startLatch::countDown);
            looper.run();
        });

        mainThread.start();

        // Wait for main thread start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        List<Integer> results = new ArrayList<>();

        CountDownLatch executeLatch = new CountDownLatch(3);

        // Executed last
        Looper.getMainLooper().postDelayed(() -> {
            LOGGER.info("Delayed task");
            results.add(3);
            executeLatch.countDown();
        }, 500L, TimeUnit.MILLISECONDS);

        // Executed second
        Looper.getMainLooper().postDelayed(() -> {
            results.add(2);
            executeLatch.countDown();
        }, 100L, TimeUnit.MILLISECONDS);

        // Executed first
        Looper.getMainLooper().post(() -> {
            results.add(1);
            executeLatch.countDown();
        });

        // Assure the posts are executed (and a proper delay was set)
        long start = System.currentTimeMillis();
        assertTrue(executeLatch.await(1000, TimeUnit.SECONDS));
        assertEquals(System.currentTimeMillis() - start, 500, 50);

        // Check the execution order was as expected
        assertArrayEquals(results.toArray(new Integer[3]), new Integer[]{1, 2, 3});
    }

}
