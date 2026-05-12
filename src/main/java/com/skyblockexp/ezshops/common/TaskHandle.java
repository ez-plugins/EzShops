package com.skyblockexp.ezshops.common;

/**
 * Platform-agnostic handle for a scheduled task.
 * Wraps both {@code BukkitTask} (Bukkit/Paper) and Folia's {@code ScheduledTask}.
 */
public interface TaskHandle {

    /** Requests cancellation of the task. */
    void cancel();

    /** Returns true if the task has been cancelled. */
    boolean isCancelled();
}
