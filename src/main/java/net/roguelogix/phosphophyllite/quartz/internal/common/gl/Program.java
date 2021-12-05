package net.roguelogix.phosphophyllite.quartz.internal.common.gl;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.opengl.ARBSeparateShaderObjects.*;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER_BIT;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Program implements GLDeletable, GLReloadable {
    private final ArrayList<Consumer<Program>> reloadCallbacks = new ArrayList<>();
    
    @Nullable
    private final String prepend;
    public final ResourceLocation baseResourceLocation;
    private final ResourceLocation vertexShaderLocation;
    private final ResourceLocation fragmentShaderLocation;
    private final ResourceLocation computeShaderLocation;
    
    private int vertProgramID = 0;
    private int fragProgramID = 0;
    private int compProgramID = 0;
    private int programPipelineID = 0;
    
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
        int vertexProgram = 0;
        int fragmentProgram = 0;
        int computeProgram = 0;
        
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
                if (vertexShaderCode != null) {
                    vertexShaderCode = new StringBuilder(vertexShaderCode).insert(vertexShaderCode.indexOf('\n') + 1, prepend).toString();
                    fragmentShaderCode = new StringBuilder(fragmentShaderCode).insert(fragmentShaderCode.indexOf('\n') + 1, prepend).toString();
                }
                if (computeShaderCode != null) {
                    computeShaderCode = new StringBuilder(computeShaderCode).insert(computeShaderCode.indexOf('\n') + 1, prepend).toString();
                }
            }
            
            if (vertexShaderCode != null) {
                vertexProgram = glCreateShaderProgramv(GL_VERTEX_SHADER, vertexShaderCode);
                fragmentProgram = glCreateShaderProgramv(GL_FRAGMENT_SHADER, fragmentShaderCode);
                
                int vertLinked = glGetProgrami(vertexProgram, GL_LINK_STATUS);
                int fragLinked = glGetProgrami(fragmentProgram, GL_LINK_STATUS);
                if (vertLinked != GL_TRUE && fragLinked != GL_TRUE) {
                    throw new IllegalStateException("Vertex and fragment shader compilation failed for " + baseResourceLocation + "\n" +
                            "Vertex program creation log:\n" + glGetProgramInfoLog(vertexProgram) + "\n\n" +
                            "Fragment program creation log:\n" + glGetProgramInfoLog(fragmentProgram));
                }
                if (vertLinked != GL_TRUE) {
                    throw new IllegalStateException("Vertex shader compilation failed for " + vertexShaderLocation + "\n" + glGetProgramInfoLog(vertexProgram));
                }
                if (fragLinked != GL_TRUE) {
                    throw new IllegalStateException("Fragment shader compilation failed for " + fragmentShaderLocation + "\n" + glGetProgramInfoLog(fragmentProgram));
                }
            }
            
            if (computeShaderCode != null) {
                computeProgram = glCreateShaderProgramv(GL_COMPUTE_SHADER, computeShaderCode);
                int compileStatus = glGetProgrami(computeProgram, GL_LINK_STATUS);
                if (compileStatus != GL_TRUE) {
                    throw new IllegalStateException("Fragment shader compilation failed for " + computeShaderLocation + "\n" + glGetProgramInfoLog(computeProgram));
                }
            }
            
            int newPipeline = glGenProgramPipelines();
            if (vertexProgram != 0 && fragmentProgram != 0) {
                glUseProgramStages(newPipeline, GL_VERTEX_SHADER_BIT, vertexProgram);
                glUseProgramStages(newPipeline, GL_FRAGMENT_SHADER_BIT, fragmentProgram);
            }
            if (computeProgram != 0) {
                glUseProgramStages(newPipeline, GL_COMPUTE_SHADER_BIT, computeProgram);
            }
            
            programPipelineID ^= newPipeline;
            newPipeline ^= programPipelineID;
            programPipelineID ^= newPipeline;
            
            vertProgramID ^= vertexProgram;
            vertexProgram ^= vertProgramID;
            vertProgramID ^= vertexProgram;
            
            fragProgramID ^= fragmentProgram;
            fragmentProgram ^= fragProgramID;
            fragProgramID ^= fragmentProgram;
            
            compProgramID ^= computeProgram;
            computeProgram ^= compProgramID;
            compProgramID ^= computeProgram;
            
            glDeleteProgramPipelines(newPipeline);
            
            reloadCallbacks.forEach(c -> c.accept(this));
        } finally {
            glDeleteProgram(vertexProgram);
            glDeleteProgram(fragmentProgram);
            glDeleteProgram(computeProgram);
        }
    }
    
    public void addReloadCallback(Consumer<Program> consumer) {
        consumer.accept(this);
        reloadCallbacks.add(consumer);
    }
    
    public int vertUniformLocation(String name) {
        return glGetUniformLocation(vertProgramID, name);
    }
    
    public int fragUniformLocation(String name) {
        return glGetUniformLocation(fragProgramID, name);
    }
    
    public int vertHandle() {
        return vertProgramID;
    }
    
    public int fragHandle() {
        return fragProgramID;
    }
    
    public int compHandle() {
        return compProgramID;
    }
    
    public int pipelineHandle() {
        return programPipelineID;
    }
    
    @Override
    public void delete() {
        glDeleteProgram(vertProgramID);
        glDeleteProgram(fragProgramID);
        glDeleteProgram(compProgramID);
        glDeleteProgramPipelines(programPipelineID);
        vertProgramID = 0;
        fragProgramID = 0;
        compProgramID = 0;
        programPipelineID = 0;
    }
}
