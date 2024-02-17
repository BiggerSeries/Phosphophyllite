package net.roguelogix.phosphophyllite.capability;

import com.mojang.datafixers.util.Function3;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

// basically a copy of BlockCapabilityCache, but allows the use of a wrapper function instead
@NonnullDefault
public class CachedWrappedBlockCapability<T, C> {
    
    private final Function3<ServerLevel, BlockPos, C, T> fetchFunction;
    private final ServerLevel level;
    private final BlockPos pos;
    private final C context;
    
    /**
     * {@code true} if notifications received by the cache will be forwarded to {@link #listener}.
     * By default and after each invalidation, this is set to {@code false}.
     * Calling {@link #getCapability()} sets it to {@code true}.
     */
    private boolean cacheValid = false;
    @Nullable
    private T cachedCap = null;
    
    private boolean canQuery = true;
    private final ICapabilityInvalidationListener listener;
    
    public CachedWrappedBlockCapability(Function3<ServerLevel, BlockPos, C, T> fetchFunction, ServerLevel level, BlockPos pos, C context, BooleanSupplier isValid, Runnable invalidationListener) {
        this.fetchFunction = fetchFunction;
        this.level = level;
        this.pos = pos;
        this.context = context;
        
        this.listener = () -> {
            if (!cacheValid) {
                // already invalidated, just check if the cache should be removed
                return isValid.getAsBoolean();
            }
            
            // disable queries for now
            canQuery = false;
            // mark cached cap as invalid
            cacheValid = false;
            
            if (isValid.getAsBoolean()) {
                // notify
                invalidationListener.run();
                // re-enable queries
                canQuery = true;
                return true;
            } else {
                // not valid anymore: keep queries disabled and return false
                return false;
            }
        };
        level.registerCapabilityListener(pos, listener);
    }
    
    public ServerLevel level() {
        return level;
    }
    
    public BlockPos pos() {
        return pos;
    }
    
    public C context() {
        return context;
    }
    
    /**
     * Gets the capability instance, or {@code null} if the capability is not present.
     *
     * <p>If {@linkplain #pos() the target position} is not loaded, this method will return {@code null}.
     */
    @Nullable
    public T getCapability() {
        if (!canQuery) {
            throw new IllegalStateException("Do not call getCapability on an invalid cache or from the invalidation listener!");
        }
        
        if (!cacheValid) {
            if (!level.isLoaded(pos)) {
                // If the position is not loaded, return no capability for now.
                // The cache will be invalidated when the chunk is loaded.
                cachedCap = null;
            } else {
                cachedCap = fetchFunction.apply(level, pos, context);
            }
            cacheValid = true;
        }
        
        return cachedCap;
    }
}
