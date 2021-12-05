package net.roguelogix.phosphophyllite.quartz.internal.common.gl;

import static org.lwjgl.opengl.GL33C.*;

public class GLFence implements GLDeletable {
    
    private boolean synced = true;
    private boolean signaled = true;
    private long fence = 0;
    
    public void fence() {
        if (!synced) {
            if (!clientWait(0)) {
                // if this happened, there was a GL error, potentially unknown behavior, so im throwing
                throw new IllegalStateException();
            }
        }
        delete();
        fence = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        synced = false;
    }
    
    public boolean clientWait(long timeout) {
        if (synced) {
            return signaled;
        }
        if (timeout == 0) {
            while (!synced) {
                clientWait(1);
            }
            return signaled;
        }
        int signal = glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, timeout);
        signaled = signal == GL_ALREADY_SIGNALED || signal == GL_CONDITION_SATISFIED;
        synced = signaled || signal == GL_WAIT_FAILED;
        return signaled;
    }
    
    @Override
    public void delete() {
        glDeleteSync(fence);
        fence = 0;
        synced = true;
    }
    
    /**
     * To allow optional fencing while keeping everything non-null compatible
     */
    public static GLFence DUMMY_FENCE = new GLFence() {
        @Override
        public void fence() {
        }
        
        @Override
        public boolean clientWait(long timeout) {
            return true;
        }
    };
}
