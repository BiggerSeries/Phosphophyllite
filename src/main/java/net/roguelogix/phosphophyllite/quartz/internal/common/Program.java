package net.roguelogix.phosphophyllite.quartz.internal.common;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Program implements GLObject, GLReloadable {
    private final ArrayList<Consumer<Program>> reloadCallbacks = new ArrayList<>();
    
    @Nullable
    private final String prepend;
    public final ResourceLocation baseResourceLocation;
    private final ResourceLocation vertexShaderLocation;
    private final ResourceLocation fragmentShaderLocation;
    private final ResourceLocation computeShaderLocation;
    
    private int programID = 0;
    
    public Program(ResourceLocation location, @Nullable String prepend) {
        if (prepend != null && prepend.isEmpty()) {
            prepend = null;
        }
        this.prepend = prepend;
        
        final String namespace = location.getNamespace();
        baseResourceLocation = location;
        vertexShaderLocation = new ResourceLocation(namespace, location.getPath() + ".vert");
        fragmentShaderLocation = new ResourceLocation(namespace, location.getPath() + ".frag");
        computeShaderLocation = new ResourceLocation(namespace, location.getPath() + ".comp");
        reload();
    }
    
    public Program(ResourceLocation location) {
        this(location, null);
    }
    
    public void reload() {
        
        int vertexShader = 0;
        int fragmentShader = 0;
        int computeShader = 0;
        int newProgramID = 0;
        
        try {
            
            var vertexShaderCode = Util.readResourceLocation(vertexShaderLocation);
            var fragmentShaderCode = Util.readResourceLocation(fragmentShaderLocation);
            var computeShaderCode = Util.readResourceLocation(computeShaderLocation);
            
            if (vertexShaderCode != null || fragmentShaderCode != null) {
                if (vertexShaderCode == null) {
                    throw new IllegalStateException("Unable to load vertex shader code from " + vertexShaderLocation);
                }
                if (fragmentShaderCode == null) {
                    throw new IllegalStateException("Unable to load vertex shader code from " + fragmentShaderLocation);
                }
            } else if (computeShaderCode == null) {
                throw new IllegalStateException("Unable to load any shader code from " + baseResourceLocation);
            }
            
            
            if (prepend != null) {
                if(vertexShaderCode != null) {
                    vertexShaderCode = new StringBuilder(vertexShaderCode).insert(vertexShaderCode.indexOf('\n') + 1, prepend).toString();
                    fragmentShaderCode = new StringBuilder(fragmentShaderCode).insert(fragmentShaderCode.indexOf('\n') + 1, prepend).toString();
                }
                if(computeShaderCode != null){
                    computeShaderCode = new StringBuilder(computeShaderCode).insert(computeShaderCode.indexOf('\n') + 1, prepend).toString();
                }
            }
            
            if (vertexShaderCode != null) {
                vertexShader = glCreateShader(GL_VERTEX_SHADER);
                glShaderSource(vertexShader, vertexShaderCode);
                glCompileShader(vertexShader);
                int compileStatus = glGetShaderi(vertexShader, GL_COMPILE_STATUS);
                if (compileStatus != GL_TRUE) {
                    throw new IllegalStateException("Vertex shader compilation failed for " + vertexShaderLocation + "\n" + glGetShaderInfoLog(vertexShader));
                }
                
                fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
                glShaderSource(fragmentShader, fragmentShaderCode);
                glCompileShader(fragmentShader);
                compileStatus = glGetShaderi(fragmentShader, GL_COMPILE_STATUS);
                if (compileStatus != GL_TRUE) {
                    throw new IllegalStateException("Fragment shader compilation failed for " + fragmentShaderLocation + "\n" + glGetShaderInfoLog(fragmentShader));
                }
            }
            
            if (computeShaderCode != null) {
                computeShader = glCreateShader(GL_COMPUTE_SHADER);
                glShaderSource(computeShader, computeShaderCode);
                glCompileShader(computeShader);
                int compileStatus = glGetShaderi(computeShader, GL_COMPILE_STATUS);
                if (compileStatus != GL_TRUE) {
                    throw new IllegalStateException("Fragment shader compilation failed for " + computeShaderLocation + "\n" + glGetShaderInfoLog(computeShader));
                }
            }
            
            newProgramID = glCreateProgram();
            
            if (vertexShader != 0 && fragmentShader != 0) {
                glAttachShader(newProgramID, vertexShader);
                glAttachShader(newProgramID, fragmentShader);
            }
            if (computeShader != 0) {
                glAttachShader(newProgramID, computeShader);
            }
            glLinkProgram(newProgramID);
            int linkStatus = glGetProgrami(newProgramID, GL_LINK_STATUS);
            if (linkStatus != GL_TRUE) {
                throw new IllegalStateException("Program linking failed for " + baseResourceLocation + "\n" + glGetProgramInfoLog(newProgramID));
            }
            
            if (vertexShader != 0 && fragmentShader != 0) {
                glDetachShader(newProgramID, vertexShader);
                glDetachShader(newProgramID, fragmentShader);
            }
            if (computeShader != 0) {
                glDetachShader(newProgramID, computeShader);
            }
            int oldProgramID = programID;
            programID = newProgramID;
            newProgramID = oldProgramID;
            
            reloadCallbacks.forEach(c -> c.accept(this));
            
        } finally {
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteShader(computeShader);
            glDeleteProgram(newProgramID);
        }
    }
    
    public void addReloadCallback(Consumer<Program> consumer) {
        consumer.accept(this);
        reloadCallbacks.add(consumer);
    }
    
    @Override
    public int handle() {
        return programID;
    }
    
    @Override
    public void delete() {
        glDeleteProgram(programID);
        programID = 0;
    }
}
