package com.example.totemitems;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 让所有物品都拥有不死图腾的效果。
 */
public final class AllItemsTotem extends JavaPlugin implements TabExecutor {

    private TotemSettings settings;
    private ResurrectListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new TotemSettings(getConfig(), getLogger());
        this.listener = new ResurrectListener(this);
        getServer().getPluginManager().registerEvents(this.listener, this);

        var cmd = getCommand("allitemstotem");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        var switchCmd = getCommand("itemstotem");
        if (switchCmd != null) {
            ItemstotemCommand executor = new ItemstotemCommand(this);
            switchCmd.setExecutor(executor);
            switchCmd.setTabCompleter(executor);
        }

        getLogger().info("AllItemsTotem 已启用（所有物品 = 不死图腾），当前效果："
                + (this.settings.enabled ? "开启" : "关闭"));
    }

    @Override
    public void onDisable() {
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

    // ------------------------------------------------------------------ 全局开关

    /** 全局效果是否开启（对所有玩家生效，写回 config.yml） */
    public boolean isEffectEnabled() {
        return this.settings.enabled;
    }

    public void setEffectEnabled(boolean enabled) {
        getConfig().set("enabled", enabled);
        saveConfig();
        reload();
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
