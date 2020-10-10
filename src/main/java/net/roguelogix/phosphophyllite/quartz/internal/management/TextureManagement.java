package net.roguelogix.phosphophyllite.quartz.internal.management;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.roguelogix.phosphophyllite.quartz.api.IQuartzTile;
import net.roguelogix.phosphophyllite.quartz.internal.rendering.Renderer;

import java.util.*;

public class TextureManagement {
    
    private static final Map<String, Integer> textureIDMap = new HashMap<>();
    
    public static int getTextureID(String textureLocation) {
        if (textureLocation == null) {
            return 0;
        }
        Integer id = textureIDMap.get(textureLocation);
        if (id == null) {
            // this just means you fucked up, or i did, one or the other
            return 0;
        }
        return id;
    }
    
    public static void loadFallbackTexture() {
        // im just gonna assume this cant fail (it can, technically, but it probably wont)
        ArrayList<String> texture = new ArrayList<>(1);
        texture.add("phosphophyllite:textures/block/fallback_texture.png");
        Map<String, Integer> textureMap = Renderer.INSTANCE.loadTextures(texture);
        if (textureMap.size() != 1 || textureMap.get(texture.get(0)) != 0) {
            throw new IllegalStateException("Failed to load fallback texture");
        }
        textureIDMap.putAll(textureMap);
    }
    
    public static void loadTexturesForTiles() {
        // TODO: maybe use blocks? most are 1:1 with tiles, mine are at least
        //       also has side benefit that i wouldn't create more instances then
        
        // load textures from the quartzstates given by the tiles that implement IQuartzTile
        
        Set<String> textures = new HashSet<>();
        
        for (TileEntityType<? extends TileEntity> value : ForgeRegistries.TILE_ENTITIES.getValues()) {
            TileEntity tile = value.create();
            if (tile instanceof IQuartzTile) {
                textures.addAll(StateCache.allTexturesUsedByStateJSON(((IQuartzTile) tile).getStateJSONLocation().toString()));
            }
        }
        textures.forEach(s->{
            EventHandling.LOGGER.debug("loading texture: " + s);
        });
        EventHandling.LOGGER.debug("attempting to load " + textures.size() + " textures");
        Map<String, Integer> ids = Renderer.INSTANCE.loadTextures(Collections.list(Collections.enumeration(textures)));
        int preSize = textureIDMap.size();
        textureIDMap.putAll(ids);
        EventHandling.LOGGER.debug("loaded " + (textureIDMap.size() - preSize) + " new textures");
        ids.forEach((name, id) -> {
            if(id == 0){
                EventHandling.LOGGER.warn("failed to load texture: " + name);
            }else{
                EventHandling.LOGGER.debug("loaded texture: " + name + ", with ID: " + id);
            }
        });
    }
}
