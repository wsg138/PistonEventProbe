package com.enthusia.pistoneventprobe;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class PistonEventProbePlugin extends JavaPlugin implements Listener {

    private static final double POLAR_RADIUS = 16.0;
    private static final double POLAR_RADIUS_SQUARED = POLAR_RADIUS * POLAR_RADIUS;

    private final AtomicLong sequence = new AtomicLong();
    private final Map<BlockPistonExtendEvent, Long> eventIds = new IdentityHashMap<>();

    private boolean continuous;
    private int capturesRemaining;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("pistonprobe"), "pistonprobe command missing from plugin.yml")
                .setExecutor(this);

        getLogger().info("PistonEventProbe enabled. Run /pistonprobe listeners, then /pistonprobe start 10.");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("pistonprobe.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(statusText());
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                int count = 10;
                if (args.length >= 2) {
                    try {
                        count = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        sender.sendMessage("Count must be a number from 1 to 100.");
                        return true;
                    }
                }

                count = Math.max(1, Math.min(100, count));
                continuous = false;
                capturesRemaining = count;
                sender.sendMessage("Piston probe armed for the next " + count + " piston extension event(s).");
                getLogger().info("Capture armed by " + sender.getName() + " for " + count + " event(s).");
            }
            case "continuous" -> {
                continuous = true;
                capturesRemaining = 0;
                sender.sendMessage("Piston probe enabled continuously. Use /pistonprobe stop when finished.");
                getLogger().warning("Continuous capture enabled by " + sender.getName() + ". This can be noisy.");
            }
            case "stop" -> {
                continuous = false;
                capturesRemaining = 0;
                eventIds.clear();
                sender.sendMessage("Piston probe stopped.");
                getLogger().info("Capture stopped by " + sender.getName() + ".");
            }
            case "listeners" -> {
                logRegisteredListeners();
                sender.sendMessage("Registered BlockPistonExtendEvent listeners were written to console/latest.log.");
            }
            default -> sender.sendMessage("Usage: /pistonprobe <start [count]|continuous|stop|status|listeners>");
        }

        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPistonExtendLowest(BlockPistonExtendEvent event) {
        if (!isCapturing()) {
            return;
        }

        long eventId = sequence.incrementAndGet();
        eventIds.put(event, eventId);
        logEventSnapshot(eventId, "LOWEST", event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPistonExtendMonitor(BlockPistonExtendEvent event) {
        Long eventId = eventIds.remove(event);
        if (eventId == null) {
            return;
        }

        logEventSnapshot(eventId, "MONITOR", event);
        List<PlayerSnapshot> snapshots = logPlayerChecks(eventId, event);

        Bukkit.getScheduler().runTask(this, () -> logNextTickPlayers(eventId, snapshots));

        if (!continuous) {
            capturesRemaining = Math.max(0, capturesRemaining - 1);
            if (capturesRemaining == 0) {
                getLogger().info(marker(eventId) + "Capture limit reached; probe automatically stopped.");
            }
        }
    }

    private boolean isCapturing() {
        return continuous || capturesRemaining > 0;
    }

    private String statusText() {
        if (continuous) {
            return "Piston probe status: continuous capture enabled.";
        }
        if (capturesRemaining > 0) {
            return "Piston probe status: armed for " + capturesRemaining + " more event(s).";
        }
        return "Piston probe status: stopped.";
    }

    private void logRegisteredListeners() {
        RegisteredListener[] listeners = BlockPistonExtendEvent.getHandlerList().getRegisteredListeners();
        getLogger().info("[LISTENERS] BlockPistonExtendEvent registered listener count=" + listeners.length);

        for (int index = 0; index < listeners.length; index++) {
            RegisteredListener listener = listeners[index];
            getLogger().info("[LISTENERS] #" + (index + 1)
                    + " plugin=" + listener.getPlugin().getName()
                    + " enabled=" + listener.getPlugin().isEnabled()
                    + " priority=" + listener.getPriority()
                    + " ignoresCancelled=" + listener.isIgnoringCancelled()
                    + " listenerClass=" + listener.getListener().getClass().getName());
        }
    }

    private void logEventSnapshot(long eventId, String phase, BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        Location pistonLocation = piston.getLocation();
        World blockWorld = piston.getWorld();
        World locationWorld = pistonLocation.getWorld();

        getLogger().info(marker(eventId)
                + "phase=" + phase
                + " called=true"
                + " cancelled=" + event.isCancelled()
                + " async=" + event.isAsynchronous()
                + " pistonType=" + piston.getType()
                + " piston=" + formatLocation(pistonLocation)
                + " direction=" + event.getDirection()
                + " movedBlockCount=" + event.getBlocks().size()
                + " blockWorldName=" + blockWorld.getName()
                + " locationWorldName=" + (locationWorld == null ? "<null>" : locationWorld.getName())
                + " sameWorldObject=" + (blockWorld == locationWorld));

        for (int index = 0; index < event.getBlocks().size(); index++) {
            Block moved = event.getBlocks().get(index);
            getLogger().info(marker(eventId)
                    + "phase=" + phase
                    + " movedBlock[" + index + "]=" + moved.getType()
                    + "@" + formatLocation(moved.getLocation()));
        }
    }

    private List<PlayerSnapshot> logPlayerChecks(long eventId, BlockPistonExtendEvent event) {
        Location pistonLocation = event.getBlock().getLocation();
        World pistonWorld = pistonLocation.getWorld();
        List<PlayerSnapshot> snapshots = new ArrayList<>();

        if (pistonWorld == null) {
            getLogger().severe(marker(eventId) + "Piston location returned a null world; Polar's world check cannot pass.");
            return snapshots;
        }

        List<PlayerDistance> players = Bukkit.getOnlinePlayers().stream()
                .map(player -> calculatePlayerDistance(player, pistonLocation, event.getBlocks()))
                .sorted(Comparator.comparingDouble(PlayerDistance::pistonDistanceSquared))
                .toList();

        getLogger().info(marker(eventId) + "onlinePlayers=" + players.size()
                + " pistonWorld=" + pistonWorld.getName());

        for (PlayerDistance result : players) {
            Player player = result.player();
            Location playerLocation = player.getLocation();
            Vector velocity = player.getVelocity();

            boolean sameWorldObject = player.getWorld() == pistonWorld;
            boolean worldNameEquals = player.getWorld().getName().equals(pistonWorld.getName());
            boolean within16OfPistonCorner = result.pistonDistanceSquared() <= POLAR_RADIUS_SQUARED;
            boolean passesDescribedPolarFilter = worldNameEquals && within16OfPistonCorner;
            boolean within16OfAnyMovedBlock = result.nearestMovedBlockDistanceSquared() <= POLAR_RADIUS_SQUARED;

            getLogger().info(marker(eventId)
                    + "player=" + player.getName()
                    + " uuid=" + player.getUniqueId()
                    + " playerWorld=" + player.getWorld().getName()
                    + " playerLoc=" + formatLocation(playerLocation)
                    + " sameWorldObject=" + sameWorldObject
                    + " worldNameEquals=" + worldNameEquals
                    + " pistonDistance=" + formatDistance(result.pistonDistanceSquared())
                    + " horizontalPistonDistance=" + formatDistance(result.horizontalPistonDistanceSquared())
                    + " nearestMovedBlockDistance=" + formatDistance(result.nearestMovedBlockDistanceSquared())
                    + " within16OfPiston=" + within16OfPistonCorner
                    + " within16OfMovedBlock=" + within16OfAnyMovedBlock
                    + " describedPolarFilterPass=" + passesDescribedPolarFilter
                    + " velocity=" + formatVector(velocity));

            if (sameWorldObject && result.pistonDistanceSquared() <= 32.0 * 32.0) {
                snapshots.add(new PlayerSnapshot(player.getUniqueId(), player.getName(), playerLocation.clone(), velocity.clone()));
            }
        }

        return snapshots;
    }

    private PlayerDistance calculatePlayerDistance(Player player, Location pistonLocation, List<Block> movedBlocks) {
        Location playerLocation = player.getLocation();

        // Coordinate-only calculation is intentional so the probe can report values even if
        // an unexpected world-object/name mismatch exists.
        double dx = playerLocation.getX() - pistonLocation.getX();
        double dy = playerLocation.getY() - pistonLocation.getY();
        double dz = playerLocation.getZ() - pistonLocation.getZ();
        double pistonDistanceSquared = dx * dx + dy * dy + dz * dz;
        double horizontalDistanceSquared = dx * dx + dz * dz;

        double nearestMovedBlockDistanceSquared = Double.POSITIVE_INFINITY;
        for (Block movedBlock : movedBlocks) {
            Location movedLocation = movedBlock.getLocation();
            double movedDx = playerLocation.getX() - movedLocation.getX();
            double movedDy = playerLocation.getY() - movedLocation.getY();
            double movedDz = playerLocation.getZ() - movedLocation.getZ();
            double distanceSquared = movedDx * movedDx + movedDy * movedDy + movedDz * movedDz;
            nearestMovedBlockDistanceSquared = Math.min(nearestMovedBlockDistanceSquared, distanceSquared);
        }

        if (movedBlocks.isEmpty()) {
            nearestMovedBlockDistanceSquared = pistonDistanceSquared;
        }

        return new PlayerDistance(
                player,
                pistonDistanceSquared,
                horizontalDistanceSquared,
                nearestMovedBlockDistanceSquared
        );
    }

    private void logNextTickPlayers(long eventId, List<PlayerSnapshot> snapshots) {
        for (PlayerSnapshot before : snapshots) {
            Player player = Bukkit.getPlayer(before.uuid());
            if (player == null || !player.isOnline()) {
                getLogger().info(marker(eventId) + "nextTick player=" + before.name() + " offline=true");
                continue;
            }

            Location afterLocation = player.getLocation();
            Vector afterVelocity = player.getVelocity();
            double movement = sameWorld(before.location(), afterLocation)
                    ? before.location().distance(afterLocation)
                    : Double.NaN;

            getLogger().info(marker(eventId)
                    + "nextTick player=" + player.getName()
                    + " loc=" + formatLocation(afterLocation)
                    + " movedSinceEvent=" + (Double.isNaN(movement) ? "different-world" : formatDecimal(movement))
                    + " velocityBefore=" + formatVector(before.velocity())
                    + " velocityAfter=" + formatVector(afterVelocity));
        }
    }

    private boolean sameWorld(Location first, Location second) {
        return first.getWorld() != null && first.getWorld() == second.getWorld();
    }

    private String marker(long eventId) {
        return "[PROBE event=" + eventId + "] ";
    }

    private String formatLocation(Location location) {
        String world = location.getWorld() == null ? "<null>" : location.getWorld().getName();
        return world + "(" + formatDecimal(location.getX()) + ","
                + formatDecimal(location.getY()) + ","
                + formatDecimal(location.getZ()) + ")";
    }

    private String formatVector(Vector vector) {
        return "(" + formatDecimal(vector.getX()) + ","
                + formatDecimal(vector.getY()) + ","
                + formatDecimal(vector.getZ()) + ")";
    }

    private String formatDistance(double distanceSquared) {
        if (Double.isInfinite(distanceSquared)) {
            return "infinite";
        }
        return formatDecimal(Math.sqrt(distanceSquared));
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private record PlayerDistance(
            Player player,
            double pistonDistanceSquared,
            double horizontalPistonDistanceSquared,
            double nearestMovedBlockDistanceSquared
    ) {
    }

    private record PlayerSnapshot(UUID uuid, String name, Location location, Vector velocity) {
    }
}
