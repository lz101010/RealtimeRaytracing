package util;
import java.awt.image.BufferedImage;
import java.awt.Color;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;



public class Util {
    public static final int SHADER_DATA = 0;
    private static long lastNanos, currentNanos;
    private static float fps = 0;
    private static int ticks = 0;
    public static long framesTime = 0;
    
    private static int quadProgram;
    private static int vertexArray;
    private static int texLocation;
    private static int ibid, vbid;
    
    private static boolean init = false;
    
    public static void destroy() {
        GL15.glDeleteBuffers(ibid);
        GL15.glDeleteBuffers(vbid);
        GL30.glDeleteVertexArrays(vertexArray);
        GL20.glDeleteProgram(quadProgram);
    }
    
    public static void init() {
        if(init) return;
        init = true;
        quadProgram = createProgram("./data/Quad_VS.glsl", "./data/Quad_FS.glsl");
        texLocation = GL20.glGetUniformLocation(quadProgram, "image");
        vertexArray = GL30.glGenVertexArrays();

        float v[] = {
                -1.f, -1.f, 0, 0,
                +1.f, -1.f, 1, 0,
                -1.f, +1.f, 0, 1,
                +1.f, +1.f, 1, 1};
        int i[] = {0,1,2,3};
        FloatBuffer data = BufferUtils.createFloatBuffer(16);
        data.put(v);
        data.flip();
        IntBuffer indices = BufferUtils.createIntBuffer(4);
        indices.put(i);
        indices.flip();
        
        ibid = GL15.glGenBuffers();
        vbid = GL15.glGenBuffers();
        
        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbid);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STREAM_DRAW);
        
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibid);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STREAM_DRAW);
        
        GL20.glEnableVertexAttribArray(SHADER_DATA);
        GL20.glVertexAttribPointer(SHADER_DATA, 4, GL11.GL_FLOAT, false, 16, 0);  
        
        GL30.glBindVertexArray(0);
        
        data = null;
        indices = null;
    }
    
    public static String getText(String filename) {
        String sourcecode = "";
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sourcecode += line + "\n";
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return sourcecode;
    }
    
    public static void tick() {
        lastNanos = currentNanos;
        currentNanos = System.nanoTime();
        framesTime += currentNanos - lastNanos;
        if((++ticks) % 10 == 0) {
            fps = 10.f/framesTime * 1e9f;
            framesTime = 0;
        } 
    }
    
    public static long getLastMillis() {
        return (currentNanos - lastNanos) / (long)1e6;
    }
    
    public static float getFps() {
        return fps;
    }
    
    static int printTicks = 0;
    public static void printFps(int ticks) {
        if(printTicks++ >= ticks) {
            printTicks = 0;
            if(fps > 1e-2f)
            System.out.println(getFps());
        }
    }
    
    public static void printFps() {
        printFps(1+(int)getFps());
    }
    
    public static void drawCLBuffer(CLCommandQueue queue, CLMem dst, int w , int h, int size) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * size * 4);
        OpenCL.clEnqueueReadBuffer(queue, dst, CL10.CL_TRUE, 0, pixels, null, null);
        OpenCL.clFinish(queue);
        GL11.glDrawPixels(w, h, GL11.GL_RGBA, GL11.GL_FLOAT, pixels);
    }
    
    
    public static void drawTexture(int unit) {
        GL30.glBindVertexArray(vertexArray);
        GL20.glUseProgram(quadProgram);
        GL20.glUniform1i(texLocation, unit);
        GL11.glDrawElements(GL11.GL_TRIANGLE_STRIP, 4,  GL11.GL_UNSIGNED_INT, 0);
    }
    
    public static int createProgram(String vs, String fs) {
        int id = GL20.glCreateProgram();
        
        int vsID = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        int fsID = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        
        GL20.glAttachShader(id, vsID);
        GL20.glAttachShader(id, fsID);
        
        String vertexShaderContents = getText(vs);
        String fragmentShaderContents = getText(fs);
        
        GL20.glShaderSource(vsID, vertexShaderContents);
        GL20.glShaderSource(fsID, fragmentShaderContents);
        
        GL20.glCompileShader(vsID);
        GL20.glCompileShader(fsID);
        
        String log;
        log = GL20.glGetShaderInfoLog(vsID, 1024);
        System.out.print(log);
        log = GL20.glGetShaderInfoLog(fsID, 1024);
        System.out.print(log);
        
        GL20.glBindAttribLocation(id, SHADER_DATA, "vs_in_data");
        
        GL20.glLinkProgram(id);        
        
        log = GL20.glGetProgramInfoLog(id, 1024);
        System.out.print(log);
        return id;
    }
    
    public static float[][][] getImageContents(String imageFile) {
        File file = new File(imageFile);
        if(!file.exists()) {
            throw new IllegalArgumentException(imageFile + " does not exist");
        }
        try {
            BufferedImage image = ImageIO.read(file);
            float[][][] result = new float[image.getHeight()][image.getWidth()][3];
            for(int y=0; y < image.getHeight(); ++y) {
                for(int x=0; x < image.getWidth(); ++x) {
                    Color c = new Color(image.getRGB(image.getWidth() - 1 - x, y));
                    result[y][x][0] = (float)c.getRed() / 255.0f;
                    result[y][x][1] = (float)c.getGreen() / 255.0f;
                    result[y][x][2] = (float)c.getBlue() / 255.0f;
                }
            }
            return result;
        } catch (IOException ex) {
        	ex.printStackTrace();
            return null;
        }
    }
    
    public static Util.ImageContents loadImage(String imageFile) {
        File file = new File(imageFile);
        if(!file.exists()) {
            throw new IllegalArgumentException(imageFile + " does not exist");
        }
        try {
            BufferedImage image = ImageIO.read(file);
            Util.ImageContents contents = new Util.ImageContents(image.getWidth(), image.getHeight(), image.getColorModel().getNumComponents());
            for(int y=0; y < image.getHeight(); ++y) {
                for(int x=0; x < image.getWidth(); ++x) {
                    image.getRaster().getPixel(x, y, contents.pixel);
                    contents.putPixel();
                }
            }
            contents.data.position(0);
            return contents;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }        
    }
    
    public static class ImageContents {
        /**
         * Breite des Bildes in Pixel
         */
        public final int width;
        
        /**
         * Hoehe des Bildes in Pixel
         */
        public final int height;
        
        /**
         * Anzahl der Farbkomponenten
         */
        public final int colorComponents;
        
        /**
         * Rohe Pixeldaten, row-major
         */
        public final FloatBuffer data;
        
        private float pixel[];

        private ImageContents(int width, int height, int colorComponents) {
            this.width = width;
            this.height = height;
            this.colorComponents = colorComponents;
            this.data = BufferUtils.createFloatBuffer(this.width * this.height * this.colorComponents);
            this.pixel = new float[this.colorComponents];
        }
        
        private void putPixel() {
            for(float component : this.pixel) {
                this.data.put(component / 255.0f);
            }
        }
    }
}
