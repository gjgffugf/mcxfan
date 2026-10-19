package com.example.totemitems;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /itemstotem on|off|toggle|status
 * 管理员用的全局开关：对整个服务器所有玩家生效，状态写入 config.yml 的 enabled。
 */
public final class ItemstotemCommand implements TabExecutor {

    private static final List<String> SUB_COMMANDS = List.of("on", "off", "toggle", "status");

    private final AllItemsTotem plugin;

    ItemstotemCommand(AllItemsTotem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("allitemstotem.admin")) {
            sender.sendMessage("§c你没有权限执行该命令（需要 allitemstotem.admin）。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                if (plugin.isEffectEnabled()) {
                    sender.sendMessage("§7物品图腾效果已经是 §a开启 §7状态。");
                    return true;
                }
                plugin.setEffectEnabled(true);
                broadcast(sender, "§a开启");
            }
            case "off" -> {
                if (!plugin.isEffectEnabled()) {
                    sender.sendMessage("§7物品图腾效果已经是 §c关闭 §7状态。");
                    return true;
                }
                plugin.setEffectEnabled(false);
                broadcast(sender, "§c关闭");
            }
            case "toggle" -> {
                plugin.setEffectEnabled(!plugin.isEffectEnabled());
                broadcast(sender, plugin.isEffectEnabled() ? "§a开启" : "§c关闭");
            }
            case "status" -> sender.sendMessage("§7物品图腾效果（全局）："
                    + (plugin.isEffectEnabled() ? "§a开启" : "§c关闭"));
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void broadcast(CommandSender sender, String state) {
        sender.sendMessage("§7已将物品图腾效果（全局）设为" + state + "§7。");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§6物品图腾 §7- 全局开关（管理员）");
        sender.sendMessage("§7/" + label + " on §8- 对所有玩家开启效果");
        sender.sendMessage("§7/" + label + " off §8- 对所有玩家关闭效果");
        sender.sendMessage("§7/" + label + " toggle §8- 切换");
        sender.sendMessage("§7/" + label + " status §8- 查看当前状态");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("allitemstotem.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }
        return List.of();
    }
}
