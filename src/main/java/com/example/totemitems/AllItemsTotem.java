package com.example.totemitems;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 让所有物品都拥有不死图腾的效果。
 */
public final class AllItemsTotem extends JavaPlugin implements TabExecutor {

    private TotemSettings settings;
    private ResurrectListener listener;

    /** 关闭了效果的玩家（默认所有人都开启） */
    private final Set<UUID> disabled = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new TotemSettings(getConfig(), getLogger());
        this.listener = new ResurrectListener(this);
        getServer().getPluginManager().registerEvents(this.listener, this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(), this);

        loadDisabledPlayers();

        var cmd = getCommand("allitemstotem");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        var toggleCmd = getCommand("itemstotem");
        if (toggleCmd != null) {
            ItemstotemCommand executor = new ItemstotemCommand(this);
            toggleCmd.setExecutor(executor);
            toggleCmd.setTabCompleter(executor);
        }

        getLogger().info("AllItemsTotem 已启用（所有物品 = 不死图腾）");
    }

    @Override
    public void onDisable() {
        saveDisabledPlayers();
        getLogger().info("AllItemsTotem 已卸载");
    }

    public void reload() {
        reloadConfig();
        this.settings = new TotemSettings(getConfig(), getLogger());
        this.listener.setSettings(this.settings);
    }

    public TotemSettings settings() {
        return this.settings;
    }

    // ------------------------------------------------------------------ 玩家开关

    public boolean isEffectEnabled(Player player) {
        return !this.disabled.contains(player.getUniqueId());
    }

    public void setEffectEnabled(Player player, boolean enabled) {
        if (enabled) {
            this.disabled.remove(player.getUniqueId());
        } else {
            this.disabled.add(player.getUniqueId());
        }
        saveDisabledPlayersAsync();
    }

    private File stateFile() {
        return new File(getDataFolder(), "players.yml");
    }

    private void loadDisabledPlayers() {
        this.disabled.clear();
        File file = stateFile();
        if (!file.exists()) {
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String raw : data.getStringList("disabled")) {
            try {
                this.disabled.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // 忽略无效数据
            }
        }
    }

    private void saveDisabledPlayers() {
        FileConfiguration data = new YamlConfiguration();
        List<String> list = this.disabled.stream().map(UUID::toString).toList();
        data.set("disabled", list);
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().warning("无法创建插件数据目录");
                return;
            }
            data.save(stateFile());
        } catch (IOException e) {
            getLogger().warning("保存 players.yml 失败：" + e.getMessage());
        }
    }

    private void saveDisabledPlayersAsync() {
        Bukkit.getAsyncScheduler().runNow(this, task -> saveDisabledPlayers());
    }

    private final class JoinQuitListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            saveDisabledPlayers();
        }
    }

    // ------------------------------------------------------------------ /allitemstotem（管理员）

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("allitemstotem.admin")) {
            sender.sendMessage("§c你没有权限执行该命令。");
            return true;
        }
        reload();
        sender.sendMessage("§aAllItemsTotem 配置已重载。");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        return sender.hasPermission("allitemstotem.admin") ? List.of("reload") : List.of();
    }
}
