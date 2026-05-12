package com.skyblockexp.ezshops.common;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * Unified scheduler abstraction that transparently supports both the standard
 * Bukkit/Paper scheduler and the Folia regional scheduler API.
 *
 * <p>On Folia, sync tasks are dispatched to the {@code GlobalRegionScheduler}
 * and async tasks to the {@code AsyncScheduler}. On all other server
 * implementations the familiar {@code BukkitScheduler} is used.</p>
 *
 * <p>All callers should use this class instead of calling
 * {@code Bukkit.getScheduler()} or {@code plugin.getServer().getScheduler()}
 * directly so that the plugin remains Folia-compatible.</p>
 */
public final class SchedulerAdapter {

    private static final boolean FOLIA = CompatibilityUtil.isFolia();

    /** Ticks → milliseconds conversion (1 tick = 50 ms at 20 TPS). */
    private static final long MILLIS_PER_TICK = 50L;

    private SchedulerAdapter() {}

    // -------------------------------------------------------------------------
    // Sync tasks (main thread / global region on Folia)
    // -------------------------------------------------------------------------

    /**
     * Schedules a one-shot task to run on the server's primary thread (or Folia's
     * global region) as soon as possible.
     *
     * @param plugin the owning plugin
     * @param task   the runnable to execute
     * @return a handle that can be used to cancel the task
     */
    public static TaskHandle runTask(Plugin plugin, Runnable task) {
        if (FOLIA) {
            var scheduled = Bukkit.getGlobalRegionScheduler().run(plugin, st -> task.run());
            return foliaHandle(scheduled);
        }
        BukkitTask bt = Bukkit.getScheduler().runTask(plugin, task);
        return bukkitHandle(bt);
    }

    /**
     * Schedules a repeating task on the server's primary thread (or Folia's global
     * region).
     *
     * @param plugin      the owning plugin
     * @param task        the runnable to execute each period
     * @param delayTicks  initial delay in ticks
     * @param periodTicks repeat interval in ticks
     * @return a handle that can be used to cancel the task
     */
    public static TaskHandle runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            var scheduled = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, st -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
            return foliaHandle(scheduled);
        }
        BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bukkitHandle(bt);
    }

    // -------------------------------------------------------------------------
    // Async tasks
    // -------------------------------------------------------------------------

    /**
     * Schedules a one-shot task to run asynchronously off the main thread.
     *
     * @param plugin the owning plugin
     * @param task   the runnable to execute
     * @return a handle that can be used to cancel the task
     */
    public static TaskHandle runTaskAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            var scheduled = Bukkit.getAsyncScheduler().runNow(plugin, st -> task.run());
            return foliaHandle(scheduled);
        }
        BukkitTask bt = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        return bukkitHandle(bt);
    }

    /**
     * Schedules a repeating async task.
     *
     * @param plugin      the owning plugin
     * @param task        the runnable to execute each period
     * @param delayTicks  initial delay in ticks (converted to milliseconds for Folia)
     * @param periodTicks repeat interval in ticks (converted to milliseconds for Folia)
     * @return a handle that can be used to cancel the task
     */
    public static TaskHandle runTaskTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (FOLIA) {
            long delayMs = Math.max(MILLIS_PER_TICK, delayTicks * MILLIS_PER_TICK);
            long periodMs = Math.max(MILLIS_PER_TICK, periodTicks * MILLIS_PER_TICK);
            var scheduled = Bukkit.getAsyncScheduler()
                    .runAtFixedRate(plugin, st -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
            return foliaHandle(scheduled);
        }
        BukkitTask bt = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkitHandle(bt);
    }

    // -------------------------------------------------------------------------
    // Internal handle factories
    // -------------------------------------------------------------------------

    private static TaskHandle bukkitHandle(BukkitTask bt) {
        return new TaskHandle() {
            @Override
            public void cancel() {
                bt.cancel();
            }

            @Override
            public boolean isCancelled() {
                return bt.isCancelled();
            }
        };
    }

    private static TaskHandle foliaHandle(io.papermc.paper.threadedregions.scheduler.ScheduledTask st) {
        return new TaskHandle() {
            @Override
            public void cancel() {
                st.cancel();
            }

            @Override
            public boolean isCancelled() {
                return st.isCancelled();
            }
        };
    }
}
