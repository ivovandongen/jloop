package nl.ivovandongen.jloop;

import java.util.concurrent.CountDownLatch;

/**
 * Quick latch that can be used to block till a loop is started.
 */
class StartupLatch {

    private final CountDownLatch latch;

    /**
     * Creates the latch.
     */
    StartupLatch() {
        this.latch = new CountDownLatch(1);
    }

    /**
     * Called when the Latched loop is started.
     */
    void started() {
        latch.countDown();
    }

    /**
     * Called by the client to wait for the Loop startup.
     */
    void await() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
