package net.roguelogix.phosphophyllite.registry;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RangeDecoratorConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.FeatureDecorator;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.config.ConfigManager;
import net.roguelogix.phosphophyllite.threading.Event;
import net.roguelogix.phosphophyllite.threading.WorkQueue;
import net.roguelogix.phosphophyllite.util.VanillaFeatureWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

public class Registry {
    
    private static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Registry");
    
    private static final WorkQueue preEventWorkQueue = new WorkQueue();
    private static boolean stopWorkerThread = false;
    
    static {
        Thread thread = new Thread(() -> {
            LOGGER.info("Thread started");
            while (!stopWorkerThread) {
                try {
                    preEventWorkQueue.runAll();
                } catch (Throwable e) {
                    LOGGER.error("Exception rolled back");
                    e.printStackTrace();
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
            LOGGER.info("Thread stopped");
        });
        thread.setName("Phosphophyllite Registry Worker");
        thread.setDaemon(true);
        thread.start();
    }
    
    private final WorkQueue blockRegistrationQueue = new WorkQueue();
    private RegistryEvent.Register<Block> blockRegistryEvent;
    
    private final WorkQueue itemRegistrationQueue = new WorkQueue();
    private RegistryEvent.Register<Item> itemRegistryEvent;
    private final Event itemGroupCreationEvent = new Event();
    private CreativeModeTab itemGroup = null;
    
    private final WorkQueue fluidRegistrationQueue = new WorkQueue();
    private RegistryEvent.Register<Fluid> fluidRegistryEvent;
    
    private final WorkQueue containerRegistrationQueue = new WorkQueue();
    private RegistryEvent.Register<MenuType<?>> containerRegistryEvent;
    
    private final WorkQueue tileRegistrationQueue = new WorkQueue();
    private RegistryEvent.Register<BlockEntityType<?>> tileRegistryEvent;
    private final Map<Class<?>, LinkedList<Block>> tileBlocks = new HashMap<>();
    
    private final WorkQueue clientSetupQueue = new WorkQueue();
    private FMLClientSetupEvent clientSetupEvent;
    
    private final WorkQueue commonSetupQueue = new WorkQueue();
    private FMLCommonSetupEvent commonSetupEvent;
    
    private final ArrayList<Runnable> biomeLoadingEventHandlers = new ArrayList<>();
    private BiomeLoadingEvent biomeLoadingEvent;
    
    private final Map<String, AnnotationHandler> annotationMap = new HashMap<>();
    
    {
        annotationMap.put(RegisterBlock.class.getName(), this::registerBlockAnnotation);
        annotationMap.put(RegisterItem.class.getName(), this::registerItemAnnotation);
        annotationMap.put(RegisterFluid.class.getName(), this::registerFluidAnnotation);
        annotationMap.put(RegisterContainer.class.getName(), this::registerContainerAnnotation);
        annotationMap.put(RegisterTileEntity.class.getName(), this::registerTileEntityAnnotation);
        annotationMap.put(RegisterOre.class.getName(), this::registerWorldGenAnnotation);
        annotationMap.put(RegisterConfig.class.getName(), this::registerConfigAnnotation);
        annotationMap.put(OnModLoad.class.getName(), this::onModLoadAnnotation);
    }
    
    public Registry() {
        String callerClass = new Exception().getStackTrace()[1].getClassName();
        String callerPackage = callerClass.substring(0, callerClass.lastIndexOf("."));
        String modNamespace = callerPackage.substring(callerPackage.lastIndexOf(".") + 1);
        ModFileScanData modFileScanData = FMLLoader.getLoadingModList().getModFileById(modNamespace).getFile().getScanResult();
        
        for (ModFileScanData.AnnotationData annotation : modFileScanData.getAnnotations()) {
            AnnotationHandler handler = annotationMap.get(annotation.annotationType().getClassName());
            if (handler == null) {
                // not an annotation i handle
                continue;
            }
            String className = annotation.clazz().getClassName();
            if (className.startsWith(callerPackage)) {
                try {
                    Class<?> clazz = Registry.class.getClassLoader().loadClass(className);
                    // class loaded, so, pass it off to the handler
                    handler.run(modNamespace, clazz, annotation.memberName());
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                }
            }
        }
        
        // in case you didnt register one, this will make sure the items still get registered
        itemGroupCreationEvent.trigger();
    
        IEventBus ModBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        ModBus.addGenericListener(Block.class, this::blockRegistration);
        ModBus.addGenericListener(Item.class, this::itemRegistration);
        ModBus.addGenericListener(Fluid.class, this::fluidRegistration);
        ModBus.addGenericListener(MenuType.class, this::containerRegistration);
        ModBus.addGenericListener(BlockEntityType.class, this::tileEntityRegistration);
        
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, this::biomeLoadingEventHandler);
        ModBus.addListener(this::commonSetupEventHandler);
        
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModBus.addListener(this::clientSetupEventHandler);
        }
        
        // kill the thread when loading is done, but only if we are phos
        if (modNamespace.equals(Phosphophyllite.modid)) {
            ModBus.addListener((FMLLoadCompleteEvent e) -> preEventWorkQueue.enqueue(() -> stopWorkerThread = true));
        }
    }
    
    private void blockRegistration(RegistryEvent.Register<Block> event) {
        blockRegistryEvent = event;
        blockRegistrationQueue.runAll();
        blockRegistryEvent = null;
    }
    
    private void itemRegistration(RegistryEvent.Register<Item> event) {
        itemRegistryEvent = event;
        itemRegistrationQueue.runAll();
        itemRegistryEvent = null;
    }
    
    private void fluidRegistration(RegistryEvent.Register<Fluid> event) {
        fluidRegistryEvent = event;
        fluidRegistrationQueue.runAll();
        fluidRegistryEvent = null;
    }
    
    private void containerRegistration(RegistryEvent.Register<MenuType<?>> containerTypeRegistryEvent) {
        containerRegistryEvent = containerTypeRegistryEvent;
        containerRegistrationQueue.runAll();
        containerRegistryEvent = null;
    }
    
    private void tileEntityRegistration(RegistryEvent.Register<BlockEntityType<?>> tileEntityTypeRegister) {
        tileRegistryEvent = tileEntityTypeRegister;
        tileRegistrationQueue.runAll();
        tileRegistryEvent = null;
    }
    
    private void clientSetupEventHandler(FMLClientSetupEvent event) {
        clientSetupEvent = event;
        clientSetupQueue.runAll();
        clientSetupEvent = null;
    }
    
    private void commonSetupEventHandler(FMLCommonSetupEvent event) {
        commonSetupEvent = event;
        commonSetupQueue.runAll();
        commonSetupEvent = null;
    }
    
    private void biomeLoadingEventHandler(BiomeLoadingEvent event) {
        biomeLoadingEvent = event;
        biomeLoadingEventHandlers.forEach(Runnable::run);
        biomeLoadingEvent = null;
    }
    
    private void stalledWorker() {
        throw new IllegalThreadStateException("Registry worker thread stalled or dead, check logs");
    }
    
    private interface AnnotationHandler {
        void run(final String modNamespace, final Class<?> clazz, final String memberName);
    }
    
    private void registerBlockAnnotation(final String modNamespace, final Class<?> blockClazz, final String memberName) {
        assert blockClazz.isAnnotationPresent(RegisterBlock.class);
        
        final RegisterBlock annotation = blockClazz.getAnnotation(RegisterBlock.class);
        final Block[] blockArray = new Block[1];
        
        String modid = annotation.modid();
        if (modid.equals("")) {
            modid = modNamespace;
        }
        String name = annotation.name();
        if (modid.equals("")) {
            LOGGER.error("Unable to register block without a name");
            return;
        }
        
        if (!Block.class.isAssignableFrom(blockClazz)) {
            LOGGER.error("Attempt to register block from class not extended from Block. " + blockClazz.getSimpleName());
            return;
        }
        
        final String registryName = modid + ":" + name;
        
        final Event blockCreationEvent = preEventWorkQueue.enqueue(() -> {
            
            Constructor<?> constructor;
            try {
                constructor = blockClazz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                LOGGER.error("Failed to find default constructor to create instance of " + blockClazz.getSimpleName());
                return;
            }
            constructor.setAccessible(true);
            
            Block block;
            try {
                block = (Block) constructor.newInstance();
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
                LOGGER.error("Exception caught when instantiating instance of " + blockClazz.getSimpleName());
                e.printStackTrace();
                return;
            }
            
            for (Field declaredField : blockClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(RegisterBlock.Instance.class)) {
                    if (!declaredField.getType().isAssignableFrom(blockClazz)) {
                        LOGGER.error("Unassignable instance variable " + declaredField.getName() + " in " + blockClazz.getSimpleName());
                        continue;
                    }
                    if (!Modifier.isStatic(declaredField.getModifiers())) {
                        LOGGER.error("Cannot set non-static instance variable " + declaredField.getName() + " in " + blockClazz.getSimpleName());
                        continue;
                    }
                    declaredField.setAccessible(true);
                    try {
                        declaredField.set(null, block);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (annotation.tileEntityClass() != RegisterBlock.class) {
                tileBlocks.computeIfAbsent(annotation.tileEntityClass(), k -> new LinkedList<>()).add(block);
            }
            
            blockArray[0] = block;
        });
        blockRegistrationQueue.enqueue(() -> {
            if (!blockCreationEvent.join(10000)) {
                stalledWorker();
            }
            Block block = blockArray[0];
            if (block == null) {
                return;
            }
            block.setRegistryName(registryName);
            blockRegistryEvent.getRegistry().register(block);
        });
        clientSetupQueue.enqueue(() -> {
            if (!blockCreationEvent.join(10000)) {
                stalledWorker();
            }
            Block block = blockArray[0];
            if (block == null) {
                return;
            }
            RenderType renderType = null;
            for (Method declaredMethod : blockClazz.getDeclaredMethods()) {
                if (declaredMethod.isAnnotationPresent(RegisterBlock.RenderLayer.class)) {
                    if (Modifier.isStatic(declaredMethod.getModifiers())) {
                        LOGGER.error("Cannot call static render layer method " + declaredMethod.getName() + " in " + blockClazz.getSimpleName());
                        continue;
                    }
                    if (!RenderType.class.isAssignableFrom(declaredMethod.getReturnType())) {
                        LOGGER.error("RenderLayer annotated method " + declaredMethod.getName() + " in " + blockClazz.getSimpleName() + " does not return RenderType");
                        continue;
                    }
                    if (declaredMethod.getParameterCount() != 0) {
                        LOGGER.error("RenderLayer annotated method " + declaredMethod.getName() + " in " + blockClazz.getSimpleName() + " requires arguments");
                        continue;
                    }
                    if (renderType != null) {
                        LOGGER.error("Duplicate RenderLayer methods in " + blockClazz.getSimpleName());
                        continue;
                    }
                    declaredMethod.setAccessible(true);
                    try {
                        Object obj = declaredMethod.invoke(block);
                        renderType = (RenderType) obj;
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        continue;
                    }
                    ItemBlockRenderTypes.setRenderLayer(block, renderType);
                }
            }
        });
        if (annotation.registerItem()) {
            
            Item[] blockItemArray = new Item[1];
            blockItemArray[0] = Blocks.STONE.asItem();
            
            if (blockClazz.isAnnotationPresent(CreativeTabBlock.class)) {
                itemGroup = new CreativeModeTab(modNamespace) {
                    @Override
                    @Nonnull
                    public ItemStack makeIcon() {
                        return new ItemStack(blockItemArray[0]);
                    }
                    
                    @Override
                    public void fillItemList(@Nonnull NonNullList<ItemStack> items) {
                        super.fillItemList(items);
                        items.sort((o1, o2) -> o1.getDisplayName().getString().compareToIgnoreCase(o2.getDisplayName().getString()));
                    }
                };
                itemGroupCreationEvent.trigger();
            }
            
            itemRegistrationQueue.enqueue(() -> {
                if (!blockCreationEvent.join(10000)) {
                    stalledWorker();
                }
                Block block = blockArray[0];
                if (block == null) {
                    return;
                }
                //noinspection ConstantConditions
                blockItemArray[0] = new BlockItem(block, new Item.Properties().tab(annotation.creativeTab() ? itemGroup : null /* its fine */)).setRegistryName(registryName);
                itemRegistryEvent.getRegistry().register(blockItemArray[0]);
            });
        }
    }
    
    private void registerItemAnnotation(String modNamespace, Class<?> itemClazz, final String memberName) {
        assert itemClazz.isAnnotationPresent(RegisterItem.class);
        
        final RegisterItem annotation = itemClazz.getAnnotation(RegisterItem.class);
        final Item[] itemArray = new Item[1];
        
        String modid = annotation.modid();
        if (modid.equals("")) {
            modid = modNamespace;
        }
        String name = annotation.name();
        if (modid.equals("")) {
            LOGGER.error("Unable to register item without a name");
            return;
        }
        
        
        if (!Item.class.isAssignableFrom(itemClazz)) {
            LOGGER.error("Attempt to register item from class not extended from Item");
            return;
        }
        
        final String registryName = modid + ":" + name;
        
        final Event itemCreationEvent = preEventWorkQueue.enqueue(() -> {
            
            Constructor<?> constructor;
            try {
                constructor = itemClazz.getDeclaredConstructor(Item.Properties.class);
            } catch (NoSuchMethodException e) {
                LOGGER.error("Failed to find constructor to create instance of " + itemClazz.getSimpleName());
                return;
            }
            constructor.setAccessible(true);
            
            Item item;
            try {
                item = (Item) constructor.newInstance(new Item.Properties().tab(itemGroup));
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
                LOGGER.error("Exception caught when instantiating instance of " + itemClazz.getSimpleName());
                e.printStackTrace();
                return;
            }
            
            for (Field declaredField : itemClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(RegisterItem.Instance.class)) {
                    if (!declaredField.getType().isAssignableFrom(itemClazz)) {
                        LOGGER.error("Unassignable instance variable " + declaredField.getName() + " in " + itemClazz.getSimpleName());
                        continue;
                    }
                    if (!Modifier.isStatic(declaredField.getModifiers())) {
                        LOGGER.error("Cannot set non-static instance variable " + declaredField.getName() + " in " + itemClazz.getSimpleName());
                        continue;
                    }
                    declaredField.setAccessible(true);
                    try {
                        declaredField.set(null, item);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            itemArray[0] = item;
        }, itemGroupCreationEvent);
        
        itemRegistrationQueue.enqueue(() -> {
            if (!itemCreationEvent.join(10000)) {
                stalledWorker();
            }
            Item item = itemArray[0];
            if (item == null) {
                return;
            }
            item.setRegistryName(registryName);
            itemRegistryEvent.getRegistry().register(item);
        });
    }
    
    private void registerFluidAnnotation(String modNamespace, Class<?> fluidClazz, final String memberName) {
        assert fluidClazz.isAnnotationPresent(RegisterFluid.class);
        
        final RegisterFluid annotation = fluidClazz.getAnnotation(RegisterFluid.class);
        
        String modid = annotation.modid().equals("") ? modNamespace : annotation.modid();
        String name = annotation.name();
        if (modid.equals("")) {
            LOGGER.error("Unable to register fluid without a name");
            return;
        }
        
        
        if (!ForgeFlowingFluid.class.isAssignableFrom(fluidClazz)) {
            LOGGER.error("Attempt to register fluid from class not extended from PhosphophylliteFluid");
            return;
        }
        
        final String baseRegistryName = modid + ":" + name;
        
        PhosphophylliteFluid[] fluids = new PhosphophylliteFluid[2];
        Item[] bucketArray = new Item[1];
        LiquidBlock[] blockArray = new LiquidBlock[1];
        
        final Event fluidCreationEvent = preEventWorkQueue.enqueue(() -> {
            
            Constructor<?> constructor;
            try {
                constructor = fluidClazz.getDeclaredConstructor(ForgeFlowingFluid.Properties.class);
            } catch (NoSuchMethodException e) {
                LOGGER.error("Failed to find constructor to create instance of " + fluidClazz.getSimpleName());
                return;
            }
            
            Supplier<? extends PhosphophylliteFluid> stillSupplier = () -> fluids[0];
            Supplier<? extends PhosphophylliteFluid> flowingSupplier = () -> fluids[1];
            FluidAttributes.Builder attributes = FluidAttributes.builder(new ResourceLocation(modid, "fluid/" + name + "_still"), new ResourceLocation(modid, "fluid/" + name + "_flowing"));
//            attributes.overlay(new ResourceLocation(modid, "fluid/" + name + "_overlay"));
            attributes.color(annotation.color());
            ForgeFlowingFluid.Properties properties = new ForgeFlowingFluid.Properties(stillSupplier, flowingSupplier, attributes);
            if (annotation.registerBucket()) {
                properties.bucket(() -> bucketArray[0]);
            }
            properties.block(() -> blockArray[0]);
            
            PhosphophylliteFluid stillInstance;
            PhosphophylliteFluid flowingInstance;
            
            try {
                stillInstance = (PhosphophylliteFluid) constructor.newInstance(properties);
                flowingInstance = (PhosphophylliteFluid) constructor.newInstance(properties);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Exception caught when instantiating instance of " + fluidClazz.getSimpleName());
                e.printStackTrace();
                return;
            }
    
            LiquidBlock block = new LiquidBlock(stillSupplier, Block.Properties.of(Material.WATER).noCollission().explosionResistance(100.0F).noDrops());
            
            stillInstance.isSource = true;
            
            fluids[0] = stillInstance;
            fluids[1] = flowingInstance;
            blockArray[0] = block;
            
            for (Field declaredField : fluidClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(RegisterFluid.Instance.class)) {
                    if (!declaredField.getType().isAssignableFrom(fluidClazz)) {
                        LOGGER.error("Unassignable instance variable " + declaredField.getName() + " in " + fluidClazz.getSimpleName());
                        continue;
                    }
                    if (!Modifier.isStatic(declaredField.getModifiers())) {
                        LOGGER.error("Cannot set non-static instance variable " + declaredField.getName() + " in " + fluidClazz.getSimpleName());
                        continue;
                    }
                    declaredField.setAccessible(true);
                    try {
                        declaredField.set(null, stillInstance);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        
        fluidRegistrationQueue.enqueue(() -> {
            if (!fluidCreationEvent.join(10000)) {
                stalledWorker();
            }
            PhosphophylliteFluid still = fluids[0];
            PhosphophylliteFluid flowing = fluids[1];
            if (still == null || flowing == null) {
                return;
            }
            
            still.setRegistryName(baseRegistryName);
            flowing.setRegistryName(baseRegistryName + "_flowing");
            
            fluidRegistryEvent.getRegistry().register(still);
            fluidRegistryEvent.getRegistry().register(flowing);
        });
        
        blockRegistrationQueue.enqueue(() -> {
            if (!fluidCreationEvent.join(10000)) {
                stalledWorker();
            }
            Block block = blockArray[0];
            if (block == null) {
                return;
            }
            block.setRegistryName(baseRegistryName);
            blockRegistryEvent.getRegistry().register(block);
        });
        
        if (annotation.registerBucket()) {
            itemRegistrationQueue.enqueue(() -> {
                if (!fluidCreationEvent.join(10000)) {
                    stalledWorker();
                }
                if (fluids[0] == null) {
                    return;
                }
                BucketItem bucket = new BucketItem(() -> fluids[0], new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).tab(itemGroup));
                bucketArray[0] = bucket;
                bucket.setRegistryName(baseRegistryName + "_bucket");
                itemRegistryEvent.getRegistry().register(bucket);
            });
        }
    }
    
    private void registerContainerAnnotation(String modNamespace, Class<?> containerClazz, final String memberName) {
        assert containerClazz.isAnnotationPresent(RegisterContainer.class);
        
        final RegisterContainer annotation = containerClazz.getAnnotation(RegisterContainer.class);
        
        String modid = annotation.modid();
        if (modid.equals("")) {
            modid = modNamespace;
        }
        String name = annotation.name();
        if (modid.equals("")) {
            LOGGER.error("Unable to register container without a name");
            return;
        }
        
        
        if (!AbstractContainerMenu.class.isAssignableFrom(containerClazz)) {
            LOGGER.error("Attempt to register container from class not extended from Container");
            return;
        }
        
        final String registryName = modid + ":" + name;
        
        MenuType<?>[] containerTypeArray = new MenuType[1];
        
        final Event containerTypeCreationEvent = preEventWorkQueue.enqueue(() -> {
            ContainerSupplier supplier = null;
            for (Field declaredField : containerClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(RegisterContainer.Supplier.class)) {
                    int modifiers = declaredField.getModifiers();
                    if (!Modifier.isStatic(modifiers)) {
                        LOGGER.error("Cannot access non-static container supplier " + declaredField.getName() + " in " + containerClazz.getSimpleName());
                        return;
                    }
                    if (!Modifier.isFinal(modifiers)) {
                        LOGGER.warn("Container supplier " + declaredField.getName() + " not final in" + containerClazz.getSimpleName());
                    }
                    if (!ContainerSupplier.class.isAssignableFrom(declaredField.getType())) {
                        LOGGER.error("Supplier annotation found on non-ContainerSupplier field " + declaredField.getName() + " in " + containerClazz.getSimpleName());
                        continue;
                    }
                    if (supplier != null) {
                        LOGGER.error("Duplicate suppliers for container " + containerClazz.getSimpleName());
                        continue;
                    }
                    declaredField.setAccessible(true);
                    try {
                        supplier = (ContainerSupplier) declaredField.get(null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (supplier == null) {
                        LOGGER.error("Container supplier field " + declaredField.getName() + " null in " + containerClazz.getSimpleName());
                    }
                }
            }
            if (supplier == null) {
                LOGGER.error("No supplier found for container " + containerClazz.getSimpleName());
                return;
            }
            
            ContainerSupplier finalSupplier = supplier;
            containerTypeArray[0] = IForgeContainerType.create((windowId, playerInventory, data) -> finalSupplier.create(windowId, data.readBlockPos(), playerInventory.player));
            
            for (Field declaredField : containerClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(RegisterContainer.Type.class)) {
                    if (!declaredField.getType().isAssignableFrom(MenuType.class)) {
                        LOGGER.error("Unassignable type variable " + declaredField.getName() + " in " + containerClazz.getSimpleName());
                        continue;
                    }
                    if (!Modifier.isStatic(declaredField.getModifiers())) {
                        LOGGER.error("Cannot set non-static type variable " + declaredField.getName() + " in " + containerClazz.getSimpleName());
                        continue;
                    }
                    declaredField.setAccessible(true);
                    try {
                        declaredField.set(null, containerTypeArray[0]);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        
        containerRegistrationQueue.enqueue(() -> {
            if (!containerTypeCreationEvent.join(10000)) {
                stalledWorker();
            }
            MenuType<?> type = containerTypeArray[0];
            if (type == null) {
                return;
            }
            type.setRegistryName(registryName);
            containerRegistryEvent.getRegistry().register(type);
        });
        
        clientSetupQueue.enqueue(() -> {
            if (!containerTypeCreationEvent.join(10000)) {
                stalledWorker();
            }
            MenuType<?> type = containerTypeArray[0];
            if (type == null) {
                return;
            }
        });
    }
    
    private void registerTileEntityAnnotation(String modNamespace, Class<?> tileClazz, final String memberName) {
        assert tileClazz.isAnnotationPresent(RegisterTileEntity.class);
        
        final RegisterTileEntity annotation = tileClazz.getAnnotation(RegisterTileEntity.class);
        
        String modid = annotation.modid();
        if (modid.equals("")) {
            modid = modNamespace;
        }
        String name = annotation.name();
        if (modid.equals("")) {
            LOGGER.error("Unable to register tile type without a name");
            return;
        }
        
        if (!BlockEntity.class.isAssignableFrom(tileClazz)) {
            LOGGER.error("Attempt to register tile type from class not extended from TileEntity");
            return;
        }
        
        final String registryName = modid + ":" + name;
        
        final BlockEntityType.BlockEntitySupplier<?>[] supplier = new BlockEntityType.BlockEntitySupplier[1];
        
        final Event supplierLookupEvent = preEventWorkQueue.enqueue(() -> {
            for (Field declaredField : tileClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(RegisterTileEntity.Supplier.class)) {
                    int modifiers = declaredField.getModifiers();
                    if (!Modifier.isStatic(modifiers)) {
                        LOGGER.error("Cannot access non-static tile supplier " + declaredField.getName() + " in " + tileClazz.getSimpleName());
                        return;
                    }
                    if (!Modifier.isFinal(modifiers)) {
                        LOGGER.warn("Tile supplier " + declaredField.getName() + " not final in" + tileClazz.getSimpleName());
                    }
                    if (!BlockEntityType.BlockEntitySupplier.class.isAssignableFrom(declaredField.getType())) {
                        LOGGER.error("Supplier annotation found on non-TileSupplier field " + declaredField.getName() + " in " + tileClazz.getSimpleName());
                        continue;
                    }
                    if (supplier[0] != null) {
                        LOGGER.error("Duplicate suppliers for tile " + tileClazz.getSimpleName());
                        continue;
                    }
                    declaredField.setAccessible(true);
                    try {
                        supplier[0] = (BlockEntityType.BlockEntitySupplier<?>) declaredField.get(null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (supplier[0] == null) {
                        LOGGER.error("Tile supplier field " + declaredField.getName() + " null in " + tileClazz.getSimpleName());
                    }
                }
            }
            if (supplier[0] == null) {
                LOGGER.error("No supplier found for tile " + tileClazz.getSimpleName());
            }
        });
        
        tileRegistrationQueue.enqueue(() -> {
            if (!supplierLookupEvent.join(10000)) {
                stalledWorker();
            }
            
            if (supplier[0] == null) {
                return;
            }
            
            LinkedList<Block> blocks = tileBlocks.remove(tileClazz);
            
            if (blocks == null) {
                return;
            }
            
            // fuck you java, its the correct size here
            @SuppressWarnings({"ConstantConditions", "ToArrayCallWithZeroLengthArrayArgument"})
            BlockEntityType<?> type = BlockEntityType.Builder.of(supplier[0], blocks.toArray(new Block[blocks.size()])).build(null);
            
            type.setRegistryName(registryName);
            
            boolean typeSet = false;
            for (Field declaredField : tileClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(RegisterTileEntity.Type.class)) {
                    if (!declaredField.getType().isAssignableFrom(BlockEntityType.class)) {
                        LOGGER.error("Unassignable type variable " + declaredField.getName() + " in " + tileClazz.getSimpleName());
                        continue;
                    }
                    if (!Modifier.isStatic(declaredField.getModifiers())) {
                        LOGGER.error("Cannot set non-static type variable " + declaredField.getName() + " in " + tileClazz.getSimpleName());
                        continue;
                    }
                    if (typeSet) {
                        LOGGER.warn("Duplicate TileEntityType fields in " + tileClazz.getSimpleName());
                    }
                    declaredField.setAccessible(true);
                    try {
                        declaredField.set(null, type);
                        typeSet = true;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (!typeSet) {
                throw new IllegalStateException("Tile entity type unable to be saved for " + tileClazz.getSimpleName());
            }
            tileRegistryEvent.getRegistry().register(type);
        });
    }
    
    private void registerWorldGenAnnotation(String modNamespace, Class<?> oreClazz, final String memberName) {
        assert oreClazz.isAnnotationPresent(RegisterOre.class);
        
        if (!oreClazz.isAnnotationPresent(RegisterBlock.class)) {
            LOGGER.error("Cannot register unknown block for world generation");
            return;
        }
        
        final RegisterBlock blockAnnotation = oreClazz.getAnnotation(RegisterBlock.class);
        
        // dont need to error check because RegisterBlock handles that for us
        String modid = blockAnnotation.modid();
        if (modid.equals("")) {
            modid = modNamespace;
        }
        String name = blockAnnotation.name();
        
        final String registryName = modid + ":" + name;
        final ResourceLocation resourceLocation = new ResourceLocation(registryName);
        
        final VanillaFeatureWrapper<?, ?>[] wrapperArray = new VanillaFeatureWrapper<?, ?>[1];
        final Block[] oreInstanceArray = new Block[1];
        final HashSet<String> spawnBiomes = new HashSet<>();
        
        commonSetupQueue.enqueue(() -> {
            Block oreInstance = ForgeRegistries.BLOCKS.getValue(resourceLocation);
            if (!(oreInstance instanceof IPhosphophylliteOre)) {
                LOGGER.error("Attempt to register non-IPhosphophylliteOre block for world generation");
                return;
            }
            IPhosphophylliteOre oreInfo = (IPhosphophylliteOre) oreInstance;
            
            RuleTest fillerBlock = oreInfo.isNetherOre() ? OreConfiguration.Predicates.NETHERRACK : OreConfiguration.Predicates.NATURAL_STONE;
            
            
            ConfiguredFeature<?, ?> feature = Feature.ORE
                    .configured(new OreConfiguration(fillerBlock, oreInstance.defaultBlockState(), oreInfo.size()))
                    .decorated(FeatureDecorator.RANGE.configured(new RangeDecoratorConfiguration(UniformHeight.of(VerticalAnchor.absolute(oreInfo.minLevel()), VerticalAnchor.absolute(oreInfo.maxLevel())))))
                    .squared()
                    .count(oreInfo.count());
            
            boolean doSpawn = oreInfo.doSpawn();
            
            VanillaFeatureWrapper<?, ?> wrapper = new VanillaFeatureWrapper<>(feature, () -> doSpawn);
            
            wrapperArray[0] = wrapper;
            oreInstanceArray[0] = oreInstance;
            spawnBiomes.addAll(Arrays.asList(oreInfo.spawnBiomes()));
            commonSetupEvent.enqueueWork(() -> net.minecraft.core.Registry.register(net.minecraft.core.RegistryAccess.builtin().registryOrThrow(net.minecraft.core.Registry.CONFIGURED_FEATURE_REGISTRY), resourceLocation, wrapper));
        });
        biomeLoadingEventHandlers.add(() -> {
            
            IPhosphophylliteOre oreInfo = (IPhosphophylliteOre) oreInstanceArray[0];
            
            if ((biomeLoadingEvent.getCategory() == Biome.BiomeCategory.NETHER) != oreInfo.isNetherOre()) {
                return;
            }
            if (spawnBiomes.size() > 0) {
                if (!spawnBiomes.contains(biomeLoadingEvent.getName().toString())) {
                    return;
                }
            }
            
            biomeLoadingEvent.getGeneration().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).add(() -> wrapperArray[0]);
        });
    }
    
    private void registerConfigAnnotation(String modNamespace, Class<?> configClazz, final String memberName) {
        ConfigManager.registerConfig(configClazz, modNamespace);
    }
    
    private void onModLoadAnnotation(String modNamespace, Class<?> modLoadClazz, final String memberName) {
        try {
            Method method = modLoadClazz.getDeclaredMethod(memberName.substring(0, memberName.indexOf('(')));
            if(!Modifier.isStatic(method.getModifiers())){
                LOGGER.error("Cannot call non-static @OnModLoad method " + method.getName() + " in " + modLoadClazz.getSimpleName());
                return;
            }
            
            if(method.getParameterCount() != 0){
                LOGGER.error("Cannot call @OnModLoad method with parameters " + method.getName() + " in " + modLoadClazz.getSimpleName());
                return;
            }
            
            method.setAccessible(true);
            method.invoke(null);
            
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
