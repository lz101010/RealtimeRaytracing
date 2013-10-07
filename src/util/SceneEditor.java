package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class SceneEditor {
	
	private static String filename = "./data/scene.xml";
	
	/**
	 * reads the number of spheres, triangles and lights from a scene-file
	 * @return integer-array with spherescount at position 0, trianglescount at position 1, lightscount at position 3
	 */
	public static int[] readScene()
	{
		int[] result = new int[3];
		BufferedReader br = null;
		try {
			 
			String line;
			boolean tag_spherescount = false, tag_trianglescount = false, tag_lightscount = false;
 
			br = new BufferedReader(new FileReader(filename));
 
			while ((line = br.readLine()) != null) 
			{
				line = line.trim();
				
				if(line.compareTo("<spherescount>") == 0)  { tag_spherescount = true; continue;}
				if(line.compareTo("<trianglescount>") == 0){ tag_trianglescount = true; continue;}
				if(line.compareTo("<lightscount>") == 0)   { tag_lightscount = true; continue;}
				
				if(line.compareTo("</spherescount>") == 0)  { tag_spherescount = false; continue;}
				if(line.compareTo("</trianglescount>") == 0){ tag_trianglescount = false; continue;}
				
				if(tag_spherescount)	{ result[0] = Integer.parseInt(line); continue; }
				if(tag_trianglescount)	{ result[1] = Integer.parseInt(line); continue; }
				if(tag_lightscount)		{ result[2] = Integer.parseInt(line); return result;	}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * parses the scenefile scene.xml
	 * @param counts numbers of objects in scene
	 * @return floatbuffer-array with all float values for the spheres at position 0, values for triangles at position 1, values for lights at position 2
	 */
	public static FloatBuffer[] readScene(int [] counts)
	{
		FloatBuffer[] result = new FloatBuffer[3];
		result[2] = BufferUtils.createFloatBuffer(counts[2] * 3);
		
		float [] spheresBuffer = new float[counts[0] * 11];
		float [] trianglesBuffer = new float[counts[1] * 16];
		
		int spheres_cnt = 0, triangles_cnt = 0, lights_cnt = 0;
		int spheresToErase = 0, trianglesToErase = 0;
		
		boolean	tag_spheres, tag_sphere, tag_radius, tag_coordinates,
	    		tag_triangles, tag_triangle, tag_coordA, tag_coordB, tag_coordC,
	    		tag_lights, tag_light,
	    		tag_texture_number, tag_color, tag_reflectivity, tag_refractivity;

		tag_spheres = tag_sphere = tag_triangles = tag_triangle = tag_lights = tag_light = false;
		tag_radius = tag_coordinates = tag_coordA = tag_coordB = tag_coordC = tag_color = false;
		tag_texture_number = tag_reflectivity = tag_refractivity = false;
		
		BufferedReader br = null;
		
		try
		{
			String line;
			
			br = new BufferedReader(new FileReader(filename));
			
			while((line = br.readLine()) != null)
			{
				line = line.trim();				
								
				// check for tags
				if(line.compareTo("<spheres>") == 0)       { tag_spheres = true; continue;}
				if(line.compareTo("<sphere>") == 0)        { tag_sphere = true;  continue;}
				if(line.compareTo("<radius>") == 0)        { tag_radius = true;  continue;}
				if(line.compareTo("<coordinates>") == 0)   { tag_coordinates = true;  continue;}
				if(line.compareTo("<triangles>") == 0)     { tag_triangles = true; continue;}
				if(line.compareTo("<triangle>") == 0)      { tag_triangle = true; continue;}
				if(line.compareTo("<coordA>") == 0)        { tag_coordA = true;  continue;}
				if(line.compareTo("<coordB>") == 0)        { tag_coordB = true;  continue;}
				if(line.compareTo("<coordC>") == 0)        { tag_coordC = true;  continue;}
				if(line.compareTo("<texture_number>") == 0)  { tag_texture_number = true; continue;}
				if(line.compareTo("<color>") == 0)         { tag_color = true;  continue;}
				if(line.compareTo("<reflectivity>") == 0)  { tag_reflectivity = true;  continue;}
				if(line.compareTo("<refractivity>") == 0)  { tag_refractivity = true;  continue;}
				if(line.compareTo("<lights>") == 0)        { tag_lights = true;  continue;}
				if(line.compareTo("<light>") == 0)         { tag_light = true;  continue;}

				if(line.compareTo("</spheres>") == 0)       { tag_spheres = false;  continue;}
				if(line.compareTo("</sphere>") == 0)        { tag_sphere = false;  continue;}
				if(line.compareTo("</radius>") == 0)        { tag_radius = false;  continue;}
				if(line.compareTo("</coordinates>") == 0)   { tag_coordinates = false;  continue;}
				if(line.compareTo("</triangles>") == 0)     { tag_triangles = false; continue;}
				if(line.compareTo("</triangle>") == 0)      { tag_triangle = false;  continue;}
				if(line.compareTo("</coordA>") == 0)        { tag_coordA = false; continue;}
				if(line.compareTo("</coordB>") == 0)        { tag_coordB = false; continue;}
				if(line.compareTo("</coordC>") == 0)        { tag_coordC = false; continue;}
				if(line.compareTo("</texture_number>") == 0)  { tag_texture_number = false; continue;}
				if(line.compareTo("</color>") == 0)         { tag_color = false; continue;}
				if(line.compareTo("</reflectivity>") == 0)  { tag_reflectivity = false; continue;}
				if(line.compareTo("</refractivity>") == 0)  { tag_refractivity = false; continue;}
				if(line.compareTo("</lights>") == 0)        { tag_lights = false; continue;}
				if(line.compareTo("</light>") == 0)         { tag_light = false; continue;}
				
				if(tag_spheres)
				{
					if(tag_sphere)
					{
						if(tag_radius)		 { spheresBuffer[spheres_cnt++] = Float.parseFloat(line); continue;}
						if(tag_coordinates)	 { spheresBuffer[spheres_cnt++] = Float.parseFloat(line); continue;}
						if(tag_texture_number) { spheresBuffer[spheres_cnt++] = Float.parseFloat(line); continue;}
						if(tag_refractivity) { spheresBuffer[spheres_cnt++] = Float.parseFloat(line); continue;}
						if(tag_reflectivity) { spheresBuffer[spheres_cnt++] = Float.parseFloat(line); continue;}
						if(tag_color)
						{
							float value = Float.parseFloat(line);
							if(spheres_cnt % 11 == 10 && value == 1.0)
							{
								spheresToErase++;
							}
							spheresBuffer[spheres_cnt++] = Float.parseFloat(line);
							continue;
						}
					}
				}
				
				if(tag_triangles)
				{
					if(tag_triangle)
					{
						if(tag_coordA)       { trianglesBuffer[triangles_cnt++] = Float.parseFloat(line); continue;}
						if(tag_coordB)       { trianglesBuffer[triangles_cnt++] = Float.parseFloat(line); continue;}
						if(tag_coordC)       { trianglesBuffer[triangles_cnt++] = Float.parseFloat(line); continue;}
						if(tag_texture_number) { trianglesBuffer[triangles_cnt++] = Float.parseFloat(line); continue;}
						if(tag_refractivity) { trianglesBuffer[triangles_cnt++] = Float.parseFloat(line); continue;}
						if(tag_reflectivity) { trianglesBuffer[triangles_cnt++] = Float.parseFloat(line); continue;}
						if(tag_color)
						{
							float value = Float.parseFloat(line);
							if(triangles_cnt % 16 == 15 && value == 1.0)
							{
								trianglesToErase++;
							}
							
							trianglesBuffer[triangles_cnt++] = Float.parseFloat(line);
							continue;
						}
					}
				}
				
				if(tag_lights)
				{
					if(tag_light) {	result[2].put(lights_cnt++, Float.parseFloat(line)); continue; }
				}
			}
			
			if(spheresToErase > 0)
			{
				result[0] = clean(spheresBuffer, counts[0], spheresToErase, 11);
				counts[0] -= spheresToErase;
			}
			else
			{
				result[0] = BufferUtils.createFloatBuffer(counts[0] * 11);
				result[0].put(spheresBuffer);
			}
			if(trianglesToErase > 0)
			{
				System.out.println(trianglesToErase);
				result[1] = clean(trianglesBuffer, counts[1], trianglesToErase, 16);
				counts[1] -= trianglesToErase;
			}
			else
			{
				result[1] = BufferUtils.createFloatBuffer(counts[1] * 16);
				result[1].put(trianglesBuffer);
			}
			
		} catch(IOException e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if(br != null) br.close();
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
		
		return result;
	}

	/**
	 * clears a floatbuffer of all objects with alpha-value 1
	 * @param buffer buffer to be cleaned
	 * @param count number of values in total
	 * @param toRemove number of objects to remove
	 * @param size size of floatbuffer
	 * @return cleaned floatbuffer
	 */
	private static FloatBuffer clean(float [] buffer, int count, int toRemove, int size)
	{
		FloatBuffer dst = BufferUtils.createFloatBuffer((count - toRemove) * size);
		int j = 0;
		for(int i = 0; j < (count - toRemove) * size; i++)
		{
			if(i % size != (size - 1))
			{
				
				dst.put(j, buffer[i]);
			}
			else
			{
				if(buffer[i] == 1.0f)
				{
					j -= size;
				}
				else
				{
					dst.put(j, buffer[i]);
				}
			}
			j++;
		}
		return dst;
	}
	
	/**
	 * adds a sphere to the scene-file
	 * @param radius radius of the sphere
	 * @param mX x-coordinate of the sphere's position
	 * @param mY y-coordinate of the sphere's position
	 * @param mZ z-coordinate of the sphere's position
	 * @param texture_number texture-number of the sphere, ranges from 0 to 4
	 * @param refractivity spheres's refractivity
	 * @param reflectivity spheres's reflectivity
	 * @param cR red-component of rgba-value
	 * @param cG blue-component of rgba-value
	 * @param cB green-component of rgba-value
	 * @param cA alpha-component of rgba-value
	 */
	public static void addSphere(	float radius,
									float mX, float mY, float mZ,
									float texture_number, float refractivity, float reflectivity,
									float cR, float cG, float cB, float cA)
	{
		if(texture_number < 0) texture_number = 0;
		if(texture_number > 4) texture_number = 4;
		
		BufferedReader br = null;
		BufferedWriter out = null;
		
		String file_txt = "";
		
		String sphere_txt = "";
		sphere_txt += "      <radius>\n";
		sphere_txt += "        "; sphere_txt += Float.toString(radius); sphere_txt += "\n";
		sphere_txt += "      </radius>\n";
		sphere_txt += "      <coordinates>\n";
		sphere_txt += "        "; sphere_txt += Float.toString(mX); sphere_txt += "\n";
		sphere_txt += "        "; sphere_txt += Float.toString(mY); sphere_txt += "\n";
		sphere_txt += "        "; sphere_txt += Float.toString(mZ); sphere_txt += "\n";
		sphere_txt += "      </coordinates>\n";
		sphere_txt += "      <texture_number>\n";
		sphere_txt += "        "; sphere_txt += Float.toString(texture_number); sphere_txt += "\n";
		sphere_txt += "      </texture_number>\n";
		sphere_txt += "      <refractivity>\n";
		sphere_txt += "        "; sphere_txt += Float.toString(refractivity); sphere_txt += "\n";
		sphere_txt += "      </refractivity>\n";
		sphere_txt += "      <reflectivity>\n";
		sphere_txt += "        "; sphere_txt += Float.toString(reflectivity); sphere_txt += "\n";
		sphere_txt += "      </reflectivity>\n";
		sphere_txt += "      <color>\n";
		sphere_txt += "        "; sphere_txt += Float.toString(cR); sphere_txt += "\n";
		sphere_txt += "        "; sphere_txt += Float.toString(cG); sphere_txt += "\n";
		sphere_txt += "        "; sphere_txt += Float.toString(cB); sphere_txt += "\n";
		sphere_txt += "        "; sphere_txt += Float.toString(cA); sphere_txt += "\n";
		sphere_txt += "      </color>\n";
		sphere_txt += "    </sphere>\n";
		sphere_txt += "    <sphere>\n";
		
		try {
			String line;
			boolean tag_cnt = false, tag_spheres = false;
 
			br = new BufferedReader(new FileReader(filename));
 
			while ((line = br.readLine()) != null) 
			{
				if(tag_cnt)
				{
					int spherescount = Integer.parseInt(line.trim()) + 1;
					
					file_txt += "    "; file_txt += Integer.toString(spherescount); file_txt += "\n";
					tag_cnt = false;
					continue;
				}
				file_txt += line;
				file_txt += "\n";
				
				if(line.trim().compareTo("<spherescount>") == 0)  { tag_cnt = true; continue; }
				if(line.trim().compareTo("<spheres>") == 0)       { tag_spheres = true; continue; }
				
				if(tag_spheres)
				{
					file_txt += sphere_txt;
					tag_spheres = false;
					continue;
				}				
			}
			out = new BufferedWriter(new FileWriter(filename));
	        out.write(file_txt);
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(br != null)br.close();
				if(out != null) out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * adds a triangle to the scene
	 * @param aX x-coordinate of triangle's vertex A
	 * @param aY y-coordinate of triangle's vertex A
	 * @param aZ z-coordinate of triangle's vertex A
	 * @param bX x-coordinate of triangle's vertex B
	 * @param bY y-coordinate of triangle's vertex B
	 * @param bZ z-coordinate of triangle's vertex B
	 * @param cX x-coordinate of triangle's vertex C
	 * @param cY y-coordinate of triangle's vertex C
	 * @param cZ z-coordinate of triangle's vertex C
	 * @param texture_number texture-number of the triangle, ranges from 0 to 4
	 * @param refractivity triangle refractivity
	 * @param reflectivity triangle reflectivity
	 * @param cR red-component of rgba-value
	 * @param cG blue-component of rgba-value
	 * @param cB green-component of rgba-value
	 * @param cA alpha-component of rgba-value
	 */
	public static void addTriangle(	float aX, float aY, float aZ,
									float bX, float bY, float bZ,
									float cX, float cY, float cZ,
									float texture_number, float refractivity, float reflectivity,
									float cR, float cG, float cB, float cA)
	{
		if(texture_number < 0) texture_number = 0;
		if(texture_number > 4) texture_number = 4;
		
		BufferedReader br = null;
		BufferedWriter out = null;
		
		String file_txt = "";
		
		String triangle_txt = "";
		triangle_txt += "      <coordA>\n";
		triangle_txt += "        "; triangle_txt += Float.toString(aX); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(aY); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(aZ); triangle_txt += "\n";
		triangle_txt += "      </coordA>\n";
		triangle_txt += "      <coordB>\n";
		triangle_txt += "        "; triangle_txt += Float.toString(bX); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(bY); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(bZ); triangle_txt += "\n";
		triangle_txt += "      </coordB>\n";
		triangle_txt += "      <coordC>\n";
		triangle_txt += "        "; triangle_txt += Float.toString(cX); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(cY); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(cZ); triangle_txt += "\n";
		triangle_txt += "      </coordC>\n";
		triangle_txt += "      <texture_number>\n";
		triangle_txt += "        "; triangle_txt += Float.toString(texture_number); triangle_txt += "\n";
		triangle_txt += "      </texture_number>\n";
		triangle_txt += "      <refractivity>\n";
		triangle_txt += "        "; triangle_txt += Float.toString(refractivity); triangle_txt += "\n";
		triangle_txt += "      </refractivity>\n";
		triangle_txt += "      <reflectivity>\n";
		triangle_txt += "        "; triangle_txt += Float.toString(reflectivity); triangle_txt += "\n";
		triangle_txt += "      </reflectivity>\n";
		triangle_txt += "      <color>\n";
		triangle_txt += "        "; triangle_txt += Float.toString(cR); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(cG); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(cB); triangle_txt += "\n";
		triangle_txt += "        "; triangle_txt += Float.toString(cA); triangle_txt += "\n";
		triangle_txt += "      </color>\n";
		triangle_txt += "    </triangle>\n";
		triangle_txt += "    <triangle>\n";
		
		try {
			String line;
			boolean tag_cnt = false, tag_triangles = false;
 
			br = new BufferedReader(new FileReader(filename));
 
			while ((line = br.readLine()) != null) 
			{
				if(tag_cnt)
				{
					int trianglescount = Integer.parseInt(line.trim()) + 1;
					
					file_txt += "    "; file_txt += Integer.toString(trianglescount); file_txt += "\n";
					tag_cnt = false;
					continue;
				}
				file_txt += line;
				file_txt += "\n";
				
				if(line.trim().compareTo("<trianglescount>") == 0)  { tag_cnt = true; continue; }
				if(line.trim().compareTo("<triangles>") == 0)       { tag_triangles = true; continue; }
				
				if(tag_triangles)
				{
					file_txt += triangle_txt;
					tag_triangles = false;
					continue;
				}				
			}
			out = new BufferedWriter(new FileWriter(filename));
	        out.write(file_txt);
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(br != null)br.close();
				if(out != null) out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * adds a light source to the scene
	 * @param x x-coordinate of the light source
	 * @param y x-coordinate of the light source
	 * @param z x-coordinate of the light source
	 */
	public static void addLight(float x, float y, float z)
	{
		BufferedReader br = null;
		BufferedWriter out = null;
		
		String file_txt = "";
		
		String light_txt = "";
		light_txt += "      "; light_txt += Float.toString(x); light_txt += "\n";
		light_txt += "      "; light_txt += Float.toString(y); light_txt += "\n";
		light_txt += "      "; light_txt += Float.toString(z); light_txt += "\n";
		light_txt += "    </light>\n";
		light_txt += "    <light>\n";
		
		try {
			String line;
			boolean tag_cnt = false, tag_lights = false;
 
			br = new BufferedReader(new FileReader(filename));
 
			while ((line = br.readLine()) != null) 
			{
				if(tag_cnt)
				{
					int trianglescount = Integer.parseInt(line.trim()) + 1;
					
					file_txt += "    "; file_txt += Integer.toString(trianglescount); file_txt += "\n";
					tag_cnt = false;
					continue;
				}
				file_txt += line;
				file_txt += "\n";
				
				if(line.trim().compareTo("<lightscount>") == 0)  { tag_cnt = true; continue; }
				if(line.trim().compareTo("<lights>") == 0)       { tag_lights = true; continue; }
				
				if(tag_lights)
				{
					file_txt += light_txt;
					tag_lights = false;
					continue;
				}				
			}
			out = new BufferedWriter(new FileWriter(filename));
	        out.write(file_txt);
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(br != null)br.close();
				if(out != null) out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * writes a scene to file
	 * @param spheres contains the values of all spheres
	 * @param triangles contains the values of all triangles
	 * @param lights contains the values of all light sources
	 * @param spherescount number of spheres
	 * @param trianglescount number of triangles
	 * @param lightscount number of light sources
	 */
	public static void writeScene(	FloatBuffer spheres, FloatBuffer triangles, FloatBuffer lights,
									int spherescount, int trianglescount, int lightscount)
	{
		BufferedWriter out = null;
		
		String file_txt = "<?xml version='1.0' encoding='us-ascii'?>\n";
		file_txt += "<!-- scene description -->\n";
		file_txt += "<scene>\n";
		file_txt += "  <spherescount>\n";
		file_txt += "    " + Integer.toString(spherescount) + "\n";
		file_txt += "  </spherescount>\n";
		file_txt += "  <trianglescount>\n";
		file_txt += "    " + Integer.toString(trianglescount) + "\n";
		file_txt += "  </trianglescount>\n";
		file_txt += "  <lightscount>\n";
		file_txt += "    " + Integer.toString(lightscount) + "\n";
		file_txt += "  </lightscount>\n";
		file_txt += "  <spheres>\n";
		
		for(int i = 0; i < spherescount; i++)
		{
			file_txt += "    <sphere>\n";
			file_txt += "      <radius>\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 0)) + "\n";
			file_txt += "      </radius>\n";
			file_txt += "      <coordinates>\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 1)) + "\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 2)) + "\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 3)) + "\n";
			file_txt += "      </coordinates>\n";
			file_txt += "      <texture_number>\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 4)) + "\n";
			file_txt += "      </texture_number>\n";
			file_txt += "      <refractivity>\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 5)) + "\n";
			file_txt += "      </refractivity>\n";
			file_txt += "      <reflectivity>\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 6)) + "\n";
			file_txt += "      </reflectivity>\n";
			file_txt += "      <color>\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 7)) + "\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 8)) + "\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 9)) + "\n";
			file_txt += "        " + Float.toString(spheres.get(i * 11 + 10)) + "\n";
			file_txt += "      </color>\n";
			file_txt += "    </sphere>\n";
		}
		
		file_txt += "  </spheres>\n";
		file_txt += "  <triangles>\n";
		
		for(int i = 0; i < trianglescount; i++)
		{
			file_txt += "    <triangle>\n";
			file_txt += "      <coordA>\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 0)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 1)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 2)) + "\n";
			file_txt += "      </coordA>\n";
			file_txt += "      <coordB>\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 3)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 4)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 5)) + "\n";
			file_txt += "      </coordB>\n";
			file_txt += "      <coordC>\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 6)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 7)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 8)) + "\n";
			file_txt += "      </coordC>\n";
			file_txt += "      <texture_number>\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 9)) + "\n";
			file_txt += "      </texture_number>\n";
			file_txt += "      <refractivity>\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 10)) + "\n";
			file_txt += "      </refractivity>\n";
			file_txt += "      <reflectivity>\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 11)) + "\n";
			file_txt += "      </reflectivity>\n";
			file_txt += "      <color>\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 12)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 13)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 14)) + "\n";
			file_txt += "        " + Float.toString(triangles.get(i * 16 + 15)) + "\n";
			file_txt += "      </color>\n";
			file_txt += "    </triangle>\n";
		}
		
		file_txt += "  </triangles>\n";
		file_txt += "  <lights>\n";
		
		for(int i = 0; i < lightscount; i++)
		{
			file_txt += "    <light>\n";
			file_txt += "      <coordinates>\n";
			file_txt += "        " + Float.toString(lights.get(i * 3 + 0)) + "\n";
			file_txt += "        " + Float.toString(lights.get(i * 3 + 1)) + "\n";
			file_txt += "        " + Float.toString(lights.get(i * 3 + 2)) + "\n";
			file_txt += "      </coordinates>\n";
			file_txt += "    </light>\n";
		}
		
		file_txt += "  </lights>\n";
		file_txt += "</scene>\n";			
		
		try {
			out = new BufferedWriter(new FileWriter(filename));
	        out.write(file_txt);
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(out != null) out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}