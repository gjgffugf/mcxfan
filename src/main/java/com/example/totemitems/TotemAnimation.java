package com.example.totemitems;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 用 ItemDisplay 实体模拟原版不死图腾的"物品举到眼前旋转"动画。
 * 原版这个动画是纯客户端的，服务端无法直接触发，只能这样还原。
 */
public final class TotemAnimation {

    private TotemAnimation() {
    }

    public static void play(Plugin plugin, Player player, ItemStack item, TotemSettings settings) {
        if (!settings.animation || item == null || item.getType().isAir()) {
            return;
        }

        Location eye = player.getEyeLocation();
        ItemStack shown = item.clone();
        shown.setAmount(1);

        ItemDisplay display = eye.getWorld().spawn(eye, ItemDisplay.class, entity -> {
            entity.setItemStack(shown);
            entity.setBillboard(Display.Billboard.FIXED);           // 固定朝向，才能看出 3D 旋转
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setPersistent(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setViewRange(64.0F);
            entity.setBrightness(new Display.Brightness(15, 15));   // 暗处也能看清
            entity.setInterpolationDuration(0);                      // 自己每 tick 更新，不用客户端插值
        });

        final int total = settings.animationTicks;
        final double distance = settings.animationDistance;
        final int[] tick = {0};

        Bukkit.getRegionScheduler().runAtFixedRate(plugin, eye, task -> {
            tick[0]++;
            int t = tick[0];

            if (t > total || !player.isOnline() || !display.isValid()) {
                cancel(task, display);
                return;
            }

            // 跟随玩家的视线，始终悬在眼前
            Location current = player.getEyeLocation();
            Location target = current.clone().add(current.getDirection().normalize().multiply(distance));
            target.setPitch(current.getPitch());
            display.teleport(target);

            float progress = t / (float) total;
            float spin = (float) Math.toRadians(t * 18.0F);          // 绕 Y 轴自转
            float tilt = (float) Math.toRadians(-8.0F);              // 略微仰起，更接近原版
            float scale = scaleAt(progress);

            Quaternionf rotation = new Quaternionf(new AxisAngle4f(tilt, 1.0F, 0.0F, 0.0F))
                    .mul(new Quaternionf(new AxisAngle4f(spin, 0.0F, 1.0F, 0.0F)));

            display.setTransformation(new Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    rotation,
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
        }, 1L, 1L);
    }

    /** 弹出 → 停住 → 缩小消失 */
    private static float scaleAt(float progress) {
        if (progress < 0.15F) {
            float k = progress / 0.15F;
            return 0.30F + 0.95F * k;
        } else if (progress < 0.70F) {
            float k = (progress - 0.15F) / 0.55F;
            return 1.25F - 0.35F * k;
        } else {
            float k = (progress - 0.70F) / 0.30F;
            return 0.90F * (1.0F - k);
        }
    }

    private static void cancel(ScheduledTask task, ItemDisplay display) {
        task.cancel();
        if (display.isValid()) {
            display.remove();
        }
    }
}
