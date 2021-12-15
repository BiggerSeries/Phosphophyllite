package net.roguelogix.phosphophyllite.quartz.internal.vk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.roguelogix.phosphophyllite.quartz.DrawBatch;
import net.roguelogix.phosphophyllite.quartz.internal.Buffer;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.GL32C.glGetInteger;
import static org.lwjgl.vulkan.KHRExternalMemoryCapabilities.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES_KHR;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.vkGetPhysicalDeviceProperties2KHR;
import static org.lwjgl.vulkan.VK10.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VKCore extends QuartzCore {
    
    public static final VKCore INSTANCE = attemptCreate();
    
    @Nullable
    public static VKCore attemptCreate() {
        if (!GLFWVulkan.glfwVulkanSupported()) {
            return null;
        }
        VkInstance instance = null;
        try (final var stack = MemoryStack.stackPush()) {
            boolean explicitDevice = false;
            final var deviceUUIDBuf = stack.malloc(GL_UUID_SIZE_EXT);
            final var driverUUIDBuf = stack.malloc(GL_UUID_SIZE_EXT);
            final var caps = GL.getCapabilities();
            if (!VKConfig.INSTANCE.useHostFrameCopy && caps.GL_EXT_semaphore && caps.GL_EXT_memory_object) {
                explicitDevice = true;
                final var devices = glGetInteger(GL_NUM_DEVICE_UUIDS_EXT);
                if (devices != 1) {
                    return null;
                }
                glGetUnsignedBytei_vEXT(GL_DEVICE_UUID_EXT, 0, deviceUUIDBuf);
                glGetUnsignedBytevEXT(GL_DRIVER_UUID_EXT, driverUUIDBuf);
            } else if (!VKConfig.INSTANCE.useHostFrameCopy) {
                return null;
            }
            
            final var appInfo = VkApplicationInfo.mallocStack();
            final var name = stack.UTF8("Quartz");
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(name);
            appInfo.applicationVersion(VK_MAKE_VERSION(0, 0, 0));
            appInfo.pEngineName(name);
            appInfo.engineVersion(VK_MAKE_VERSION(0, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_0);
            
            PointerBuffer layerPointers = null;
            if (VKConfig.INSTANCE.enableValidationLayers) {
                layerPointers = stack.mallocPointer(1);
                layerPointers.put(0, stack.UTF8("VK_LAYER_KHRONOS_validation"));
            }
            final var extensionPointers = stack.mallocPointer(2);
            extensionPointers.put(0, stack.UTF8("VK_KHR_external_memory_capabilities"));
            extensionPointers.put(1, stack.UTF8("VK_KHR_get_physical_device_properties2"));
            
            final var createInfo = VkInstanceCreateInfo.mallocStack();
            // because
            createInfo.set(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO, 0, 0, appInfo, layerPointers, extensionPointers);
            
            final var instancePointer = stack.mallocPointer(1);
            if (VK10.vkCreateInstance(createInfo, null, instancePointer) != 0) {
                QuartzCore.LOGGER.info("Failed to create Vulkan Instance");
                return null;
            }
            instance = new VkInstance(instancePointer.get(0), createInfo);
            
            final var deviceCountBuf = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, deviceCountBuf, null);
            int deviceCount = deviceCountBuf.get(0);
            if (deviceCount == 0) {
                return null;
            }
            if (!explicitDevice) {
                deviceCount = 1;
            }
            final var devicesBuf = stack.mallocPointer(deviceCount);
            vkEnumeratePhysicalDevices(instance, deviceCountBuf, devicesBuf);
            VkPhysicalDevice physicalDevice = null;
            for (int i = 0; i < deviceCount; i++) {
                final var currentPhysicalDevice = new VkPhysicalDevice(devicesBuf.get(i), instance);
                
                final var properties = VkPhysicalDeviceProperties.mallocStack();
                
                final var idProperties = VkPhysicalDeviceIDPropertiesKHR.mallocStack();
                idProperties.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES_KHR);
                
                final var properties2 = VkPhysicalDeviceProperties2KHR.mallocStack();
                properties2.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2_KHR);
                properties2.pNext(idProperties.address());
                
                vkGetPhysicalDeviceProperties(currentPhysicalDevice, properties);
                vkGetPhysicalDeviceProperties2KHR(currentPhysicalDevice, properties2);
                
                if (!explicitDevice || (idProperties.deviceUUID().compareTo(deviceUUIDBuf) == 0) && idProperties.driverUUID().compareTo(driverUUIDBuf) == 0) {
                    switch (properties.deviceType()) {
                        case VK_PHYSICAL_DEVICE_TYPE_CPU, VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU, VK_PHYSICAL_DEVICE_TYPE_OTHER -> {
                            if (explicitDevice) {
                                return null;
                            }
                            continue;
                        }
                    }
                    physicalDevice = currentPhysicalDevice;
                    break;
                }
            }
            
            if (physicalDevice == null) {
                return null;
            }
            
            final var core = new VKCore(instance, physicalDevice);
            instance = null;
            return core;
        } finally {
            if (instance != null) {
                vkDestroyInstance(instance, null);
            }
        }
    }
    
    public final VkInstance vkInstance;
    public final VkPhysicalDevice vkPhysicalDevice;
    
    VKCore(VkInstance vkInstance, VkPhysicalDevice vkPhysicalDevice) {
        this.vkInstance = vkInstance;
        this.vkPhysicalDevice = vkPhysicalDevice;
    }
    
    @Override
    protected void startupInternal() {
    
    }
    
    @Override
    protected void shutdownInternal() {
    
    }
    
    @Override
    protected void resourcesReloadedInternal() {
    
    }
    
    @Override
    public DrawBatch createDrawBatch() {
        return null;
    }
    
    @Override
    public Buffer allocBuffer() {
        return null;
    }
    
    @Override
    public void frameStart(PoseStack pMatrixStack, float pPartialTicks, long pFinishTimeNano, boolean pDrawBlockOutline, Camera pActiveRenderInfo, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pProjection) {
    
    }
    
    @Override
    public void lightUpdated() {
    
    }
    
    @Override
    public void preTerrainSetup() {
    
    }
    
    @Override
    public void preOpaque() {
    
    }
    
    @Override
    public void endOpaque() {
    
    }
    
    @Override
    public void endTranslucent() {
    
    }
}
