package net.roguelogix.phosphophyllite.threading;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.roguelogix.phosphophyllite.registry.OnModLoad;

public class Queues {
    public static final WorkQueue serverThread = new WorkQueue();
    public static final WorkQueue offThread = new WorkQueue();
    
    @OnModLoad
    private static void onModLoad() {
        int threads = Runtime.getRuntime().availableProcessors();
        threads = Math.max(1, threads - 1); // if possible, leave a core for the main server threads
        if (FMLEnvironment.dist == Dist.CLIENT) {
            threads = Math.max(1, threads - 1); // if possible, leave a core for the main client thread too
        }
        offThread.addProcessingThreads(threads, "Phosphophyllite OffThread Queue Worker Thread #");
    }
}
