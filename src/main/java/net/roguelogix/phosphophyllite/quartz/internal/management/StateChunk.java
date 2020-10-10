package net.roguelogix.phosphophyllite.quartz.internal.management;

import net.roguelogix.phosphophyllite.quartz.api.QuartzState;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

class StateChunk {
    private final Vector3ic basePosition;
    private final QuartzState[][][] states = new QuartzState[16][16][16];
    private final StateCache.TextureInfo[][][] textureInfos = new StateCache.TextureInfo[16][16][16];
    
    StateChunk(Vector3ic basePosition) {
        this.basePosition = new Vector3i(basePosition);
    }
    
    void setState(Vector3i position, QuartzState state) {
        position.sub(basePosition);
        if (position.maxComponent() > 15) {
            position.add(basePosition);
            return;
        }
        states[position.x][position.y][position.z] = state;
        position.add(basePosition);
    }
    
    void updateStates() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    QuartzState state = states[x][y][z];
                    StateCache.TextureInfo info = null;
                    if(state != null){
                        info = StateCache.infoForState(state);
                    }
                    textureInfos[x][y][z] = info;
                }
            }
        }
    }
    
    StateCache.TextureInfo getTextureInfo(Vector3i position) {
        position.sub(basePosition);
        if (position.maxComponent() > 15) {
            position.add(basePosition);
            return null;
        }
        StateCache.TextureInfo info = textureInfos[position.x][position.y][position.z];
        position.add(basePosition);
        return info;
    }
    
    boolean isEmpty() {
        for (QuartzState[][] state : states) {
            for (QuartzState[] quartzStates : state) {
                for (QuartzState quartzState : quartzStates) {
                    if (quartzState != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
