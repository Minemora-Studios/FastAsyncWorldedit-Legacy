package com.boydti.fawe.bukkit.v0;

public class ChunkListener_8 extends ChunkListener {

    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Override
    protected int getDepth(Exception ex) {
        return ex.getStackTrace().length;
    }

    @Override
    protected StackTraceElement getElement(Exception ex, int index) {
        StackTraceElement[] trace = ex.getStackTrace();
        return (index >= 0 && index < trace.length) ? trace[index] : null;
    }

}