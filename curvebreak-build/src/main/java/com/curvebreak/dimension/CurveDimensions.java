package com.curvebreak.dimension;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CurveDimensions {
    private CurveDimensions() {}

    public static final List<CurveDimension> ALL = List.of(
        new CurveDimension("haven", "Haven", 1, 100, "A protected Curve hub and safe research city."),
        new CurveDimension("verdantia", "Verdantia", 1, 96, "An enormous living forest filled with alien growth."),
        new CurveDimension("dustreach", "Dustreach", 1, 92, "Wind-carved desert ruins hiding early Curve technology."),
        new CurveDimension("ashfall", "Ashfall", 2, 86, "A volcanic world of basalt fields, lava and ash."),
        new CurveDimension("tidelost", "Tidelost", 2, 84, "A drowned world of reefs, trenches and flooded ruins."),
        new CurveDimension("frostveil", "Frostveil", 2, 80, "A frozen wasteland where shelter matters."),
        new CurveDimension("ironreach", "Ironreach", 3, 74, "A machine world built from endless industrial ruins."),
        new CurveDimension("aetheris", "Aetheris", 3, 72, "High floating terrain and broken sky islands."),
        new CurveDimension("mycelia", "Mycelia", 3, 70, "A glowing fungal biosphere with strange ecology."),
        new CurveDimension("stormwake", "Stormwake", 3, 66, "A dark world locked beneath constant storms."),
        new CurveDimension("solaris", "Solaris", 4, 60, "A sun-scorched badland rich in high-energy materials."),
        new CurveDimension("gravemire", "Gravemire", 4, 56, "A poisoned marsh surrounding abandoned research sites."),
        new CurveDimension("eclipse", "Eclipse", 4, 50, "A world of permanent night and deep underground ruins."),
        new CurveDimension("crystal_expanse", "Crystal Expanse", 4, 47, "A cavernous realm dominated by massive crystals."),
        new CurveDimension("nullspace", "Nullspace", 5, 39, "Reality behaves incorrectly here; the Curve is visibly damaged."),
        new CurveDimension("dead_circuit", "Dead Circuit", 5, 34, "The remains of a civilization consumed by its own network."),
        new CurveDimension("shattered_sky", "Shattered Sky", 5, 28, "Chunks of terrain drift over an endless void."),
        new CurveDimension("the_rift", "The Rift", 6, 20, "Multiple worlds bleed into one unstable region."),
        new CurveDimension("the_edge", "The Edge", 7, 11, "The last stable boundary before reality breaks apart."),
        new CurveDimension("beyond", "Beyond", 7, 4, "Something exists beyond the Curve. Very little is understood.")
    );

    public static Optional<CurveDimension> find(String id) {
        String normalized = id.toLowerCase(Locale.ROOT).replace(' ', '_');
        return ALL.stream().filter(d -> d.id().equals(normalized)).findFirst();
    }

    public static List<CurveDimension> unlockedForTier(int tier) {
        return ALL.stream().filter(d -> d.requiredTier() <= tier).toList();
    }
}
