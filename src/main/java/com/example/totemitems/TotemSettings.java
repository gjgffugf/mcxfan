package com.example.totemitems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * config.yml 的运行时快照，避免每次事件都去读配置。
 */
public final class TotemSettings {

    public final boolean enabled;
    public final boolean consumeItem;
    public final boolean includeArmor;
    public final double healHealth;
    public final boolean clearEffects;
    public final boolean extinguish;
    public final int invulnerableTicks;
    public final boolean requirePermission;
    public final boolean particles;
    public final int particleAmount;
    public final boolean sound;
    public final List<PotionEffect> effects;
    public final Set<Material> blacklist;
    public final Set<EntityDamageEvent.DamageCause> excludedCauses;

    public TotemSettings(FileConfiguration config, Logger logger) {
        this.enabled = config.getBoolean("enabled", true);
        this.consumeItem = config.getBoolean("consume-item", true);
        this.includeArmor = config.getBoolean("include-armor", false);
        this.healHealth = Math.max(0.5D, config.getDouble("heal-health", 1.0D));
        this.clearEffects = config.getBoolean("clear-effects", true);
        this.extinguish = config.getBoolean("extinguish", true);
        this.invulnerableTicks = Math.max(0, config.getInt("invulnerable-ticks", 0));
        this.requirePermission = config.getBoolean("require-permission", false);
        this.particles = config.getBoolean("particles.enabled", true);
        this.particleAmount = Math.max(1, config.getInt("particles.amount", 100));
        this.sound = config.getBoolean("sound.enabled", true);

        this.effects = Collections.unmodifiableList(readEffects(config, logger));
        this.blacklist = Collections.unmodifiableSet(readBlacklist(config, logger));
        this.excludedCauses = Collections.unmodifiableSet(readCauses(config, logger));
    }

    private static List<PotionEffect> readEffects(FileConfiguration config, Logger logger) {
        List<PotionEffect> result = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("effects");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            String typeName = section.getString(key + ".type", key);
            int amplifier = section.getInt(key + ".amplifier", 0);
            int duration = section.getInt(key + ".duration", 100);

            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(
                    NamespacedKey.minecraft(typeName.toLowerCase(Locale.ROOT).replace(' ', '_')));
            if (type == null) {
                type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(typeName.toLowerCase(Locale.ROOT)));
            }
            if (type == null) {
                logger.warning("[AllItemsTotem] 未知的药水效果类型: " + typeName + "（已跳过）");
                continue;
            }
            result.add(new PotionEffect(type, duration, amplifier));
        }
        return result;
    }

    private static Set<Material> readBlacklist(FileConfiguration config, Logger logger) {
        Set<Material> result = new HashSet<>();
        for (String raw : config.getStringList("blacklist")) {
            Material material = Material.matchMaterial(raw.trim());
            if (material == null) {
                logger.warning("[AllItemsTotem] 未知的材质: " + raw + "（已跳过）");
                continue;
            }
            result.add(material);
        }
        return result;
    }

    private static Set<EntityDamageEvent.DamageCause> readCauses(FileConfiguration config, Logger logger) {
        Set<EntityDamageEvent.DamageCause> result = EnumSet.noneOf(EntityDamageEvent.DamageCause.class);
        for (String raw : config.getStringList("excluded-causes")) {
            try {
                result.add(EntityDamageEvent.DamageCause.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                logger.warning("[AllItemsTotem] 未知的伤害类型: " + raw + "（已跳过）");
            }
        }
        return result;
    }
}
