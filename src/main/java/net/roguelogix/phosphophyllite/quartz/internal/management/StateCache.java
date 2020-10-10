package net.roguelogix.phosphophyllite.quartz.internal.management;

import net.minecraft.util.ResourceLocation;
import net.roguelogix.phosphophyllite.quartz.api.QuartzState;
import net.roguelogix.phosphophyllite.repack.tnjson.TnJson;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nonnull;
import java.util.*;

public class StateCache {
    
    public static class TextureInfo {
        public String textureNameWest = null;
        public String textureNameEast = null;
        public String textureNameBottom = null;
        public String textureNameTop = null;
        public String textureNameSouth = null;
        public String textureNameNorth = null;
        
        public byte textureRotationWest = 0;
        public byte textureRotationEast = 0;
        public byte textureRotationBottom = 0;
        public byte textureRotationTop = 0;
        public byte textureRotationSouth = 0;
        public byte textureRotationNorth = 0;
    }
    
    private static final HashMap<String, HashMap<HashMap<String, String>, TextureInfo>> infoCache = new HashMap<>();
    
    public static TextureInfo infoForState(@Nonnull QuartzState state) {
        HashMap<HashMap<String, String>, TextureInfo> mapCache = infoCache.computeIfAbsent(state.blockName, (k) -> new HashMap<>());
        return mapCache.computeIfAbsent(state.values, (k) -> loadStateInfo(state));
    }
    
    private static TextureInfo loadStateInfo(QuartzState state) {
        Map<String, Object> jsonMap = loadJSON(state.blockName);
        
        TextureInfo textureInfo = new TextureInfo();
        
        Stack<Map<String, Object>> toProcess = new Stack<>();
        toProcess.push(jsonMap);
        
        while (!toProcess.isEmpty()) {
            Map<String, Object> processingMap = toProcess.pop();
            Map<String, Object> requiredState = (Map<String, Object>) processingMap.get("state");
            if (requiredState != null) {
                for (String s : requiredState.keySet()) {
                    Object stateObject = requiredState.get(s);
                    if (stateObject instanceof String) {
                        String stateString = (String) stateObject;
                        if (!state.values.getOrDefault(s, "").equals(stateString)) {
                            continue;
                        }
                    }
                }
            }
            
            List<Object> subBranches = (List<Object>) processingMap.get("branches");
            if (subBranches != null) {
                // reverse order so it will process the first one, well, first, when it pops them off
                for (int i = subBranches.size() - 1; i >= 0; i--) {
                    toProcess.push((Map<String, Object>) subBranches.get(i));
                }
            }
            
            for (String s : processingMap.keySet()) {
                Object o = processingMap.get(s);
                switch (s.toLowerCase()) {
                    case "textures": {
                        if (o instanceof Map) {
                            Map<String, Object> textureMap = (Map<String, Object>) o;
                            for (String textureLocation : textureMap.keySet()) {
                                switch (textureLocation.toLowerCase()) {
                                    case "all": {
                                        String textureName = (String) textureMap.get(textureLocation);
                                        textureInfo.textureNameWest = textureName;
                                        textureInfo.textureNameEast = textureName;
                                        textureInfo.textureNameBottom = textureName;
                                        textureInfo.textureNameTop = textureName;
                                        textureInfo.textureNameSouth = textureName;
                                        textureInfo.textureNameNorth = textureName;
                                        break;
                                    }
                                    case "west": {
                                        textureInfo.textureNameWest = (String) textureMap.get(textureLocation);
                                        break;
                                    }
                                    case "east": {
                                        textureInfo.textureNameEast = (String) textureMap.get(textureLocation);
                                        break;
                                    }
                                    case "top": {
                                        textureInfo.textureNameTop = (String) textureMap.get(textureLocation);
                                        break;
                                    }
                                    case "bottom": {
                                        textureInfo.textureNameBottom = (String) textureMap.get(textureLocation);
                                        break;
                                    }
                                    case "south": {
                                        textureInfo.textureNameSouth = (String) textureMap.get(textureLocation);
                                        break;
                                    }
                                    case "north": {
                                        textureInfo.textureNameNorth = (String) textureMap.get(textureLocation);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case "rotations": {
                        if (o instanceof Map) {
                            Map<String, Object> textureMap = (Map<String, Object>) o;
                            for (String rotationLocation : textureMap.keySet()) {
                                switch (rotationLocation.toLowerCase()) {
                                    case "all": {
                                        byte textureRotation = (byte) textureMap.get(rotationLocation);
                                        textureInfo.textureRotationWest = textureRotation;
                                        textureInfo.textureRotationEast = textureRotation;
                                        textureInfo.textureRotationBottom = textureRotation;
                                        textureInfo.textureRotationTop = textureRotation;
                                        textureInfo.textureRotationSouth = textureRotation;
                                        textureInfo.textureRotationNorth = textureRotation;
                                        break;
                                    }
                                    case "west": {
                                        textureInfo.textureRotationWest = (byte) textureMap.get(rotationLocation);
                                        break;
                                    }
                                    case "east": {
                                        textureInfo.textureRotationEast = (byte) textureMap.get(rotationLocation);
                                        break;
                                    }
                                    case "top": {
                                        textureInfo.textureRotationTop = (byte) textureMap.get(rotationLocation);
                                        break;
                                    }
                                    case "bottom": {
                                        textureInfo.textureRotationBottom = (byte) textureMap.get(rotationLocation);
                                        break;
                                    }
                                    case "south": {
                                        textureInfo.textureRotationSouth = (byte) textureMap.get(rotationLocation);
                                        break;
                                    }
                                    case "north": {
                                        textureInfo.textureRotationNorth = (byte) textureMap.get(rotationLocation);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case "end": {
                        toProcess.clear();
                        break;
                    }
                }
            }
        }
        
        return textureInfo;
    }
    
    public static Set<String> allTexturesUsedByStateJSON(String jsonName) {
        Set<String> textureSet = new HashSet<>();
        
        Stack<Map<String, Object>> toProcess = new Stack<>();
        toProcess.push(loadJSON(jsonName));
        
        while (!toProcess.empty()) {
            Map<String, Object> map = toProcess.pop();
            if(map.containsKey("branches")){
                Object branches = map.get("branches");
                if(branches instanceof List){
                    List<?> branchesList = (List<?>) branches;
                    for (Object o : branchesList) {
                        if(o instanceof Map){
                            Map<String, Object> branchMap = (Map<String, Object>) o;
                            toProcess.push(branchMap);
;                        }
                    }
                }
            }
            
            if(map.containsKey("textures")){
                Object textures = map.get("textures");
                if(textures instanceof Map){
                    Map<String, Object> texturesMap = (Map<String, Object>) textures;
                    for (String s : texturesMap.keySet()) {
                        Object textureLocation = texturesMap.get(s);
                        if(textureLocation instanceof String){
                            textureSet.add((String) textureLocation);
                        }
                    }
                }
            }
        }
        
        return textureSet;
    }
    
    private static final HashMap<String, Map<String, Object>> jsonCache = new HashMap<>();
    
    private static Map<String, Object> loadJSON(String jsonName) {
        return jsonCache.computeIfAbsent(jsonName, (k) ->{
            String str = Util.readTextResourceLocation(new ResourceLocation(jsonName + ".json5"));
            return TnJson.parse(str);
        });
    }
}
