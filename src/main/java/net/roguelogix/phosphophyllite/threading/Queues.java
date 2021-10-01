package net.roguelogix.phosphophyllite.threading;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.roguelogix.phosphophyllite.registry.OnModLoad;

public class Queues {
    public static final WorkQueue serverThread;
    public static final WorkQueue offThread;
    
    @OnModLoad
    private static void onModLoad() {
    }
    
    static {
        WorkQueue serverThread1 = null;
        int threads = Runtime.getRuntime().availableProcessors();
        threads = Math.max(1, threads - 1); // if possible, leave a core for the main server threads
        try {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                threads = Math.max(1, threads - 1); // if possible, leave a core for the main client thread too
            }
            serverThread1 = new WorkQueue();
        }catch (NoClassDefFoundError ignored){
            // happens when forge isn't loaded
        }
        serverThread = serverThread1;
        offThread = new WorkQueue();
        offThread.addProcessingThreads(threads, "Phosphophyllite OffThread Queue Worker Thread #");
    }
}
