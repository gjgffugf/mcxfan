package com.example.totemitems;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 核心逻辑：玩家受到致命伤害时，从物品栏里找一件物品当作不死图腾用掉。
 */
public final class ResurrectListener implements Listener {

    private final AllItemsTotem plugin;
    private TotemSettings settings;

    ResurrectListener(AllItemsTotem plugin) {
        this.plugin = plugin;
        this.settings = plugin.settings();
    }

    void setSettings(TotemSettings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        TotemSettings s = this.settings;
        if (!s.enabled || s.excludedCauses.contains(event.getCause())) {
            return;
        }
        // s.enabled 即全局开关，管理员用 /itemstotem on|off 控制

        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return;
        }
        if (player.isDead() || !player.isValid()) {
            return;
        }
        if (s.requirePermission && !player.hasPermission("allitemstotem.use")) {
            return;
        }

        // 只有真正会致死的伤害才触发
        if (event.getFinalDamage() < player.getHealth()) {
            return;
        }

        Slot slot = findItem(player, s);
        if (slot == null) {
            return; // 身上没有任何可用物品，正常死亡
        }

        // 广播一次原地复活事件，其它插件仍可以否决这次复活
        EntityResurrectEvent resurrectEvent = new EntityResurrectEvent(player);
        Bukkit.getPluginManager().callEvent(resurrectEvent);
        if (resurrectEvent.isCancelled()) {
            return;
        }

        ItemStack shown = slot.item().clone();

        if (s.consumeItem && !consume(player, slot)) {
            return;
        }

        event.setCancelled(true);
        resurrect(player, s);
        TotemAnimation.play(plugin, player, shown, s);
    }

    /** 执行原版不死图腾同款的复活效果。 */
    private void resurrect(Player player, TotemSettings s) {
        player.setHealth(Math.min(s.healHealth, player.getMaxHealth()));
        if (s.clearEffects) {
            player.clearActivePotionEffects();
        }
        if (s.extinguish) {
            player.setFireTicks(0);
        }
        for (org.bukkit.potion.PotionEffect effect : s.effects) {
            player.addPotionEffect(effect);
        }
        if (s.invulnerableTicks > 0) {
            player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), s.invulnerableTicks));
        }

        World world = player.getWorld();
        Location loc = player.getLocation().add(0.0D, 1.0D, 0.0D);
        if (s.particles) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc,
                    s.particleAmount, 0.45D, 0.60D, 0.45D, 0.40D);
        }
        if (s.sound) {
            world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ 物品查找与消耗

    private enum SlotType { MAIN_HAND, OFF_HAND, STORAGE, ARMOR }

    private record Slot(SlotType type, int index, ItemStack item) {}

    /** 顺序：主手 → 副手 → 背包（含快捷栏）→ 盔甲栏（可选） */
    private Slot findItem(Player player, TotemSettings s) {
        PlayerInventory inv = player.getInventory();

        ItemStack mainHand = inv.getItemInMainHand();
        if (isUsable(mainHand, s)) {
            return new Slot(SlotType.MAIN_HAND, -1, mainHand);
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (isUsable(offHand, s)) {
            return new Slot(SlotType.OFF_HAND, -1, offHand);
        }

        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (isUsable(storage[i], s)) {
                return new Slot(SlotType.STORAGE, i, storage[i]);
            }
        }

        if (s.includeArmor) {
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (isUsable(armor[i], s)) {
                    return new Slot(SlotType.ARMOR, i, armor[i]);
                }
            }
        }
        return null;
    }

    private boolean consume(Player player, Slot slot) {
        PlayerInventory inv = player.getInventory();
        ItemStack left = shrink(slot.item());

        switch (slot.type()) {
            case MAIN_HAND -> inv.setItemInMainHand(left);
            case OFF_HAND -> inv.setItemInOffHand(left);
            case STORAGE -> {
                ItemStack[] storage = inv.getStorageContents();
                storage[slot.index()] = left;
                inv.setStorageContents(storage);
            }
            case ARMOR -> {
                ItemStack[] armor = inv.getArmorContents();
                armor[slot.index()] = left;
                inv.setArmorContents(armor);
            }
        }
        player.updateInventory();
        return true;
    }

    private static ItemStack shrink(ItemStack item) {
        if (item == null || item.getAmount() <= 1) {
            return null; // 用掉最后一个，清空该格
        }
        ItemStack copy = item.clone();
        copy.setAmount(item.getAmount() - 1);
        return copy;
    }

    private static boolean isUsable(ItemStack item, TotemSettings s) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return false;
        }
        return !s.blacklist.contains(item.getType());
    }
}
