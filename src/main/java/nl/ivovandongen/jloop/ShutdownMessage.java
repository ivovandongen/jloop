package nl.ivovandongen.jloop;

/**
 * Message to be posted when the {@link Loop} needs to stop
 * after processing the queue up to this point.
 */
class ShutdownMessage implements Message {
    private final Looper looper;

    ShutdownMessage(final Looper looper) {
        this.looper = looper;
    }

    @Override
    public void execute() {
        looper.halt();
    }
}
