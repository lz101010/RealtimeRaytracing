package main;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Vector3f;

import util.FPSCamera;
import util.MatrixUtil;
import util.OpenCL;
import util.Util;
import util.VectorUtil;
import util.SceneEditor;

public class RayTracer3D 
{    
    static boolean done = false;
    static int w = 512, h = 512;
    static final PointerBuffer gws = BufferUtils.createPointerBuffer(2);
    static final PointerBuffer lws = BufferUtils.createPointerBuffer(2);
    static CLContext context;
    static CLDevice device;
    static CLProgram program;
    static CLCommandQueue queue;
    static CLKernel kernel_normal, kernel_refrac, kernel_reflec;
    static int colorsGL;
    static int mode = 0;
    static boolean tstrip = false;
    static boolean toggle = true;
    static boolean random = false;
    
    static Random r = new Random(1);
    
    static final FPSCamera camera = new FPSCamera();
    
    static CLMem colorsCL, viewMem, eyeMem, spheresMem, trianglesMem, lightsMem, bufferMem, deleteMem, texMem01, texMem02, texMem03, texMem04, texMem05;
    static FloatBuffer spheres, triangles, lights, delete;
    static int spherescount = 0, trianglescount = 0, lightscount = 0;
    
    static float radius = 10, colorR = 0, colorG = 0, colorB = 0, colorA = 0, texture_number = 0, reflectivity = 0, refractivity = 0;
    static float triangleAX = 0, triangleAY = 0, triangleAZ = 0, triangleBX = 0, triangleBY = 0, triangleBZ = 0;
    static int triangle_count = 0;
    static int flag = 0;
    
    
    /**
     * initializes OpenCL
     * @param source source-file
     * @throws LWJGLException
     */
    static void createCL(String source) throws LWJGLException 
    {
        CLPlatform platform = null;
        
        for(CLPlatform plf : CLPlatform.getPlatforms()) 
        {
            if(plf.getDevices(CL10.CL_DEVICE_TYPE_GPU) != null) 
            {
                device = plf.getDevices(CL10.CL_DEVICE_TYPE_GPU).get(0);
                platform = plf;
                if(device != null) break;
            }
        }
        
        context = CLContext.create(platform, platform.getDevices(CL10.CL_DEVICE_TYPE_GPU), null, Display.getDrawable(), null);
        
        queue = OpenCL.clCreateCommandQueue(context, device, 0);
        
        program = OpenCL.clCreateProgramWithSource(context, source);
        OpenCL.clBuildProgram(program, device, "", null);
        OpenCL.checkProgram(program, device);
    }
    
    public static void main(String[] a) 
    {
    	try 
        {
    		// init environment
            Display.setDisplayMode(new DisplayMode(w, h));
            Display.create();
            Display.setTitle("Realtime Raytracing");
            Keyboard.enableRepeatEvents(true);
            Keyboard.create();
            Mouse.create();
            
            // init openCL
            OpenCL.init();
            
            // init openGL 
            Util.init();
            createCL(Util.getText("./data/RayTracer3D.cl"));
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
            
    	// init texture
        colorsGL = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorsGL);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer)null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
            
        camera.move(471.0f, 65.0f, -53.0f);
        camera.rotate(-1.3f, 0.0f);
        
        // init kernels
        kernel_normal = OpenCL.clCreateKernel(program, "ray3Dnormal");
        kernel_refrac = OpenCL.clCreateKernel(program, "ray3Drefrac");
        kernel_reflec = OpenCL.clCreateKernel(program, "ray3Dreflec");
        
        updateScene();
        
        // load textures
        float[][][] texture01 = Util.getImageContents("data/texture_stone.jpg");
        float[][][] texture02 = Util.getImageContents("data/texture_wood.jpg");
        float[][][] texture03 = Util.getImageContents("data/texture_ice.jpg");
        float[][][] texture04 = Util.getImageContents("data/texture_metal.jpg");

        FloatBuffer tex01 = BufferUtils.createFloatBuffer(512 * 512 * 3);
        FloatBuffer tex02 = BufferUtils.createFloatBuffer(512 * 512 * 3);
        FloatBuffer tex03 = BufferUtils.createFloatBuffer(512 * 512 * 3);
        FloatBuffer tex04 = BufferUtils.createFloatBuffer(512 * 512 * 3);

        for(int i = 0; i < 512; i++)
        {
        	for(int j = 0; j < 512; j++)
        	{
        		tex01.put(texture01[i][j]);
        		tex02.put(texture02[i][j]);
        		tex03.put(texture03[i][j]);
        		tex04.put(texture04[i][j]);
        	}
        }
        
        float[][][] texture05 = Util.getImageContents("data/skybox.jpg");
        FloatBuffer tex05 = BufferUtils.createFloatBuffer(2048 * 2048 * 3);
        for(int i = 0; i < 2048; i++)
        {
        	for(int j = 0; j < 2048; j++)
        	{
        		tex05.put(texture05[j][i]);
        	}
        }
        tex01.position(0); tex02.position(0); tex03.position(0); tex04.position(0); tex05.position(0); 
        
        texMem01 = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, tex01);
        texMem02 = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, tex02);
        texMem03 = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, tex03);
        texMem04 = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, tex04);
        texMem05 = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, tex05);
        
        // helper cl_mem_object for deleting objects in the scene
        delete = BufferUtils.createFloatBuffer(1);
        delete.position(0);
        delete.put(0, 0);

        // openCL-texture object
        colorsCL = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_WRITE_ONLY, GL11.GL_TEXTURE_2D, 0, colorsGL, null);
        
        viewMem      = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY, 16 * 4);
        eyeMem       = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY, 3 * 4);
        deleteMem    = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, delete);
        
        // fill kernels with arguments
        kernel_normal.setArg(0, colorsCL);
        kernel_normal.setArg(7, eyeMem);
        kernel_normal.setArg(8, viewMem);
        kernel_normal.setArg(9, deleteMem);
        kernel_normal.setArg(10, texMem01);
        kernel_normal.setArg(11, texMem02);
        kernel_normal.setArg(12, texMem03);
        kernel_normal.setArg(13, texMem04);
        kernel_normal.setArg(14, texMem05);
        
        kernel_refrac.setArg(0, colorsCL);
        kernel_refrac.setArg(7, eyeMem);
        kernel_refrac.setArg(8, viewMem);
        
        kernel_reflec.setArg(0, colorsCL);
        kernel_reflec.setArg(7, eyeMem);
        kernel_reflec.setArg(8, viewMem);

        gws.put(0, w);
        gws.put(1, h);
        lws.put(0, 8);
        lws.put(1, 16);
        
        updateView();
        gameLoop();
    }
    
    /**
     * handles the user input
     */
    public static void handleInput() 
    {
        float scale = 2;
        // camera movement
        if(Keyboard.isKeyDown(Keyboard.KEY_W) )     { toggle = false;camera.move(0, 0, +scale);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_S) )     { toggle = false;camera.move(0, 0, -scale);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_A) )     { toggle = false;camera.move(-scale, 0, 0);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_D) )     { toggle = false;camera.move(+scale, 0, 0);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_SPACE) ) { toggle = false;camera.move(0, +scale, 0);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_C) )     { toggle = false;camera.move(0, -scale, 0);updateView(); }
        
        // random values when inserting objects
        if(Keyboard.isKeyDown(Keyboard.KEY_R) && toggle) { toggle = false; random = !random; }
        
        // triangle strip modus when inserting triangles
        if(Keyboard.isKeyDown(Keyboard.KEY_T) && toggle) { toggle = false; tstrip = !tstrip; }
        
        // switch thru kernels
        if(Keyboard.isKeyDown(Keyboard.KEY_M) && toggle) 
        { 
        	toggle = false; 
        	mode = (mode + 1) % 3; 
        }
        
        // insert sphere
        if(Keyboard.isKeyDown(Keyboard.KEY_INSERT) && toggle)
        { 
        	toggle = false;
        	triangle_count = 0;
        	Vector3f vec = camera.getEyePos();
        	if(random)
        	{
        		float radius = r.nextFloat() * 10 + 1;
        		float colorR = r.nextFloat();
        		float colorG = r.nextFloat();
        		float colorB = r.nextFloat();
        		float reflectivity = r.nextFloat();
        		SceneEditor.addSphere(radius, vec.getX(), vec.getY(), vec.getZ(), texture_number, refractivity, reflectivity, colorR, colorG, colorB, colorA);
        	}
        	else
        	{
        		SceneEditor.addSphere(radius, vec.getX(), vec.getY(), vec.getZ(), texture_number, refractivity, reflectivity, colorR, colorG, colorB, colorA);
        	}
        	updateScene();
        }
        
        // insert triangle
        if(Keyboard.isKeyDown(Keyboard.KEY_HOME) && toggle)
        {
        	toggle = false;
        	Vector3f vec = camera.getEyePos();
        	switch(triangle_count)
        	{
        	case 0 : triangleAX = vec.getX(); triangleAY = vec.getY(); triangleAZ = vec.getZ(); triangle_count++; break;
        	case 1 : triangleBX = vec.getX(); triangleBY = vec.getY(); triangleBZ = vec.getZ(); triangle_count++; break;
        	case 2 :
        		if(random)
        		{
            		float colorR = r.nextFloat();
            		float colorG = r.nextFloat();
            		float colorB = r.nextFloat();
            		SceneEditor.addTriangle(triangleAX, triangleAY, triangleAZ, 
							triangleBX, triangleBY, triangleBZ, 
							vec.getX(), vec.getY(), vec.getZ(), 
							texture_number, refractivity, reflectivity, 
							colorR, colorG, colorB, colorA);
            	updateScene();
        		}
        		else
        		{
        		SceneEditor.addTriangle(triangleAX, triangleAY, triangleAZ, 
        								triangleBX, triangleBY, triangleBZ, 
        								vec.getX(), vec.getY(), vec.getZ(), 
        								texture_number, refractivity, reflectivity, 
        								colorR, colorG, colorB, colorA);
        		updateScene();
        		}
        		if(!tstrip)
        		{
        			triangle_count = 0;
        			break;
        		}
        		else
        		{
        			triangleAX = triangleBX; triangleAY = triangleBY; triangleAZ = triangleBZ;
        			triangleBX = vec.getX(); triangleBY = vec.getY(); triangleBZ = vec.getZ();
        		}
        		
        	}
        }
        
        // insert light
        if(Keyboard.isKeyDown(Keyboard.KEY_END) && toggle)
        {
        	toggle = false;
        	Vector3f vec = camera.getEyePos();
        	SceneEditor.addLight(vec.x, vec.y, vec.z);
        	updateScene();
        }
        
        // delete object
        if(Keyboard.isKeyDown(Keyboard.KEY_DELETE) && toggle)
        {
        	toggle = false;
        	delete.put(0, 2);
        	OpenCL.clEnqueueWriteBuffer(queue, deleteMem, CL10.CL_TRUE, 0, delete, null, null);
        	kernel_normal.setArg(9, deleteMem);
        }
        
        // change flags
        if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT) && toggle)
        { 
        	toggle = false; 
        	flag = (flag + 1) % 8; 
        	printFlag(); 
        }
        
        if(Keyboard.isKeyDown(Keyboard.KEY_LEFT) && toggle)  
        { 
        	toggle = false;
        	flag = (flag + 7) % 8; 
        	printFlag();
        }
        
        if(	!Keyboard.isKeyDown(Keyboard.KEY_M) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_INSERT) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_HOME) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_RIGHT) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_LEFT) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_R) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_DELETE) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_END) &&
        	!Keyboard.isKeyDown(Keyboard.KEY_T))
        {
        	toggle = true;
        }
        
        // change flag value
        if(Keyboard.isKeyDown(Keyboard.KEY_DOWN))  { changeFlag(-1); }
        if(Keyboard.isKeyDown(Keyboard.KEY_UP))    { changeFlag(+1); }
        	
        // rotate camera
        while (Mouse.next()) 
        {
            if (Mouse.getEventButton() == 0) 
            {
                Mouse.setGrabbed(Mouse.getEventButtonState());
            }
            if (Mouse.isGrabbed()) 
            {
                camera.rotate(1e-3f*Mouse.getEventDX(), -1e-3f*Mouse.getEventDY());
                updateView();
            }
        }
    }
    
    /**
     * update the view matrix
     */
    static void updateView() 
    {
        CL10.clEnqueueWriteBuffer(queue, viewMem, CL10.CL_TRUE, 0L, MatrixUtil.getFromMatrix(camera.getView()), null, null);
        CL10.clEnqueueWriteBuffer(queue, eyeMem,  CL10.CL_TRUE, 0L, VectorUtil.getFromVector(camera.getEyePos()), null, null);
        OpenCL.clFinish(queue);
    }
    
    /**
     * update the scene when objects are added or removed
     */
    static void updateScene()
    {
    	int counts[] = SceneEditor.readScene();
        FloatBuffer[] buffers = SceneEditor.readScene(counts);
                
        spherescount   = counts[0];
        trianglescount = counts[1];
        lightscount    = counts[2];
        
        spheres   = buffers[0];
        triangles = buffers[1];
        lights    = buffers[2];
        
        spheres.position(0);
        triangles.position(0);
        lights.position(0);
    	
        spheresMem   = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, spheres);
        trianglesMem = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, triangles);
        lightsMem    = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, lights);
        
        kernel_normal.setArg(1, spheresMem);
        kernel_normal.setArg(2, trianglesMem);
        kernel_normal.setArg(3, lightsMem);
        kernel_normal.setArg(4, spherescount);
        kernel_normal.setArg(5, trianglescount);
        kernel_normal.setArg(6, lightscount);
        
        kernel_refrac.setArg(1, spheresMem);
        kernel_refrac.setArg(2, trianglesMem);
        kernel_refrac.setArg(3, lightsMem);
        kernel_refrac.setArg(4, spherescount);
        kernel_refrac.setArg(5, trianglescount);
        kernel_refrac.setArg(6, lightscount);
        
        kernel_reflec.setArg(1, spheresMem);
        kernel_reflec.setArg(2, trianglesMem);
        kernel_reflec.setArg(3, lightsMem);
        kernel_reflec.setArg(4, spherescount);
        kernel_reflec.setArg(5, trianglescount);
        kernel_reflec.setArg(6, lightscount);
    }
    
    /**
     * helper function for printing the current flag
     */
    static void printFlag()
    {
    	switch(flag)
    	{
    	case 0 : System.out.println("Flag: RADIUS"); break;
    	case 1 : System.out.println("Flag: texture_numb"); break;
    	case 2 : System.out.println("Flag: REFRACTIVITY"); break;
    	case 3 : System.out.println("Flag: REFLECTIVITY"); break;
    	case 4 : System.out.println("Flag: COLOR_R"); break;
    	case 5 : System.out.println("Flag: COLOR_G"); break;
    	case 6 : System.out.println("Flag: COLOR_B"); break;
    	case 7 : System.out.println("Flag: COLOR_A"); break;
    	}
    }
    
    /**
     * helper function for changing the current flag's value
     * @param x value to be changed by
     */
    static void changeFlag(int x)
    {
    	switch(flag)
    	{
    	case 0 : 
    		radius = clamp(radius + x, 0.5f, 100.0f); 
    		System.out.println("RADIUS       : " + radius); 
    		break;
    	case 1 : 
    		texture_number = clamp(texture_number + 0.5f * x, 0.0f, 4.0f);
    		System.out.println("texture_numb : " + texture_number);
			break;
    	case 2 : 
    		refractivity = clamp(refractivity + 0.01f * x, 0.8f, 1.2f);
    		System.out.println("REFRACTIVITY : " + refractivity); 
			break;
    	case 3 : 
    		reflectivity = clamp(reflectivity + 0.01f * x, 0.0f, 1.0f);
    		System.out.println("REFLECTIVITY : " + reflectivity); 
			break;
    	case 4 : 
    		colorR = clamp(colorR + 0.01f * x, 0.0f, 1.0f);
    		System.out.println("COLOR_R      : " + colorR); 
			break;
    	case 5 : 
    		colorG = clamp(colorG + 0.01f * x, 0.0f, 1.0f);
    		System.out.println("COLOR_G      : " + colorG); 
			break;
    	case 6 : 
    		colorB = clamp(colorB + 0.01f * x, 0.0f, 1.0f);
    		System.out.println("COLOR_B      : " + colorB); 
			break;
    	case 7 : 
    		colorA = clamp(colorA + 0.01f * x, 0.0f, 1.0f);
    		System.out.println("COLOR_A      : " + colorA); 
			break;
    	}
    }
    
    /**
     * helper function
     * @param x value to be clamped
     * @param min lowest possible value
     * @param max highest possible value
     * @return x clamped
     */
    static float clamp(float x, float min, float max)
    {
    	if(x < min) return min;
    	if(x > max) return max;
    	return x;
    }
    
    /**
     * render part
     */
    static void gameLoop() 
    {
        while(!done && !Display.isCloseRequested()) 
        {
            if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) done = true;

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            
            CL10GL.clEnqueueAcquireGLObjects(queue, colorsCL, null, null);
            switch(mode)
            {
            // ray casting
            case 0 :
            	OpenCL.clEnqueueNDRangeKernel(queue, kernel_normal, 2, null, gws, lws, null, null);
            	if(delete.get(0) == 2)
            	{
            		// activate object deletion
            		OpenCL.clEnqueueReadBuffer(queue, deleteMem, CL10.CL_TRUE, 0, delete, null, null);
            		OpenCL.clEnqueueReadBuffer(queue, spheresMem, CL10.CL_TRUE, 0, spheres, null, null);
            		OpenCL.clEnqueueReadBuffer(queue, trianglesMem, CL10.CL_TRUE, 0, triangles, null, null);
            		SceneEditor.writeScene(spheres, triangles, lights, spherescount, trianglescount, lightscount);
            		kernel_normal.setArg(9, deleteMem);
            		updateScene();
            	}
            	break;
            // recursive raytracing (reflection rays)
            case 1 : OpenCL.clEnqueueNDRangeKernel(queue, kernel_refrac, 2, null, gws, lws, null, null); break;
            // recursive raytracing (refraction rays)
            case 2 : OpenCL.clEnqueueNDRangeKernel(queue, kernel_reflec, 2, null, gws, lws, null, null); break;
            }
            
            CL10GL.clEnqueueReleaseGLObjects(queue, colorsCL, null, null);
            Util.drawTexture(1);
            OpenCL.clFinish(queue);
            
            Display.update();
            handleInput();
            computeFPS();
        }
        clean();
    }
    
    /**
     * shows the current FPS
     */
    static void computeFPS()
    {
    	Util.tick();
    	Display.setTitle("Realtime Raytracing | FPS: " + Integer.toString((int)Util.getFps()));
    }
    
    /**
     * cleans up all openCL- and openGL-related objects
     */
    static void clean()  
    {
    	SceneEditor.writeScene(spheres, triangles, lights, spherescount, trianglescount, lightscount);
        GL11.glDeleteTextures(colorsGL);
        OpenCL.clReleaseMemObject(colorsCL);
        OpenCL.clReleaseMemObject(spheresMem);
        OpenCL.clReleaseMemObject(trianglesMem);
        OpenCL.clReleaseMemObject(lightsMem);
        OpenCL.clReleaseMemObject(eyeMem);
        OpenCL.clReleaseMemObject(viewMem);
        OpenCL.clReleaseCommandQueue(queue);
        OpenCL.clReleaseKernel(kernel_normal);
        OpenCL.clReleaseProgram(program);
        OpenCL.clReleaseContext(context);
        OpenCL.destroy();
        Mouse.destroy();
        Keyboard.destroy();
        Display.destroy();
    }
}
