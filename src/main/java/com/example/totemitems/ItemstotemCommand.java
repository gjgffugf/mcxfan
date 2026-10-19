package com.example.totemitems;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /itemstotem on|off|toggle|status [玩家]
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
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        // 不带玩家名 = 对自己生效
        if (args.length == 1) {
            Player self = (sender instanceof Player p) ? p : null;
            if (self == null) {
                sender.sendMessage("§c控制台请指定玩家：/" + label + " " + action + " <玩家>");
                return true;
            }
            if (!self.hasPermission("allitemstotem.use")) {
                sender.sendMessage("§c你没有权限开关该效果。");
                return true;
            }
            apply(sender, self, action, true);
            return true;
        }

        // 带玩家名 = 需要管理员权限
        if (!sender.hasPermission("allitemstotem.admin")) {
            sender.sendMessage("§c你没有权限修改其他玩家的设置。");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§c玩家 §e" + args[1] + " §c不在线或不存在。");
            return true;
        }
        apply(sender, target, action, false);
        return true;
    }

    private void apply(CommandSender sender, Player target, String action, boolean self) {
        boolean enabled;
        switch (action) {
            case "on" -> enabled = true;
            case "off" -> enabled = false;
            case "toggle" -> enabled = !plugin.isEffectEnabled(target);
            case "status" -> {
                String state = plugin.isEffectEnabled(target) ? "§a开启" : "§c关闭";
                sender.sendMessage("§7" + target.getName() + " 的物品图腾效果：" + state);
                return;
            }
            default -> {
                sendHelp(sender, "itemstotem");
                return;
            }
        }

        plugin.setEffectEnabled(target, enabled);
        String state = enabled ? "§a开启" : "§c关闭";

        if (self) {
            sender.sendMessage("§7你的物品图腾效果已" + state + "§7。");
        } else {
            sender.sendMessage("§7已把 §e" + target.getName() + " §7的物品图腾效果设为" + state + "§7。");
            target.sendMessage("§7你的物品图腾效果被设为" + state + "§7。");
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§6物品图腾 §7- 开关设置");
        sender.sendMessage("§7/" + label + " on §8- 开启");
        sender.sendMessage("§7/" + label + " off §8- 关闭");
        sender.sendMessage("§7/" + label + " toggle §8- 切换");
        sender.sendMessage("§7/" + label + " status §8- 查看当前状态");
        if (sender.hasPermission("allitemstotem.admin")) {
            sender.sendMessage("§7/" + label + " <on|off|toggle|status> <玩家> §8- 管理他人");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
            return result;
        }
        if (args.length == 2 && sender.hasPermission("allitemstotem.admin")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(player.getName());
                }
            }
            return result;
        }
        return List.of();
    }
}
