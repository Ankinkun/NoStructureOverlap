package com.ankin.nostructureoverlap;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Nostructureoverlap.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_OVERLAP_PREVENTION = BUILDER
            .comment("Enable structure overlap prevention using StructureStart bounding boxes.")
            .define("enableOverlapPrevention", true);

    private static final ModConfigSpec.BooleanValue LOG_BLOCKED_STRUCTURES = BUILDER
            .comment("Log when structures are blocked due to bounding-box overlap with a different structure id.")
            .define("logBlockedStructures", true);

    private static final ModConfigSpec.BooleanValue USE_3D_OVERLAP_DETECTION = BUILDER
            .comment(
                    "Use full 3D BoundingBox.intersects (default).",
                    "When false, only X/Z overlap is considered so vertically separated structures may coexist."
            )
            .define("use3DOverlapDetection", true);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_WHITELIST = BUILDER
            .comment(
                    "If non-empty, only these structure registry ids participate in overlap checks.",
                    "Example: [\"minecraft:village_plains\", \"minecraft:stronghold\"]"
            )
            .define("structureWhitelist", Arrays.asList(), Config::isValidStructureIdList);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLACKLIST = BUILDER
            .comment(
                    "Structure registry ids excluded from overlap checks (always allowed to place).",
                    "Use real registry ids such as minecraft:ruined_portal, not class names."
            )
            .define("structureBlacklist", Arrays.asList(), Config::isValidStructureIdList);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_SPECIFIC_ENABLED_RAW = BUILDER
            .comment(
                    "Per-structure participation overrides. Format: [\"structure_id=true\", \"another_id=false\"]",
                    "Example: [\"minecraft:village_plains=true\", \"minecraft:mineshaft=false\"]"
            )
            .define("structureSpecificEnabled", Arrays.asList(), Config::isValidStructureBooleanList);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableOverlapPrevention;
    public static boolean logBlockedStructures;
    public static boolean use3DOverlapDetection;
    public static Set<String> structureWhitelist;
    public static Set<String> structureBlacklist;
    public static Map<String, Boolean> structureSpecificEnabled;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableOverlapPrevention = ENABLE_OVERLAP_PREVENTION.get();
        logBlockedStructures = LOG_BLOCKED_STRUCTURES.get();
        use3DOverlapDetection = USE_3D_OVERLAP_DETECTION.get();
        structureWhitelist = new HashSet<>(STRUCTURE_WHITELIST.get());
        structureBlacklist = new HashSet<>(STRUCTURE_BLACKLIST.get());

        structureSpecificEnabled = new LinkedHashMap<>();
        for (String entry : STRUCTURE_SPECIFIC_ENABLED_RAW.get()) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                structureSpecificEnabled.put(parts[0], Boolean.parseBoolean(parts[1]));
            }
        }
    }

    private static boolean isValidStructureId(Object obj) {
        if (!(obj instanceof String str)) {
            return false;
        }
        return str.contains(":") && str.length() > 3;
    }

    private static boolean isValidStructureIdList(Object obj) {
        if (!(obj instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            if (!isValidStructureId(item)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidStructureBooleanList(Object obj) {
        if (!(obj instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            if (!(item instanceof String str)) {
                return false;
            }
            if (!str.contains("=")) {
                return false;
            }
            String[] parts = str.split("=", 2);
            if (parts.length != 2) {
                return false;
            }
            if (!isValidStructureId(parts[0])) {
                return false;
            }
            if (!parts[1].equals("true") && !parts[1].equals("false")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether this structure id should participate in overlap prevention.
     */
    public static boolean isStructureEnabled(String structureId) {
        if (structureSpecificEnabled.containsKey(structureId)) {
            return structureSpecificEnabled.get(structureId);
        }
        if (structureBlacklist.contains(structureId)) {
            return false;
        }
        if (!structureWhitelist.isEmpty()) {
            return structureWhitelist.contains(structureId);
        }
        return true;
    }
}
