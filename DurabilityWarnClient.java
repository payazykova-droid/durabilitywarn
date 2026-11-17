package com.example.durabilitywarn;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class DurabilityWarnClient implements ClientModInitializer {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, // CHEST = нагрудник и элитры
            EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final Map<EquipmentSlot, Long> lastWarn = new EnumMap<>(EquipmentSlot.class);
    private final Map<EquipmentSlot, Object> lastItem = new EnumMap<>(EquipmentSlot.class);
    private long lastAnyWarn = -1000;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null) return;
        long time = client.level.getGameTime();
        if (time % 10 != 0) return;

        ModConfig cfg = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!cfg.enabled) return;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                lastItem.remove(slot);
                lastWarn.remove(slot);
                continue;
            }

            int max = stack.getMaxDamage();
            int left = max - stack.getDamageValue();
            int limit = Math.max(1, max * cfg.thresholdPercent / 100);
            if (left > limit) {
                lastItem.remove(slot);
                lastWarn.remove(slot);
                continue;
            }

            Object key = stack.getItem();
            boolean changed = lastItem.get(slot) != key;
            Long prev = lastWarn.get(slot);
            boolean due = changed || prev == null || time - prev >= cfg.repeatSeconds * 20L;
            if (!due || time - lastAnyWarn < 40) continue;

            lastItem.put(slot, key);
            lastWarn.put(slot, time);
            lastAnyWarn = time;
            warn(client, cfg, stack, left);
            break; // одно предупреждение за раз
        }
    }

    private void warn(Minecraft client, ModConfig cfg, ItemStack stack, int left) {
        if (cfg.playSound) {
            client.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 0.7f));
        }
        if (cfg.showText) {
            client.gui.setOverlayMessage(
                    Component.translatable("durabilitywarn.message", stack.getHoverName(), left), false);
        }
    }
}
