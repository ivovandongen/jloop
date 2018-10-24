package nl.ivovandongen.jloop;

/**
 * A message wrapping a {@link Runnable}.
 */
class RunnableMessage implements Message {

    private Runnable runnable;

    RunnableMessage(final Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void execute() {
        runnable.run();
    }
}
