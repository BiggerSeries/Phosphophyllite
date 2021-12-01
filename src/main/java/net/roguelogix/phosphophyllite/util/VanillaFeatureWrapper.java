package net.roguelogix.phosphophyllite.util;


import javax.annotation.Nonnull;
import java.util.Random;
import java.util.function.BooleanSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class VanillaFeatureWrapper<Config extends FeatureConfiguration, FeatureC extends Feature<Config>> extends ConfiguredFeature<Config, FeatureC> {
    
    final private BooleanSupplier test;
    
    public VanillaFeatureWrapper(ConfiguredFeature<Config, FeatureC> wrap, BooleanSupplier cannotRun) {
        super(wrap.feature, wrap.config);
        test = cannotRun;
    }
    
    @Nonnull
    public ConfiguredFeature<?, ?> decorated(@Nonnull ConfiguredFeature<?, ?> feature) {
        return new VanillaFeatureWrapper<>(feature, test);
    }
    
    public boolean place(@Nonnull WorldGenLevel reader, @Nonnull ChunkGenerator generator, @Nonnull Random rand, @Nonnull BlockPos pos) {
        if (!test.getAsBoolean())
            return false;
        return super.place(reader, generator, rand, pos);
    }
}
