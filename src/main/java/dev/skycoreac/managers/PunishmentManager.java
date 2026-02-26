package dev.skycoreac.managers;

import dev.skycoreac.SkyCoreAC;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PunishmentManager {

    private final SkyCoreAC plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String DISCORD = "https://discord.gg/zB6hQP99ZA";

    // Tracks how many times each player has been kicked by SkyCoreAC
    private final Map<UUID, Integer> kickCounts = new HashMap<>();

    // How many kicks before a ban (configurable, default 3)
    private static final int KICKS_BEFORE_BAN = 3;

    public PunishmentManager(SkyCoreAC plugin) {
        this.plugin = plugin;
    }

    public void punish(Player player, String checkName, int vl) {
        if (!plugin.getConfig().getBoolean("punishments.enabled", true)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            UUID uuid = player.getUniqueId();
            int kicks = kickCounts.getOrDefault(uuid, 0) + 1;
            kickCounts.put(uuid, kicks);

            if (kicks >= KICKS_BEFORE_BAN) {
                // Ban after too many kicks
                kickCounts.remove(uuid);
                player.kick(MM.deserialize(
                    "<red><bold>SkyCoreAC - Banned</bold></red>\n" +
                    "<gray>You have been banned for repeated suspicious activity.\n\n" +
                    "<white>Check: <yellow>" + checkName + "\n\n" +
                    "<gray>Appeal at:\n" +
                    "<aqua>" + DISCORD
                ));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "ban " + player.getName() + " [SkyCoreAC] " + checkName +
                    " | Appeal: " + DISCORD);
                plugin.getLogger().info("[PUNISH] Banned " + player.getName() +
                    " | " + checkName + " VL:" + vl + " (kick #" + kicks + ")");
            } else {
                // Kick with warning and kick count
                int remaining = KICKS_BEFORE_BAN - kicks;
                player.kick(MM.deserialize(
                    "<red><bold>SkyCoreAC</bold></red>\n" +
                    "<gray>Removed for suspicious activity.\n\n" +
                    "<white>Check: <yellow>" + checkName + "\n" +
                    "<gray>Kicks: <white>" + kicks + "<gray>/" + KICKS_BEFORE_BAN + "\n\n" +
                    "<red>" + remaining + " more kick(s) will result in a ban.\n\n" +
                    "<gray>Appeal at:\n" +
                    "<aqua>" + DISCORD
                ));
                plugin.getLogger().info("[PUNISH] Kicked " + player.getName() +
                    " | " + checkName + " VL:" + vl + " (kick " + kicks + "/" + KICKS_BEFORE_BAN + ")");
            }
        });
    }

    public void resetKicks(UUID uuid) {
        kickCounts.remove(uuid);
    }

    public int getKickCount(UUID uuid) {
        return kickCounts.getOrDefault(uuid, 0);
    }
}
