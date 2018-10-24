package nl.ivovandongen.jloop;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link LooperThread}.
 */
public class LooperThreadTest {

    @Test
    public void testLoopStart() throws InterruptedException {
        LooperThread lp = new LooperThread();
        lp.start();
        CountDownLatch latch = new CountDownLatch(1);
        lp.post(latch::countDown);
        latch.await(1, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void testPostAfterLoopStopped() {
        LooperThread lp = new LooperThread();
        lp.start();
        lp.stop();
        lp.post(() -> {
            //NOOP
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testPostDelayedAfterLoopStopped() {
        LooperThread lp = new LooperThread();
        lp.start();
        lp.stop();
        lp.postDelayed(() -> {
            //NOOP
        }, 1L, TimeUnit.MILLISECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void testAskAfterLoopStopped() {
        LooperThread lp = new LooperThread();
        lp.start();
        lp.stop();
        lp.ask(() -> 1);
    }

    @Test
    public void testStopBeforeStart() {
        LooperThread lp = new LooperThread();
        lp.stop();
        // All good
    }

    @Test
    public void testRestart() throws InterruptedException {
        LooperThread lp = new LooperThread();
        lp.start();
        lp.stop();
        lp.start();
        CountDownLatch latch = new CountDownLatch(1);
        lp.post(latch::countDown);
        latch.await(1, TimeUnit.SECONDS);
    }
}
