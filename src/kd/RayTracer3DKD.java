package kd;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

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
import util.SceneEditor;
import util.Util;
import util.VectorUtil;


public class RayTracer3DKD {

	private static int w = 512, h = 512;
    private static Vector3f bb = new Vector3f(1000, 1000, 1000);
    private static Vector3f bbm = new Vector3f(-1000, -1000, -1000);
    private static boolean done = false;
    private static PointerBuffer gws = BufferUtils.createPointerBuffer(2);
    private static PointerBuffer lws = BufferUtils.createPointerBuffer(2);
    private static int maxTreeDepth = 5;
    private  static int targetElementsPerNode = 1;
    private static boolean jepp = true;
    
    private static CLContext context;
    private static CLDevice device;
    private static CLProgram program;
    private static CLCommandQueue queue;
       
    private static CLMem texMem, nodesMem, nodesContentMem, flagsMem, axisMem, splitsMem, eyeMem, viewMem, spheresMem, trianglesMem, lightsMem;   
    private static CLKernel kernel;
    
    private static final int NODE_SIZE  = 4;
    private static final int SPHERE_SIZE = 11;
    private static final int TRIANGLE_SIZE = 16;
    private static int texture;
    
    private static final FPSCamera camera = new FPSCamera();
    
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private  static final int LEAF = 0;
    
    private static final List<Node> nodes = new ArrayList<Node>();
    
    private static final List<SceneObject> objects = new ArrayList<SceneObject>();
    
    private static final List<Integer> objectsType = new ArrayList<Integer>();
    
    static class SceneObject
    {
    	Vector3f min = new Vector3f();
		Vector3f max = new Vector3f();
		
		public int getId()
		{
			return objects.indexOf(this);
		}
    }
    
    static class Sphere extends SceneObject 
    {
        float radius;
        float coordX;
        float coordY;
        float coordZ;
        float transparency;
        float refractivity;
        float reflectivity;
        float colorR;
        float colorG;
        float colorB;
        float colorA;
        
        public String toString()
        {
        	return "radius: " + radius + ", x: " + coordX + " y: " + coordY + ", z: " + coordZ + ", cR: " + colorR + ", cG: " + colorG + ", cB: " + colorB;
        }
    }
    
    static class Triangle extends SceneObject 
    {
        float coordAX;
        float coordAY;
        float coordAZ;
        float coordBX;
        float coordBY;
        float coordBZ;
        float coordCX;
        float coordCY;
        float coordCZ;
        float transparency;
        float refractivity;
        float reflectivity;
        float colorR;
        float colorG;
        float colorB;
        float colorA;
        
        public String toString()
        {
        	return "aX: " + coordAX + ", aY: " + coordAY + ", aZ: " + coordAZ + ", cR: " + colorR;
        }
    }
    
    static class Node 
    {
        Vector3f min = new Vector3f();
        Vector3f max = new Vector3f();
        List<SceneObject> objects = new ArrayList<SceneObject>();
        List<Integer> objectsType = new ArrayList<Integer>();
        Node parent;
        Node c0;
        Node c1;
        int axis = -1;
        int flag = -1;
        float split;
        int contentOffSet;
        
        public String toString() 
        {
            return "min="+min + "max=" + max +
                    ", id=" + this.getId() + 
                    ", parent="+((parent == null) ? -1 : nodes.indexOf(parent)) + 
                    ", flag=" + flag + 
                    ", count=" + objects.size() +
                    ", contentOffset=" + contentOffSet + 
                    ", split=" + split +
                    ", axis=" + axis;
        }
        
        public int getId() 
        {
            return nodes.indexOf(this);
        }
    }

    
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
    
    public static void main(String[] args) 
    {
        try 
        {
            Display.setDisplayMode(new DisplayMode(w, h));
            Display.create();
            Keyboard.create();
            Mouse.create();
            OpenCL.init();
            Util.init();
            createCL(Util.getText("./data/RayTracer3DKD.cl"));
        } catch (LWJGLException e) {
            e.printStackTrace();
        }
        camera.move(1,1,-15);
        texture = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer)null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        int counts[] = SceneEditor.readScene();
        FloatBuffer[] buffers = SceneEditor.readScene(counts);
        
        int spherescount   = counts[0];
        int trianglescount = 1;//counts[1];
        int lightscount    = counts[2];
        FloatBuffer spheresBuffer   = buffers[0];
        FloatBuffer trianglesBuffer = buffers[1];
        FloatBuffer lightsBuffer    = buffers[2];
        
        spheresBuffer.position(0);
        trianglesBuffer.position(0);
        lightsBuffer.position(0);
        
        for(int i = 0; i < spherescount; i++)
        {
        	Sphere sphere = new Sphere();
        	sphere.radius = buffers[0].get(i * SPHERE_SIZE + 0);
        	sphere.coordX = buffers[0].get(i * SPHERE_SIZE + 1);
        	sphere.coordY = buffers[0].get(i * SPHERE_SIZE + 2);
        	sphere.coordZ = buffers[0].get(i * SPHERE_SIZE + 3);
        	sphere.transparency = buffers[0].get(i * SPHERE_SIZE + 4);
        	sphere.refractivity = buffers[0].get(i * SPHERE_SIZE + 5);
        	sphere.reflectivity = buffers[0].get(i * SPHERE_SIZE + 6);
        	sphere.colorR = buffers[0].get(i * SPHERE_SIZE + 7);
        	sphere.colorG = buffers[0].get(i * SPHERE_SIZE + 8);
        	sphere.colorB = buffers[0].get(i * SPHERE_SIZE + 9);
        	sphere.colorA = buffers[0].get(i * SPHERE_SIZE + 10);
        	setMinMax(sphere);

        	objects.add(sphere);
        	objectsType.add(1);
        }
        
        for(int i = 0; i < trianglescount; i++)
        {
        	Triangle triangle = new Triangle();
        	triangle.coordAX = buffers[1].get(i * TRIANGLE_SIZE + 0);
        	triangle.coordAY = buffers[1].get(i * TRIANGLE_SIZE + 1);
        	triangle.coordAZ = buffers[1].get(i * TRIANGLE_SIZE + 2);
        	triangle.coordBX = buffers[1].get(i * TRIANGLE_SIZE + 3);
        	triangle.coordBY = buffers[1].get(i * TRIANGLE_SIZE + 4);
        	triangle.coordBZ = buffers[1].get(i * TRIANGLE_SIZE + 5);
        	triangle.coordCX = buffers[1].get(i * TRIANGLE_SIZE + 6);
        	triangle.coordCY = buffers[1].get(i * TRIANGLE_SIZE + 7);
        	triangle.coordCZ = buffers[1].get(i * TRIANGLE_SIZE + 8);
        	triangle.transparency = buffers[1].get(i * TRIANGLE_SIZE + 9);
        	triangle.refractivity = buffers[1].get(i * TRIANGLE_SIZE + 10);
        	triangle.reflectivity = buffers[1].get(i * TRIANGLE_SIZE + 11);
        	triangle.colorR = buffers[1].get(i * TRIANGLE_SIZE + 12);
        	triangle.colorG = buffers[1].get(i * TRIANGLE_SIZE + 13);
        	triangle.colorB = buffers[1].get(i * TRIANGLE_SIZE + 14);
        	triangle.colorA = buffers[1].get(i * TRIANGLE_SIZE + 15);
        	setMinMax(triangle);
        	
        	objects.add(triangle);
        	objectsType.add(2);
        }
        
        createTree(null, 0);
        
        int contentSize = 0;
        int max_num = 0;
        int empty_num = 0;
        List<Integer> contentl = new ArrayList<Integer>();
        for(Node n : nodes) 
        {
            if(n.flag == LEAF) 
            {
//            	if(max_num < n.objects.size()) max_num = n.objects.size();
            	int size = 0;
            	// size = num_triangles * 1000 + num_spheres
            	for(int i = 0; i < n.objects.size(); i++)
                {
                	if(n.objectsType.get(i) == 2)
                		 size += 1000;
                	else size++;  
//                	System.out.println(n.objects.get(i));
                }
            	System.out.println(size);
            	if(size == 0) empty_num++;
                contentl.add(size);
                for(int i = 0; i < n.objects.size(); i++)
                {
                	contentl.add(n.objects.get(i).getId());
                }
                n.contentOffSet = contentSize;
                contentSize += n.objects.size() + 1;
                max_num++;
            }
        }
        System.out.println("total: " + max_num + ", empty: " + empty_num);
        int[] contentArray = new int[contentSize];
        for(int i = 0; i < contentSize; ++i) 
        {
            contentArray[i] = contentl.get(i);
        }
        
        int nodeArray[] = new int[nodes.size() * NODE_SIZE];

        int index = 0;
        for(int i = 0; i < nodes.size(); ++i) 
        {
            Node n = nodes.get(i);
            nodeArray[index++] = n.getId();
            Node c0 = n.c0;
            nodeArray[index++] = c0 == null ? -1 : c0.getId();
            Node c1 = n.c1;
            nodeArray[index++] = c1 == null ? -1 : c1.getId();
            nodeArray[index++] = n.contentOffSet;
        }
        
        int[] flagArray = new int[nodes.size()];
        int[] axisArray = new int[nodes.size()];
        float[] splitArray = new float[nodes.size()];
        for(int i = 0; i < nodes.size(); ++i) 
        {
            flagArray[i] = nodes.get(i).flag;
            axisArray[i] = nodes.get(i).axis;
            splitArray[i] = nodes.get(i).split;
        }
        camera.move(-80,30,-230);
        camera.rotate(0.3f, 0.0f);
        IntBuffer nodes = BufferUtils.createIntBuffer(nodeArray.length);
        nodes.put(nodeArray);
        nodes.position(0);
        
        IntBuffer flags = BufferUtils.createIntBuffer(flagArray.length);
        flags.put(flagArray);
        flags.position(0);
        
        IntBuffer contentBuffer = BufferUtils.createIntBuffer(contentArray.length);
        contentBuffer.put(contentArray);
        contentBuffer.position(0);
        
        IntBuffer axis = BufferUtils.createIntBuffer(axisArray.length);
        axis.put(axisArray);
        axis.position(0);
        
        FloatBuffer splits = BufferUtils.createFloatBuffer(splitArray.length);
        splits.put(splitArray);
        splits.position(0);
        
        nodesMem        = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, nodes);
        spheresMem      = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, spheresBuffer);
        trianglesMem    = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, trianglesBuffer);
        lightsMem       = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, lightsBuffer);
        nodesContentMem = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, contentBuffer);
        flagsMem        = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, flags);
        axisMem         = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, axis);
        texMem          = CL10GL.clCreateFromGLTexture2D(context, CL10.CL_MEM_WRITE_ONLY, GL11.GL_TEXTURE_2D, 0, texture, null);
        splitsMem       = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, splits);
        viewMem         = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY, MatrixUtil.getFromMatrix(camera.getView()));
        eyeMem          = OpenCL.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY, VectorUtil.getFromVector(camera.getEyePos()));

        kernel =  OpenCL.clCreateKernel(program, "ray3dTKD");
        
        kernel.setArg(0, texMem);
        kernel.setArg(1, nodesMem);
        kernel.setArg(2, nodesContentMem);
        kernel.setArg(3, flagsMem);
        kernel.setArg(4, axisMem);
        kernel.setArg(5, splitsMem);
        kernel.setArg(6, eyeMem);
        kernel.setArg(7, viewMem);
        kernel.setArg(8, spheresMem);
        kernel.setArg(9, trianglesMem);
        kernel.setArg(10, lightsMem);
        kernel.setArg(11, spherescount);
        kernel.setArg(12, trianglescount);
        kernel.setArg(13, lightscount);
        updateView();
        gameLoop();
    }
    
    static int getLongestAxis(float w, float h, float d)
    {
        return (w >= d && w >= h) ? 0 : (h >= w && h >= d) ? 1 : 2;
    }
    
    static void createTree(Node parent, int depth)
    {
        if(parent == null)
        {
            parent = new Node();
            parent.min = new Vector3f(bbm.x, bbm.y, bbm.z);
            parent.max = new Vector3f(+bb.x, +bb.y, +bb.z);
            parent.objects = objects;
            parent.objectsType = objectsType;
            parent.flag = -1;
            nodes.add(parent);
        }
        
        if(depth == maxTreeDepth) 
        {
            parent.flag = LEAF;
            return;
        }
        
        Node c0 = new Node();
        c0.parent = parent;
        Node c1 = new Node();
        c1.parent = parent;
        parent.c0 = c0;
        parent.c1 = c1;
        nodes.add(c0);
        nodes.add(c1);
        
        float[] c0minmax = getMinMax(parent.objects, parent.min, parent.max);
        
        float c0minx, c0maxx, c0miny, c0maxy, c0minz, c0maxz;
        if(jepp)
        {
         c0maxx = c0minmax[0];//parent.min.x;
         c0maxy = c0minmax[1];//parent.max.x;
         c0maxz = c0minmax[2];//parent.min.y;
         c0minx = c0minmax[3];//parent.max.y;
         c0miny = c0minmax[4];//parent.min.z;
         c0minz = c0minmax[5];//parent.max.z;
        }
        else
        {
        	 c0minx = parent.min.x;
             c0maxx = parent.max.x;
             c0miny = parent.min.y;
             c0maxy = parent.max.y;
             c0minz = parent.min.z;
             c0maxz = parent.max.z;
        }
        	
        
        float c1minx = c0minx;
        float c1maxx = c0maxx;
        float c1miny = c0miny;
        float c1maxy = c0maxy;
        float c1minz = c0minz;
        float c1maxz = c0maxz;
        
        int axis = getLongestAxis(c0maxx - c0minx, c0maxy - c0miny, c0maxz - c0minz);
        float split = 0;
        switch(axis) 
        {
        case 0: {
            split = c0minx + (c0maxx - c0minx) * 0.5f;
            c0maxx = split;
            c1minx = split;
        } break;
        case 1: {
            split = c0miny + (c0maxy - c0miny) * 0.5f;
            c0maxy = split;
            c1miny = split;
            
        } break;
        case 2: {
            split = c0minz + (c0maxz - c0minz) * 0.5f;
            c0maxz = split;
            c1minz = split;
            
        } break;
        }
        
        assert c0minx != c0maxx;
        assert c0miny != c0maxy;
        assert c1minx != c1maxx;
        assert c1miny != c1maxy;
        parent.split = split;
        parent.axis = axis;

        c0.min.x = c0minx;
        c0.min.y = c0miny;
        c0.min.z = c0minz;
        c0.max.x = c0maxx;
        c0.max.y = c0maxy;
        c0.max.z = c0maxz;
        c0.parent = parent;
        c0.flag = LEFT;
        
        c1.min.x = c1minx;
        c1.min.y = c1miny;
        c1.min.z = c1minz;
        c1.max.x = c1maxx;
        c1.max.y = c1maxy;
        c1.max.z = c1maxz;
        c1.parent = parent;
        c1.flag = RIGHT;
               
        arrangeSceneObjects(parent, c0);
        arrangeSceneObjects(parent, c1);
       
        if(c0.objects.size() > targetElementsPerNode)
        {
            createTree(c0, depth+1); 
        } else 
        {
        	System.out.println("leaf here!");
            c0.flag = LEAF;
        }

        if(c1.objects.size() > targetElementsPerNode) 
        {
            createTree(c1, depth+1); 
        } else 
        {
        	System.out.println("leaf here!");
            c1.flag = LEAF;
        }
    }
    
    static float[] getMinMax(List<SceneObject> sceneObjects, Vector3f min, Vector3f max) 
    {
    	float[] result = new float[6];
    	for(SceneObject object : sceneObjects)
    	{
    		if(object.max.x > max.x) result [0] = object.max.x;
    		if(object.max.y > max.y) result [1] = object.max.y;
    		if(object.max.z > max.z) result [2] = object.max.z;
    		if(object.min.x < min.x) result [3] = object.min.x;
    		if(object.min.y < min.y) result [4] = object.min.y;
    		if(object.min.z < min.z) result [5] = object.min.z;    		
    	}
		return result;
	}

	static void arrangeSceneObjects(Node parent, Node child) 
    {
        int objectscount = parent.objects.size();
        int flag = child.flag;
        int axis = parent.axis;
        for(int i = 0; i < objectscount; ++i) 
        {
            SceneObject s = parent.objects.get(i);
            Integer type = parent.objectsType.get(i);
            if(flag == RIGHT) 
            {
                if(getAxis(axis, s.max) >= getAxis(axis, child.min)) 
                {
                    child.objects.add(s);
                    child.objectsType.add(type);
                }
            } else if(flag == LEFT)
            {
                if(getAxis(axis, s.min) <= getAxis(axis, child.max)) 
                {
                    child.objects.add(s);
                    child.objectsType.add(type);
                }
            }
        }
    }
    
    static void setMinMax(Sphere circle) 
    {
        circle.min.x = circle.coordX - circle.radius;
        circle.max.x = circle.coordX + circle.radius;
        
        circle.min.y = circle.coordY - circle.radius;
        circle.max.y = circle.coordY + circle.radius;
        
        circle.min.z = circle.coordZ - circle.radius;
        circle.max.z = circle.coordZ + circle.radius;
    }
    
    static void setMinMax(Triangle triangle)
    {
    	triangle.min.x = Math.min(triangle.coordAX, Math.min(triangle.coordBX, triangle.coordCX));
    	triangle.max.x = Math.max(triangle.coordAX, Math.max(triangle.coordBX, triangle.coordCX));
    	if(triangle.min.x == triangle.max.x) triangle.max.x += 0.1;
    	
    	triangle.min.y = Math.min(triangle.coordAY, Math.min(triangle.coordBY, triangle.coordCY));
    	triangle.max.y = Math.max(triangle.coordAY, Math.max(triangle.coordBY, triangle.coordCY));
    	if(triangle.min.y == triangle.max.y) triangle.max.y += 0.1;
    	
    	triangle.min.z = Math.min(triangle.coordAZ, Math.min(triangle.coordBZ, triangle.coordCZ));
    	triangle.max.z = Math.max(triangle.coordAZ, Math.max(triangle.coordBZ, triangle.coordCZ));
    	if(triangle.min.z == triangle.max.z) triangle.max.z += 0.1;
    }
    
    static float getAxis(int axis, Vector3f v) 
    {
        switch(axis) {
        case 0 : return v.x;
        case 1 : return v.y;
        case 2 : return v.z;
        }
        throw new IllegalArgumentException();
    }
 
    static void gameLoop() 
    {       
        gws.put(0, w);
        gws.put(1, h);
        lws.put(0, 16);
        lws.put(1, 16);

        while(!done && !Display.isCloseRequested())
        {
            if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) { done = true; }
            
            handleInput();
            Display.update();

            OpenCL.clEnqueueAcquireGLObjects(queue, texMem, null, null);
            OpenCL.clEnqueueNDRangeKernel(queue, kernel, 2, null, gws, lws, null, null);
            OpenCL.clEnqueueReleaseGLObjects(queue, texMem,  null, null);
            OpenCL.clFinish(queue);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            Util.drawTexture(1);
            
            Util.tick();
            Display.setTitle("Realtime Raytracing | FPS: " + Integer.toString((int)Util.getFps()));
        }
        clean();
    }
    
    public static void handleInput() 
    {
        float scale = 1;
        if(Keyboard.isKeyDown(Keyboard.KEY_W)) { camera.move(0, 0, +scale);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_S)) { camera.move(0, 0, -scale);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_A)) { camera.move(-scale, 0, 0);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_D)) { camera.move(+scale, 0, 0);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_C)) { camera.move(0, -scale, 0);updateView(); }
        if(Keyboard.isKeyDown(Keyboard.KEY_SPACE)) { camera.move(0, +scale, 0);updateView(); }
        
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
    
    static void updateView() 
    {
        CL10.clEnqueueWriteBuffer(queue, viewMem, CL10.CL_TRUE, 0L, MatrixUtil.getFromMatrix(camera.getView()), null, null);
        CL10.clEnqueueWriteBuffer(queue, eyeMem, CL10.CL_TRUE, 0L, VectorUtil.getFromVector(camera.getEyePos()), null, null);
        OpenCL.clFinish(queue);
    }

    static void clean()  
    {
        OpenCL.clReleaseMemObject(nodesMem);
        OpenCL.clReleaseMemObject(spheresMem);
        OpenCL.clReleaseMemObject(trianglesMem);
        OpenCL.clReleaseMemObject(lightsMem);
        OpenCL.clReleaseMemObject(nodesContentMem);
        OpenCL.clReleaseMemObject(flagsMem);
        OpenCL.clReleaseMemObject(axisMem);
        OpenCL.clReleaseMemObject(eyeMem);
        OpenCL.clReleaseMemObject(viewMem);
        OpenCL.clReleaseMemObject(texMem);
        OpenCL.clReleaseMemObject(splitsMem);
        OpenCL.clReleaseKernel(kernel);
        OpenCL.clReleaseCommandQueue(queue);
        OpenCL.clReleaseProgram(program);
        OpenCL.clReleaseContext(context);
        OpenCL.destroy();
        Mouse.destroy();
        Keyboard.destroy();
        Util.destroy();
        Display.destroy();
    }
}