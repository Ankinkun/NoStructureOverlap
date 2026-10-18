package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Nostructureoverlap.MODID)
public class Nostructureoverlap {
    public static final String MODID = "nostructureoverlap";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Nostructureoverlap() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NoStructureOverlap ready — StructureStart bounding-box overlap prevention (1.20.1)");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NoStructureOverlap: active (3D={})", Config.use3DOverlapDetection);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        StructureBoundingBoxTracker.clearAll();
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
            StructureBoundingBoxTracker.clearDimension(serverLevel.dimension());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nostructureoverlap")
                .then(Commands.literal("status")
                        .executes(context -> {
                            int count = StructureBoundingBoxTracker.getTrackedCount();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "NoStructureOverlap: "
                                            + (Config.enableOverlapPrevention ? "enabled" : "disabled")
                                            + " | tracked: " + count
                                            + " | mode: " + (Config.use3DOverlapDetection ? "3D" : "2D")
                            ), false);
                            return count;
                        }))
                .then(Commands.literal("info")
                        .executes(context -> {
                            StringBuilder info = new StringBuilder();
                            info.append("NoStructureOverlap Info:\n");
                            info.append("• Overlap Prevention: ")
                                    .append(Config.enableOverlapPrevention ? "Enabled" : "Disabled")
                                    .append('\n');
                            info.append("• Detection: ")
                                    .append(Config.use3DOverlapDetection ? "3D bounding boxes" : "2D (X/Z)")
                                    .append('\n');
                            info.append("• Rule: different structure registry ids only\n");
                            info.append("• Log Blocked Structures: ")
                                    .append(Config.logBlockedStructures ? "Enabled" : "Disabled")
                                    .append('\n');
                            info.append("• Tracked Structures: ")
                                    .append(StructureBoundingBoxTracker.getTrackedCount())
                                    .append('\n');

                            if (!Config.structureWhitelist.isEmpty()) {
                                info.append("• Whitelist entries: ")
                                        .append(Config.structureWhitelist.size())
                                        .append('\n');
                            }
                            if (!Config.structureBlacklist.isEmpty()) {
                                info.append("• Blacklist entries: ")
                                        .append(Config.structureBlacklist.size())
                                        .append('\n');
                            }
                            if (!Config.structureSpecificEnabled.isEmpty()) {
                                info.append("• Per-structure overrides: ")
                                        .append(Config.structureSpecificEnabled.size())
                                        .append('\n');
                            }

                            context.getSource().sendSuccess(() -> Component.literal(info.toString()), false);
                            return 1;
                        }))
                .then(Commands.literal("structures")
                        .executes(context -> {
                            StringBuilder structures = new StringBuilder();
                            structures.append("Structure filters:\n");

                            if (!Config.structureWhitelist.isEmpty()) {
                                structures.append("Whitelisted: ")
                                        .append(String.join(", ", Config.structureWhitelist))
                                        .append('\n');
                            }
                            if (!Config.structureBlacklist.isEmpty()) {
                                structures.append("Blacklisted: ")
                                        .append(String.join(", ", Config.structureBlacklist))
                                        .append('\n');
                            }
                            if (!Config.structureSpecificEnabled.isEmpty()) {
                                structures.append("Overrides:\n");
                                Config.structureSpecificEnabled.forEach((id, enabled) ->
                                        structures.append("  ").append(id).append(": ")
                                                .append(enabled ? "Enabled" : "Disabled").append('\n'));
                            }

                            if (structures.toString().equals("Structure filters:\n")) {
                                structures.append("None (all registry ids participate)");
                            }

                            context.getSource().sendSuccess(
                                    () -> Component.literal(structures.toString()),
                                    false
                            );
                            return 1;
                        }))
        );
    }
}
