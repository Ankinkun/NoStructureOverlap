package com.ankin.nostructureoverlap;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

@Mod(Nostructureoverlap.MODID)
public class Nostructureoverlap {
    public static final String MODID = "nostructureoverlap";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Nostructureoverlap(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NoStructureOverlap loaded — StructureStart bounding-box overlap prevention ready");
        LOGGER.info("Uses MiniHUD-equivalent boxes after piece layout; blocks different structure ids only");
        LOGGER.info("Use /nostructureoverlap status to inspect tracked structures");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do not clear the tracker here: spawn chunk STRUCTURE_STARTS may already be registered.
        LOGGER.info("NoStructureOverlap: Server starting — BB overlap prevention active (3D={})",
                Config.use3DOverlapDetection);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        StructurePlacementValidator.clearStructures();
        LOGGER.info("NoStructureOverlap: Server stopped — cleared tracked structures");
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
            StructurePlacementValidator.clearDimension(serverLevel.dimension());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nostructureoverlap")
                .then(Commands.literal("status")
                        .executes(context -> {
                            int structureCount = StructurePlacementValidator.getStructureCount();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "NoStructureOverlap Status: "
                                            + (Config.enableOverlapPrevention ? "Enabled" : "Disabled")
                                            + " | Tracked Structures: " + structureCount
                                            + " | Mode: " + (Config.use3DOverlapDetection ? "3D BB" : "2D BB")
                            ), false);
                            return structureCount;
                        }))
                .then(Commands.literal("clear")
                        .executes(context -> {
                            StructurePlacementValidator.clearStructures();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Cleared all tracked structure bounding boxes"),
                                    true
                            );
                            return 1;
                        }))
                .then(Commands.literal("toggle")
                        .executes(context -> {
                            Config.enableOverlapPrevention = !Config.enableOverlapPrevention;
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Overlap prevention "
                                            + (Config.enableOverlapPrevention ? "enabled" : "disabled")
                            ), true);
                            return 1;
                        }))
                .then(Commands.literal("toggle3D")
                        .executes(context -> {
                            Config.use3DOverlapDetection = !Config.use3DOverlapDetection;
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Overlap detection "
                                            + (Config.use3DOverlapDetection
                                            ? "3D (full BoundingBox.intersects)"
                                            : "2D (X/Z only)")
                            ), true);
                            return 1;
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
                                    .append(StructurePlacementValidator.getStructureCount())
                                    .append('\n');

                            if (!Config.structureWhitelist.isEmpty()) {
                                info.append("• Whitelisted Structures: ")
                                        .append(Config.structureWhitelist.size())
                                        .append('\n');
                            }
                            if (!Config.structureBlacklist.isEmpty()) {
                                info.append("• Blacklisted Structures: ")
                                        .append(Config.structureBlacklist.size())
                                        .append('\n');
                            }
                            if (!Config.structureSpecificEnabled.isEmpty()) {
                                info.append("• Custom Enabled/Disabled Structures: ")
                                        .append(Config.structureSpecificEnabled.size())
                                        .append('\n');
                            }

                            context.getSource().sendSuccess(() -> Component.literal(info.toString()), false);
                            return 1;
                        }))
                .then(Commands.literal("structures")
                        .executes(context -> {
                            StringBuilder structures = new StringBuilder();
                            structures.append("Structure Configuration:\n");

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
                                structures.append("Custom Enabled/Disabled:\n");
                                Config.structureSpecificEnabled.forEach((id, enabled) ->
                                        structures.append("  ").append(id).append(": ")
                                                .append(enabled ? "Enabled" : "Disabled").append('\n'));
                            }

                            if (structures.toString().equals("Structure Configuration:\n")) {
                                structures.append(
                                        "No structure-specific filters (all registry ids participate)"
                                );
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
