package net.roguelogix.phosphophyllite.quartz.internal;

import net.roguelogix.phosphophyllite.quartz.internal.LightingManager;
import net.roguelogix.phosphophyllite.robn.ROBNObject;

import java.util.HashMap;
import java.util.Map;

/**
 * unpacked raw render data
 * passed almost directly to OpenGL
 * <p>
 * DO NOT EDIT THIS
 * this object is used to pass data between Java and C++ via ROBN
 * editing it can potentially break Quartz
 */
public class BlockRenderInfo implements ROBNObject {
    
    public int x, y, z;
    
    // -1 signals to not render that face
    // will be ignored in buffer
    public int textureIDWest = -1;
    public int textureIDEast = -1;
    public int textureIDBottom = -1;
    public int textureIDTop = -1;
    public int textureIDNorth = -1;
    public int textureIDSouth = -1;
    
    public byte textureRotationWest = 0;
    public byte textureRotationEast = 0;
    public byte textureRotationBottom = 0;
    public byte textureRotationTop = 0;
    public byte textureRotationSouth = 0;
    public byte textureRotationNorth = 0;
    
    public byte lightmapBlocklightWestLYLZ = 0x3F;
    public byte lightmapSkylightWestLYLZ = 0x3F;
    public byte AOWestLYLZ = 0;
    public byte lightmapBlocklightWestHYLZ = 0x3F;
    public byte lightmapSkylightWestHYLZ = 0x3F;
    public byte AOWestHYLZ = 0;
    public byte lightmapBlocklightWestLYHZ = 0x3F;
    public byte lightmapSkylightWestLYHZ = 0x3F;
    public byte AOWestLYHZ = 0;
    public byte lightmapBlocklightWestHYHZ = 0x3F;
    public byte lightmapSkylightWestHYHZ = 0x3F;
    public byte AOWestHYHZ = 0;
    
    public byte lightmapBlocklightEastLYLZ = 0x3F;
    public byte lightmapSkylightEastLYLZ = 0x3F;
    public byte AOEastLYLZ = 0;
    public byte lightmapBlocklightEastHYLZ = 0x3F;
    public byte lightmapSkylightEastHYLZ = 0x3F;
    public byte AOEastHYLZ = 0;
    public byte lightmapBlocklightEastLYHZ = 0x3F;
    public byte lightmapSkylightEastLYHZ = 0x3F;
    public byte AOEastLYHZ = 0;
    public byte lightmapBlocklightEastHYHZ = 0x3F;
    public byte lightmapSkylightEastHYHZ = 0x3F;
    public byte AOEastHYHZ = 0;
    
    public byte lightmapBlocklightTopLXLZ = 0x3F;
    public byte lightmapSkylightTopLXLZ = 0x3F;
    public byte AOTopLXLZ = 0;
    public byte lightmapBlocklightTopHXLZ = 0x3F;
    public byte lightmapSkylightTopHXLZ = 0x3F;
    public byte AOTopHXLZ = 0;
    public byte lightmapBlocklightTopLXHZ = 0x3F;
    public byte lightmapSkylightTopLXHZ = 0x3F;
    public byte AOTopLXHZ = 0;
    public byte lightmapBlocklightTopHXHZ = 0x3F;
    public byte lightmapSkylightTopHXHZ = 0x3F;
    public byte AOTopHXHZ = 0;
    
    public byte lightmapBlocklightBottomLXLZ = 0x3F;
    public byte lightmapSkylightBottomLXLZ = 0x3F;
    public byte AOBottomLXLZ = 0;
    public byte lightmapBlocklightBottomHXLZ = 0x3F;
    public byte lightmapSkylightBottomHXLZ = 0x3F;
    public byte AOBottomHXLZ = 0;
    public byte lightmapBlocklightBottomLXHZ = 0x3F;
    public byte lightmapSkylightBottomLXHZ = 0x3F;
    public byte AOBottomLXHZ = 0;
    public byte lightmapBlocklightBottomHXHZ = 0x3F;
    public byte lightmapSkylightBottomHXHZ = 0x3F;
    public byte AOBottomHXHZ = 0;
    
    public byte lightmapBlocklightSouthLXLY = 0x3F;
    public byte lightmapSkylightSouthLXLY = 0x3F;
    public byte AOSouthLXLY = 0x3F;
    public byte lightmapBlocklightSouthHXLY = 0x3F;
    public byte lightmapSkylightSouthHXLY = 0x3F;
    public byte AOSouthHXLY = 0x3F;
    public byte lightmapBlocklightSouthLXHY = 0x3F;
    public byte lightmapSkylightSouthLXHY = 0x3F;
    public byte AOSouthLXHY = 0x3F;
    public byte lightmapBlocklightSouthHXHY = 0x3F;
    public byte lightmapSkylightSouthHXHY = 0x3F;
    public byte AOSouthHXHY = 0x3F;
    
    public byte lightmapBlocklightNorthLXLY = 0x3F;
    public byte lightmapSkylightNorthLXLY = 0x3F;
    public byte AONorthLXLY = 0;
    public byte lightmapBlocklightNorthHXLY = 0x3F;
    public byte lightmapSkylightNorthHXLY = 0x3F;
    public byte AONorthHXLY = 0;
    public byte lightmapBlocklightNorthLXHY = 0x3F;
    public byte lightmapSkylightNorthLXHY = 0x3F;
    public byte AONorthLXHY = 0;
    public byte lightmapBlocklightNorthHXHY = 0x3F;
    public byte lightmapSkylightNorthHXHY = 0x3F;
    public byte AONorthHXHY = 0;
    
    public void textureData(StateCache.TextureInfo textureInfo) {
        textureIDWest = TextureIDMapping.idForTexture(textureInfo.textureNameWest);
        textureIDEast = TextureIDMapping.idForTexture(textureInfo.textureNameEast);
        textureIDBottom = TextureIDMapping.idForTexture(textureInfo.textureNameBottom);
        textureIDTop = TextureIDMapping.idForTexture(textureInfo.textureNameTop);
        textureIDNorth = TextureIDMapping.idForTexture(textureInfo.textureNameNorth);
        textureIDSouth = TextureIDMapping.idForTexture(textureInfo.textureNameSouth);
        
        textureRotationWest = textureInfo.textureRotationWest;
        textureRotationEast = textureInfo.textureRotationEast;
        textureRotationBottom = textureInfo.textureRotationBottom;
        textureRotationTop = textureInfo.textureRotationTop;
        textureRotationNorth = textureInfo.textureRotationNorth;
        textureRotationSouth = textureInfo.textureRotationSouth;
    }
    
    public void lightingData(LightingManager.BlockFaceLightData lightData) {
        if (lightData == null) {
            // this can happen at world load before the light manager has caught up
            return;
        }
        
        lightmapBlocklightWestLYLZ = lightData.lightmapBlocklightWestLYLZ;
        lightmapSkylightWestLYLZ = lightData.lightmapSkylightWestLYLZ;
        AOWestLYLZ = lightData.AOWestLYLZ;
        lightmapBlocklightWestHYLZ = lightData.lightmapBlocklightWestHYLZ;
        lightmapSkylightWestHYLZ = lightData.lightmapSkylightWestHYLZ;
        AOWestHYLZ = lightData.AOWestHYLZ;
        lightmapBlocklightWestLYHZ = lightData.lightmapBlocklightWestLYHZ;
        lightmapSkylightWestLYHZ = lightData.lightmapSkylightWestLYHZ;
        AOWestLYHZ = lightData.AOWestLYHZ;
        lightmapBlocklightWestHYHZ = lightData.lightmapBlocklightWestHYHZ;
        lightmapSkylightWestHYHZ = lightData.lightmapSkylightWestHYHZ;
        AOWestHYHZ = lightData.AOWestHYHZ;
        
        lightmapBlocklightEastLYLZ = lightData.lightmapBlocklightEastLYLZ;
        lightmapSkylightEastLYLZ = lightData.lightmapSkylightEastLYLZ;
        AOEastLYLZ = lightData.AOEastLYLZ;
        lightmapBlocklightEastHYLZ = lightData.lightmapBlocklightEastHYLZ;
        lightmapSkylightEastHYLZ = lightData.lightmapSkylightEastHYLZ;
        AOEastHYLZ = lightData.AOEastHYLZ;
        lightmapBlocklightEastLYHZ = lightData.lightmapBlocklightEastLYHZ;
        lightmapSkylightEastLYHZ = lightData.lightmapSkylightEastLYHZ;
        AOEastLYHZ = lightData.AOEastLYHZ;
        lightmapBlocklightEastHYHZ = lightData.lightmapBlocklightEastHYHZ;
        lightmapSkylightEastHYHZ = lightData.lightmapSkylightEastHYHZ;
        AOEastHYHZ = lightData.AOEastHYHZ;
        
        lightmapBlocklightTopLXLZ = lightData.lightmapBlocklightTopLXLZ;
        lightmapSkylightTopLXLZ = lightData.lightmapSkylightTopLXLZ;
        AOTopLXLZ = lightData.AOTopLXLZ;
        lightmapBlocklightTopHXLZ = lightData.lightmapBlocklightTopHXLZ;
        lightmapSkylightTopHXLZ = lightData.lightmapSkylightTopHXLZ;
        AOTopHXLZ = lightData.AOTopHXLZ;
        lightmapBlocklightTopLXHZ = lightData.lightmapBlocklightTopLXHZ;
        lightmapSkylightTopLXHZ = lightData.lightmapSkylightTopLXHZ;
        AOTopLXHZ = lightData.AOTopLXHZ;
        lightmapBlocklightTopHXHZ = lightData.lightmapBlocklightTopHXHZ;
        lightmapSkylightTopHXHZ = lightData.lightmapSkylightTopHXHZ;
        AOTopHXHZ = lightData.AOTopHXHZ;
        
        lightmapBlocklightBottomLXLZ = lightData.lightmapBlocklightBottomLXLZ;
        lightmapSkylightBottomLXLZ = lightData.lightmapSkylightBottomLXLZ;
        AOBottomLXLZ = lightData.AOBottomLXLZ;
        lightmapBlocklightBottomHXLZ = lightData.lightmapBlocklightBottomHXLZ;
        lightmapSkylightBottomHXLZ = lightData.lightmapSkylightBottomHXLZ;
        AOBottomHXLZ = lightData.AOBottomHXLZ;
        lightmapBlocklightBottomLXHZ = lightData.lightmapBlocklightBottomLXHZ;
        lightmapSkylightBottomLXHZ = lightData.lightmapSkylightBottomLXHZ;
        AOBottomLXHZ = lightData.AOBottomLXHZ;
        lightmapBlocklightBottomHXHZ = lightData.lightmapBlocklightBottomHXHZ;
        lightmapSkylightBottomHXHZ = lightData.lightmapSkylightBottomHXHZ;
        AOBottomHXHZ = lightData.AOBottomHXHZ;
        
        lightmapBlocklightSouthLXLY = lightData.lightmapBlocklightSouthLXLY;
        lightmapSkylightSouthLXLY = lightData.lightmapSkylightSouthLXLY;
        AOSouthLXLY = lightData.AOSouthLXLY;
        lightmapBlocklightSouthHXLY = lightData.lightmapBlocklightSouthHXLY;
        lightmapSkylightSouthHXLY = lightData.lightmapSkylightSouthHXLY;
        AOSouthHXLY = lightData.AOSouthHXLY;
        lightmapBlocklightSouthLXHY = lightData.lightmapBlocklightSouthLXHY;
        lightmapSkylightSouthLXHY = lightData.lightmapSkylightSouthLXHY;
        AOSouthLXHY = lightData.AOSouthLXHY;
        lightmapBlocklightSouthHXHY = lightData.lightmapBlocklightSouthHXHY;
        lightmapSkylightSouthHXHY = lightData.lightmapSkylightSouthHXHY;
        AOSouthHXHY = lightData.AOSouthHXHY;
        
        lightmapBlocklightNorthLXLY = lightData.lightmapBlocklightNorthLXLY;
        lightmapSkylightNorthLXLY = lightData.lightmapSkylightNorthLXLY;
        AONorthLXLY = lightData.AONorthLXLY;
        lightmapBlocklightNorthHXLY = lightData.lightmapBlocklightNorthHXLY;
        lightmapSkylightNorthHXLY = lightData.lightmapSkylightNorthHXLY;
        AONorthHXLY = lightData.AONorthHXLY;
        lightmapBlocklightNorthLXHY = lightData.lightmapBlocklightNorthLXHY;
        lightmapSkylightNorthLXHY = lightData.lightmapSkylightNorthLXHY;
        AONorthLXHY = lightData.AONorthLXHY;
        lightmapBlocklightNorthHXHY = lightData.lightmapBlocklightNorthHXHY;
        lightmapSkylightNorthHXHY = lightData.lightmapSkylightNorthHXHY;
        AONorthHXHY = lightData.AONorthHXHY;
    }
    
    @Override
    public Map<String, Object> toROBNMap() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        
        map.put("textureIDWest", textureIDWest);
        map.put("textureIDEast", textureIDEast);
        map.put("textureIDBottom", textureIDBottom);
        map.put("textureIDTop", textureIDTop);
        map.put("textureIDSouth", textureIDSouth);
        map.put("textureIDNorth", textureIDNorth);
        
        map.put("textureRotationWest", textureRotationWest);
        map.put("textureRotationEast", textureRotationEast);
        map.put("textureRotationBottom", textureRotationBottom);
        map.put("textureRotationTop", textureRotationTop);
        map.put("textureRotationSouth", textureRotationSouth);
        map.put("textureRotationNorth", textureRotationNorth);
        
        map.put("lightmapBlocklightWestLYLZ", lightmapBlocklightWestLYLZ);
        map.put("lightmapSkylightWestLYLZ", lightmapSkylightWestLYLZ);
        map.put("lightmapBlocklightWestHYLZ", lightmapBlocklightWestHYLZ);
        map.put("lightmapSkylightWestHYLZ", lightmapSkylightWestHYLZ);
        map.put("lightmapBlocklightWestLYHZ", lightmapBlocklightWestLYHZ);
        map.put("lightmapSkylightWestLYHZ", lightmapSkylightWestLYHZ);
        map.put("lightmapBlocklightWestHYHZ", lightmapBlocklightWestHYHZ);
        map.put("lightmapSkylightWestHYHZ", lightmapSkylightWestHYHZ);
        
        map.put("lightmapBlocklightEastLYLZ", lightmapBlocklightEastLYLZ);
        map.put("lightmapSkylightEastLYLZ", lightmapSkylightEastLYLZ);
        map.put("lightmapBlocklightEastHYLZ", lightmapBlocklightEastHYLZ);
        map.put("lightmapSkylightEastHYLZ", lightmapSkylightEastHYLZ);
        map.put("lightmapBlocklightEastLYHZ", lightmapBlocklightEastLYHZ);
        map.put("lightmapSkylightEastLYHZ", lightmapSkylightEastLYHZ);
        map.put("lightmapBlocklightEastHYHZ", lightmapBlocklightEastHYHZ);
        map.put("lightmapSkylightEastHYHZ", lightmapSkylightEastHYHZ);
        
        map.put("lightmapBlocklightTopLXLZ", lightmapBlocklightTopLXLZ);
        map.put("lightmapSkylightTopLXLZ", lightmapSkylightTopLXLZ);
        map.put("lightmapBlocklightTopHXLZ", lightmapBlocklightTopHXLZ);
        map.put("lightmapSkylightTopHXLZ", lightmapSkylightTopHXLZ);
        map.put("lightmapBlocklightTopLXHZ", lightmapBlocklightTopLXHZ);
        map.put("lightmapSkylightTopLXHZ", lightmapSkylightTopLXHZ);
        map.put("lightmapBlocklightTopHXHZ", lightmapBlocklightTopHXHZ);
        map.put("lightmapSkylightTopHXHZ", lightmapSkylightTopHXHZ);
        
        map.put("lightmapBlocklightBottomLXLZ", lightmapBlocklightBottomLXLZ);
        map.put("lightmapSkylightBottomLXLZ", lightmapSkylightBottomLXLZ);
        map.put("lightmapBlocklightBottomHXLZ", lightmapBlocklightBottomHXLZ);
        map.put("lightmapSkylightBottomHXLZ", lightmapSkylightBottomHXLZ);
        map.put("lightmapBlocklightBottomLXHZ", lightmapBlocklightBottomLXHZ);
        map.put("lightmapSkylightBottomLXHZ", lightmapSkylightBottomLXHZ);
        map.put("lightmapBlocklightBottomHXHZ", lightmapBlocklightBottomHXHZ);
        map.put("lightmapSkylightBottomHXHZ", lightmapSkylightBottomHXHZ);
        
        map.put("lightmapBlocklightSouthLXLY", lightmapBlocklightSouthLXLY);
        map.put("lightmapSkylightSouthLXLY", lightmapSkylightSouthLXLY);
        map.put("lightmapBlocklightSouthHXLY", lightmapBlocklightSouthHXLY);
        map.put("lightmapSkylightSouthHXLY", lightmapSkylightSouthHXLY);
        map.put("lightmapBlocklightSouthLXHY", lightmapBlocklightSouthLXHY);
        map.put("lightmapSkylightSouthLXHY", lightmapSkylightSouthLXHY);
        map.put("lightmapBlocklightSouthHXHY", lightmapBlocklightSouthHXHY);
        map.put("lightmapSkylightSouthHXHY", lightmapSkylightSouthHXHY);
        
        map.put("lightmapBlocklightNorthLXLY", lightmapBlocklightNorthLXLY);
        map.put("lightmapSkylightNorthLXLY", lightmapSkylightNorthLXLY);
        map.put("lightmapBlocklightNorthHXLY", lightmapBlocklightNorthHXLY);
        map.put("lightmapSkylightNorthHXLY", lightmapSkylightNorthHXLY);
        map.put("lightmapBlocklightNorthLXHY", lightmapBlocklightNorthLXHY);
        map.put("lightmapSkylightNorthLXHY", lightmapSkylightNorthLXHY);
        map.put("lightmapBlocklightNorthHXHY", lightmapBlocklightNorthHXHY);
        map.put("lightmapSkylightNorthHXHY", lightmapSkylightNorthHXHY);
        
        
        return map;
    }
    
    @Override
    public void fromROBNMap(Map<String, Object> map) {
    
    }
}
