package net.roguelogix.phosphophyllite.quartz.internal.dontcommitme;

import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46C.*;

public class ShaderTesting {
    public static void main(String[] args) {
        final String vertCode;
        final String fragCode;
        try {
            vertCode = Files.readString(Path.of("gl/main.vert"));
            fragCode = Files.readString(Path.of("gl/main.frag"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        if (!glfwInit()) {
            System.err.println("GLFW init failed");
            return;
        }
        
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        long window = glfwCreateWindow(800, 600, "ShaderTesting", 0, 0);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(vertexShader, vertCode);
        glShaderSource(fragmentShader, fragCode);
        glCompileShader(vertexShader);
        glCompileShader(fragmentShader);
        
        int vertCompileStatus = glGetShaderi(vertexShader, GL_COMPILE_STATUS);
        System.err.println("VERTEX ERROR LOG");
        System.err.println(glGetShaderInfoLog(vertexShader));
        int fragCompileStatus = glGetShaderi(fragmentShader, GL_COMPILE_STATUS);
        System.err.println("FRAGMENT ERROR LOG");
        System.err.println(glGetShaderInfoLog(fragmentShader));
        
        if (vertCompileStatus == GL_TRUE && fragCompileStatus == GL_TRUE) {
            System.err.println("Compilation Successful");
            
            int program = glCreateProgram();
            
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            
            glLinkProgram(program);
            
            int linkStatus = glGetProgrami(program, GL_LINK_STATUS);
            if (linkStatus == GL_TRUE) {
                System.err.println("Link Successful");
            }
            System.err.println("LINK ERROR LOG");
            System.err.println(glGetProgramInfoLog(program));
            
            glDeleteProgram(program);
        } else {
            if(vertCompileStatus != GL_TRUE){
                System.err.println("VERTEX COMPILATION FAILED");
            }
            if(fragmentShader != GL_TRUE){
                System.err.println("FRAGMENT COMPILATION FAILED");
            }
        }
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
