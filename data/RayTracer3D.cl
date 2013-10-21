#define PI 3.14159265
#define PI_MUL2 2*PI
#define LEAF 0

__constant int   antiAliasingSamples  = 1;
__constant int   maxTraceDepth = 10;
__constant int   lightReach = 20;
__constant float maxRenderDist = 5000;
__constant int w = 512;
__constant int h = 512;

__constant float4 ambientColor = (float4)(0.01, 0.01, 0.01, 0);
__constant float4 backgroundColor = (float4)(0, 0, 0, 0);
__constant float4 backgroundColor2 = (float4)(0.2, 0.2, 0.2, 0);
__constant float4 backgroundColor3 = (float4)(0.5, 0.5, 0.5, 0);

typedef struct Ray
{
	float4 origin;
	float4 dir;
} Ray;

typedef struct Sphere
{
	float radius;
	float coordX;
	float coordY;
	float coordZ;
	float texture;
	float refrac;
	float reflec;
	float colorR;
	float colorG;
	float colorB;
	float colorA;
} Sphere;

typedef struct Triangle
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
	float texture;
	float refrac;
	float reflec;
	float colorR;
	float colorG;
	float colorB;
	float colorA;
} Triangle;

typedef struct Light
{
	float coordX;
	float coordY;
	float coordZ;
} Light;

float4 getTextureColor(int width, int u, int v, __global float* texture)
{
	int index = (3 * (u * width + v));
	return (float4)(texture[index], texture[index + 1], texture[index + 2], 0);   
}

// creates a ray from given camera information
Ray getRay(__global float* eyePos, __global float* view, int xid, int yid)
{
	Ray ray;
	float4 eye = (float4)(eyePos[0], eyePos[1], eyePos[2], 0);
    float4 p = normalize((float4)(2*xid/(float)w - 1, 2*yid/(float)h - 1, 4, 0));
    float4 r;
    
    //view transformation
    r.x = dot(p, (float4)(view[0],view[1],view[2],view[3]));
    r.y = dot(p, (float4)(view[4],view[5],view[6],view[7]));
    r.z = dot(p, (float4)(view[8],view[9],view[10],view[11]));
    r.w = 0.0f;
    
    ray.origin = eye;
    ray.dir = r;
    
    return ray;
}

// reflects a ray V at normal N
float4 reflect(float4 V, float4 N)
{
	return V - 2.0f * dot(V, N) * N;
}

// refracts a vector V at normal N with refraction index refrIndex
float4 refract(float4 V, float4 N, float refrIndex)
{
    float cosT1 = -dot(N, V);
    float cosT2 = sqrt(1.0f - refrIndex * refrIndex * (1.0f - cosT1 * cosT1));
    if(cosT1 > 0)
    {
		return (refrIndex * V) + (refrIndex * cosT1 - cosT2) * N;
	}
	else
	{
		return (refrIndex * V) + (refrIndex * cosT1 + cosT2) * N;
	}
}

// calculates hitpoints of ray with sphere
// distance saved in t
// true if sphere hit, false else
bool raySphere(float radius, float4 M, Ray r, float *t)
{
	// get point on sphere
	float4 rayToCenter = r.origin - M;
	float alpha = -dot(r.dir, rayToCenter);
	float4 q = r.origin + alpha * r.dir;
	
	// get shortest distance from ray to sphere center
	q = q - M;
	float dist = dot(q, q);
	
	if(dist > radius * radius) { return false; }
	
	// distance from q to hitpoint via pythagoras
	float x = sqrt(radius * radius - dist);
	
	if(alpha - x >= 0.001)     { *t = alpha - x; }
	else if(alpha + x > 0.001) { *t = alpha + x; }
	else                       { return false; } 
	return true;
}

// calculates hitpoints of ray with triangle
// distance saved in t
// values beta and gamma saved in respective float arguments
// true if triangle hit, false else
bool rayTriangle(float4 A, float4 B, float4 C, Ray r, float *t, float *beta, float *gamma)
{
	// get normal
	float4 b = B - A;
	float4 c = C - A;
	float4 n  = cross(b, c);
	n = normalize(n);
	float d = dot(n, A);
	
	float bb = dot(b, b);
	float bc = dot(b, c);
	float cc = dot(c, c);
	
	// get parameters beta and gamma
	float D = 1.0 / (cc * bb - bc * bc);
	
	float4 uBeta  = b * cc * D - c * bc * D;
	float4 uGamma = c * bb * D - b * bc * D;
	
	float kBeta  = (-1) * dot(A, uBeta);
	float kGamma = (-1) * dot(A, uGamma);
	
	float rn = dot(n, r.dir);
	if(rn * rn < 0.0000001) { return false; }
	
	*t = (d - dot(r.origin, n)) / rn;
	if(*t <= 0) { return false; }
	
	float4 q = r.origin + *t * r.dir;
	
	*beta = dot(uBeta, q) + kBeta;
	if(*beta < 0 || *beta > 1)  { return false; }
	
	*gamma = dot(uGamma, q) + kGamma;
	if(*gamma < 0 || *gamma > 1) { return false; }
	
	float alpha = 1 - *beta - *gamma;
	if(alpha < 0) { return false; }
	
	return true;
}

// calculates the lighting thru all visible light sources, normal N, vector V
// hitpoint, given color and lighting factor
float4 phongLighting(
int count,
__global Light* lights,
float4 color,
float4 hitpoint,
float4 N,
float4 V, 
float factor)
{
	int c = count;
	for(int i = 0; i < c % 100; i++)
	{
		count /= 100;
		Light light = lights[count];
		float4 lightsource = (float4)(light.coordX, light.coordY, light.coordZ, 0);
		float4 L = normalize(lightsource - hitpoint);
		float4 R = reflect(L, N);
		
		float d = dot(L, N);
		if(d > 0) color *= (1 + d * 1);
		d = dot(V,R);
		if(d > 0) color *= (1 + pow(d, 2) * factor * (2 + 2) / (PI * 2));		
	}
	return color;
}

// calculates the color of a sphere
float4 getColorS(
Sphere s,
__global Light* lights,
int count,
float4 hitpoint,
float4 origin,
__global float* texture,
float factor)
{	
	float4 M = (float4)(s.coordX, s.coordY, s.coordZ, 0);
	float4 v_p = (hitpoint - M);
	float4 v_n = (float4)(0, 0, 1, 0);
	float4 v_e = (float4)(0, 1, 0, 0);
	
	float4 color = (float4)(s.colorR, s.colorG, s.colorB, 0);
	if(texture != 0)
	{
		// get texture coordinates
		float u = 0, v = 0;
		float theta = acos(v_p.y  / s.radius);
		float phi = atan2(v_p.z, v_p.x);
		if(phi < 0) phi += PI_MUL2;
	
		u = phi / (PI_MUL2);
		v = (PI - theta)/PI; 
		
		color = getTextureColor(512, (int)(u * 511), (int)(v * 511), texture);
	}
	return phongLighting(count, lights, color, hitpoint, (hitpoint - M) / s.radius, normalize(hitpoint - origin), factor);	
}

// calculates the color of a triangle
float4 getColorT(
Triangle t,
__global Light* lights,
int count,
float4 hitpoint,
float4 origin,
__global float* texture,
float factor, float beta, float gamma)
{
	float4 color = (float4)(t.colorR, t.colorG, t.colorB, 0);
	
	if(texture != 0)
	{
		color = getTextureColor(512, (int)(gamma * 511), (int)(beta * 511), texture);
	}
	
	float4 A = (float4)(t.coordAX, t.coordAY, t.coordAZ, 0);
	float4 B = (float4)(t.coordBX, t.coordBY, t.coordBZ, 0);
	float4 C = (float4)(t.coordCX, t.coordCY, t.coordCZ, 0);
		
	return phongLighting(count, lights, color, hitpoint, -normalize(cross(B - A, C - A)), normalize(hitpoint - origin), factor);		
}

// calculates all visible light sources from the point origin
int countLights(
__global Sphere* spheres,
__global Triangle* triangles,
__global Light* lights,
int spherescount,
int trianglescount,
int lightscount,
float4 origin, int ot)
{
	Ray shadowRay;
	shadowRay.origin = origin;
	int result = 0, count = 0;
	bool test = true;
	
	for(int i = 0; i < lightscount; i++)
	{
		test = true;
		float tmp = 0, dummy1 = 0, dummy2 = 0;
		
		Light light = lights[i];
		float4 lightsource = (float4)(light.coordX, light.coordY, light.coordZ, 0);
		float distToLight = sqrt(dot(lightsource - shadowRay.origin, lightsource - shadowRay.origin));
		
   		shadowRay.dir = (lightsource - shadowRay.origin) / distToLight;
   		
   		// possibly a sphere is in the way
   		for(int j = 0; j < spherescount && test; j++)
   		{
   			Sphere s = spheres[j];
   			if(raySphere(s.radius, (float4)(s.coordX, s.coordY, s.coordZ, 0), shadowRay, &tmp) && tmp > 0.001 && tmp < distToLight)
   			{
   				if(ot % 10 == 1 && ot / 10 == j) continue;
   				test = false;
   			}
   		}
   		if(!test) continue;
   		
   		// possibly a triangle is in the way
   		for(int j = 0; j < trianglescount && test; j++)
   		{
   			Triangle t = triangles[j];
   			float4 A = (float4)(t.coordAX, t.coordAY, t.coordAZ, 0);
    		float4 B = (float4)(t.coordBX, t.coordBY, t.coordBZ, 0);
    		float4 C = (float4)(t.coordCX, t.coordCY, t.coordCZ, 0);
   			if(rayTriangle(A, B, C, shadowRay, &tmp, &dummy1, &dummy2) && tmp > 0.001 && tmp < distToLight)
   			{
   				if(ot % 10 == 2 && ot / 10 == j) continue;
   				test = false;
   			} 
   		}
   		if(!test) continue;

		count++;
   		result += i; 
   		result *= 100;
   	}
   	// result = count + 10 * i_1 + 100 * i_2 + ... + 10^n * i_n 
   	result += count;
	return result;
}

// calculates the color of a simple ray
float4 intersect(__global Sphere *spheres, 
				__global Triangle *triangles,
				__global Light *lights,
				         int spherescount,
				         int trianglescount,
				         int lightscount,
						 Ray *ray, 
				__global float* tex01,
				__global float* tex02,
				__global float* tex03,
				__global float* tex04,
				__global float* tex05,
				bool *delete, int *object)
{
	float dist = maxRenderDist, beta = 0, gamma = 0;
	int objectType, index;

	for(int i = 0; i < spherescount; i++)
	{
		float tmp = 0.0f;
		Sphere sphere = spheres[i];
		float radius = sphere.radius;
		float4 M = (float4)(sphere.coordX, sphere.coordY, sphere.coordZ, 0);
		if(raySphere(radius, M, *ray, &tmp) && tmp < dist)
		{
			dist = tmp;
			objectType = 1;
			index = i;
		}
	}
	
	for(int i = 0; i < trianglescount; i++)
	{
		float tmp = 0, b = 0, g = 0;
		Triangle triangle = triangles[i];
		float4 A = (float4)(triangle.coordAX, triangle.coordAY, triangle.coordAZ, 0);
		float4 B = (float4)(triangle.coordBX, triangle.coordBY, triangle.coordBZ, 0);
		float4 C = (float4)(triangle.coordCX, triangle.coordCY, triangle.coordCZ, 0); 
		if(rayTriangle(A, B, C, *ray, &tmp, &b, &g) && tmp < dist && tmp > 0.01)
		{
			beta = b;
			gamma = g;
			dist = tmp;
			objectType = 2;
			index = i;
		}
	}
	
	if(dist == maxRenderDist)
	{
		if(tex01 == 0)
		{
			*delete = 1;
			return backgroundColor;
		}
		// skydome
		float4 v_p = ray->dir;
	
		float u = 0, v = 0;
		float theta = acos(v_p.y);
		float phi = atan2(v_p.z, v_p.x);
		if(phi < 0) phi += PI_MUL2;
	
		u = phi / (PI_MUL2);
		v = (PI - theta)/PI;
		
		return getTextureColor(2048, (int)(u * 2047), (int)(v * 2047), tex05);
	}

	//else
	
	if(*delete)
	{
    	switch(objectType)
    	{
    	case 1 : spheres[index].colorA = 1; break;
    	case 2 : triangles[index].colorA = 1; break;
    	
    	}
    	return (float4)(0.5, 0.5, 0.5, 1.0f);
	}
	
	float4 hitpoint = ray->origin + dist * ray->dir;
	int count = countLights(spheres, triangles, lights, spherescount, trianglescount, lightscount, hitpoint, objectType + index * 10);
	if((count % 100) == 0)
	{
		*delete = 1; 
		return (float4)(0,0,0,0);
	}
		
	float4 color = backgroundColor;
		
	if(objectType == 1)
	{
		Sphere s = spheres[index];
		if(tex01 == 0)
		{
			color = getColorS(s, lights, count, hitpoint, ray->origin, 0, 0);
			*object = index * 10 + 1;
		}
		else
		{
			switch((int) s.texture)
			{
    		case 1: color = getColorS(s, lights, count, hitpoint, ray->origin, tex01, 0); break;
    		case 2: color = getColorS(s, lights, count, hitpoint, ray->origin, tex02, 1.5); break;
    		case 3: color = getColorS(s, lights, count, hitpoint, ray->origin, tex03, 0.8); break;
    		case 4: color = getColorS(s, lights, count, hitpoint, ray->origin, tex04, 0.7); break;
    		default: color = getColorS(s, lights, count, hitpoint, ray->origin, 0, 0); break;
			}
		}
	}
	else
	{
		Triangle t = triangles[index];
		if(tex01 == 0)
		{
			color = getColorT(t, lights, count, hitpoint, ray->origin, 0, 0, beta, gamma);
			*object = index * 10 + 2;
		}
		else
		{
			switch((int) t.texture)
			{
    		case 1: color = getColorT(t, lights, count, hitpoint, ray->origin, tex01, 0, beta, gamma); break;
    		case 2: color = getColorT(t, lights, count, hitpoint, ray->origin, tex02, 1.0, beta, gamma); break;
	    	case 3: color = getColorT(t, lights, count, hitpoint, ray->origin, tex03, 0.8, beta, gamma); break;
    		case 4: color = getColorT(t, lights, count, hitpoint, ray->origin, tex04, 0.7, beta, gamma); break;
    		default: color = getColorT(t, lights, count, hitpoint, ray->origin, 0, 0, beta, gamma); break;
			}
		}
	}
	ray->origin = hitpoint;
	return mix(ambientColor, color, (float)(count % 100)/lightscount);
}

__kernel void ray3Dnormal(
__write_only image2d_t texture,
__global Sphere* spheres, 
__global Triangle* triangles,
__global Light* lights,
uint spherescount,
uint trianglescount,
uint lightscount,
__global float* eyePos, 
__global float* view,
__global int* delete,
__global float* tex01,
__global float* tex02,
__global float* tex03,
__global float* tex04,
__global float* tex05)

{
    uint xid = get_global_id(0);
    uint yid = get_global_id(1);
    
    bool del = 0;
    // mark the middle
    if(xid == 256 && yid == 256)
    {
    	if(delete[0] > 1)
    		del = true;
    	else
    	{
    		write_imagef(texture, (int2)(xid, yid), (float4)(0.5, 0.5, 0.5, 0));
    		return;
    	}		
    }
    
    Ray ray = getRay(eyePos, view, xid, yid);
    
    float4 color = intersect(spheres, triangles, lights, spherescount, trianglescount, lightscount, &ray, tex01, tex02, tex03, tex04, tex05, &del, 0);
    
    if(color.w == 1)
    {
    	delete[0] = 0;
    	write_imagef(texture, (int2)(xid, yid), (float4)(0.5, 0.5, 0.5, 0));
    	return;
    }
    
    write_imagef(texture, (int2)(xid, yid), color);
}

__kernel void ray3Drefrac(
__write_only image2d_t texture,
__global Sphere* spheres, 
__global Triangle* triangles,
__global Light* lights,
uint spherescount,
uint trianglescount,
uint lightscount,
__global float* eyePos, 
__global float* view)
{
    uint xid = get_global_id(0);
    uint yid = get_global_id(1);
    
    int object = 0;
    bool done = false;
    float refractivity = 1;
        
    Ray ray = getRay(eyePos, view, xid, yid);
    
    float4 color = intersect(spheres, triangles, lights, spherescount, trianglescount, lightscount, &ray, 0, 0, 0, 0, 0, &done, &object);
    
    if(done)
    {
    	write_imagef(texture, (int2)(xid, yid), color);    
    	return;
    }        
    
    int index = object / 10;
	int type = object % 10;
	float4 n;
	if(type == 1)
	{
		Sphere s = spheres[index];
		n = (ray.origin - (float4)(s.coordX, s.coordY, s.coordZ, 0)) / s.radius;
		refractivity *= s.refrac;
	}
	else
	{
		Triangle t = triangles[index];
		float4 A = (float4)(t.coordAX, t.coordAY, t.coordAZ, 0);
		float4 B = (float4)(t.coordBX, t.coordBY, t.coordBZ, 0);
		float4 C = (float4)(t.coordCX, t.coordCY, t.coordCZ, 0);
		n = normalize(cross(B-A, C-A));
		refractivity *= t.refrac;
	}
    
   	Ray refractionRay;
	refractionRay.origin = ray.origin;
	refractionRay.dir = refract(ray.dir, n, 1.05);

	for(int i = 0; i < maxTraceDepth && !done && refractivity > 0; i++)
	{
		float4 c = intersect(spheres, triangles, lights, spherescount, trianglescount, lightscount, &refractionRay, 0, 0, 0, 0, 0, &done, &object);
		
		index = object / 10;
		type = object % 10;
		if(type == 1)
		{
			Sphere s = spheres[index];
			n = (refractionRay.origin - (float4)(s.coordX, s.coordY, s.coordZ, 0)) / s.radius;
			refractivity *= s.refrac;
		}
		else
		{
			Triangle t = triangles[index];
			float4 A = (float4)(t.coordAX, t.coordAY, t.coordAZ, 0);
			float4 B = (float4)(t.coordBX, t.coordBY, t.coordBZ, 0);
			float4 C = (float4)(t.coordCX, t.coordCY, t.coordCZ, 0);
			n = normalize(cross(B-A, C-A));
			refractivity *= t.refrac;
		}
		refractionRay.dir = refract(refractionRay.dir, n, 1.05);
		if(!done)
			color = mix(color, c, refractivity);
		else
			color = (float4)(0,0,0,0);
	}
	write_imagef(texture, (int2)(xid, yid), color);    
}

__kernel void ray3Dreflec(
__write_only image2d_t texture,
__global Sphere* spheres, 
__global Triangle* triangles,
__global Light* lights,
uint spherescount,
uint trianglescount,
uint lightscount,
__global float* eyePos, 
__global float* view)
{
    uint xid = get_global_id(0);
    uint yid = get_global_id(1);
    
    int object = 0;
    bool done = false;
    float reflectivity = 1;
        
    Ray ray = getRay(eyePos, view, xid, yid);
    
    float4 color = intersect(spheres, triangles, lights, spherescount, trianglescount, lightscount, &ray, 0, 0, 0, 0, 0, &done, &object);
    
    if(done)
    {
    	write_imagef(texture, (int2)(xid, yid), color);    
    	return;
    }        
    
    int index = object / 10;
	int type = object % 10;
	float4 n;
	if(type == 1)
	{
		Sphere s = spheres[index];
		n = (ray.origin - (float4)(s.coordX, s.coordY, s.coordZ, 0)) / s.radius;
		reflectivity *= s.reflec;
	}
	else
	{
		Triangle t = triangles[index];
		float4 A = (float4)(t.coordAX, t.coordAY, t.coordAZ, 0);
		float4 B = (float4)(t.coordBX, t.coordBY, t.coordBZ, 0);
		float4 C = (float4)(t.coordCX, t.coordCY, t.coordCZ, 0);
		n = normalize(cross(B-A, C-A));
		reflectivity *= t.reflec;
	}
    
   	Ray reflectionRay;
	reflectionRay.origin = ray.origin;
	reflectionRay.dir = reflect(ray.dir, n);

	for(int i = 0; i < maxTraceDepth && !done && reflectivity > 0; i++)
	{
		float4 c = intersect(spheres, triangles, lights, spherescount, trianglescount, lightscount, &reflectionRay, 0, 0, 0, 0, 0, &done, &object);
		color = mix(c, color, 1 - reflectivity);
		
		index = object / 10;
		type = object % 10;
		if(type == 1)
		{
			Sphere s = spheres[index];
			n = (reflectionRay.origin - (float4)(s.coordX, s.coordY, s.coordZ, 0)) / s.radius;
			reflectivity *= s.reflec;
		}
		else
		{
			Triangle t = triangles[index];
			float4 A = (float4)(t.coordAX, t.coordAY, t.coordAZ, 0);
			float4 B = (float4)(t.coordBX, t.coordBY, t.coordBZ, 0);
			float4 C = (float4)(t.coordCX, t.coordCY, t.coordCZ, 0);
			n = normalize(cross(B-A, C-A));
			reflectivity *= t.reflec;
		}
		reflectionRay.dir = reflect(reflectionRay.dir, n);
		if(done)
			color = mix(backgroundColor, color, 1 - reflectivity);
	}
    
    write_imagef(texture, (int2)(xid, yid), color);    
}