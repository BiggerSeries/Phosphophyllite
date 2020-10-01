package net.roguelogix.phosphophyllite.quartz.internal;

import java.util.HashMap;

public class TextureIDMapping {
    
    private static HashMap<String, Integer> idMap = new HashMap<>();
    
    public static int idForTexture(String textureName) {
        if(textureName == null){
            return -1;
        }
        return idMap.computeIfAbsent(textureName, (k) -> Renderer.INSTANCE.loadTexture(textureName));
    }
}
