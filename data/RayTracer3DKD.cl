#define PI 3.14159265
#define PI_MUL2 2*PI
#define LEAF 0
__constant float4 ambientColor = (float4)(0.1, 0.1, 0.1, 0);
__constant float4 backgroundColor = (float4)(1, 1, 1, 0);



typedef struct ToDo {
    int4 node;
    float min;
    float max;
} ToDo;

typedef struct Hit {
    float distance;
    uint hit;
    //uint iterations;
    uint index;
    uint help;
} Hit;

typedef struct Ray
{
	float4 origin;
	float4 dir;
} Ray;

typedef struct Sphere {
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
} Sphere;

typedef struct Triangle {
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
} Triangle;

typedef struct Light {
	float coordX;
	float coordY;
	float coordZ;
} Light;

float getAxis(float4 val, int axis) 
{
    switch(axis) 
    {
        case 0 : return val.x;
        case 1 : return val.y;
        case 2 : return val.z;
    }
    return -1;
}

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

bool rayTriangle(float4 A, float4 B, float4 C, Ray r, float *t)
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
	
	float beta = dot(uBeta, q) + kBeta;
	if(beta < 0)  { return false; }
	
	float gamma = dot(uGamma, q) + kGamma;
	if(gamma < 0) { return false; }
	
	float alpha = 1 - beta - gamma;
	if(alpha < 0) { return false; }
	
	return true;
}


//PBRT
int intersectP(float4 eye, float4 ray, float4 invRay, float4 boxmin, float4 boxmax, float* tmin, float* tmax) 
{
    float t0 = 0; float t1 = INFINITY;
    for(uint i = 0; i < 3; ++i) 
    {
        float tNear = (getAxis(boxmin, i) - getAxis(eye, i)) * getAxis(invRay, i);
        float tFar  = (getAxis(boxmax, i) - getAxis(eye, i)) * getAxis(invRay, i);
        if(tNear > tFar) 
        {
            float tmp = tNear;
            tNear = tFar;
            tFar = tmp;
        }
        if(t0 < tNear) t0 = tNear;
        if(t1 > tFar ) t1 = tFar;
        if(t0 > t1) return 0;
    }
    *tmin = t0;
    *tmax = t1;
    return 1;
}

void testSphereIntersection(
float4 eye, 
float4 ray, 
int4 node, 
__global Sphere* spheres, 
Hit* hit, 
__global uint* nodesContent) 
{
	// iterate thru all spheres in node (num_spheres = nodesContent[node.w] % 1000) 
    for(uint i = 1; i <= nodesContent[node.w] % 1000; i++) 
    {
    	// get the current index
        uint index = nodesContent[node.w + i];
        Sphere sphere = spheres[index];
        
        float4 M = (float4)(sphere.coordX, sphere.coordY, sphere.coordZ, 0);
        float t = 0;
        
        Ray r;
        r.origin = eye;
        r.dir = ray;
                
        if(!raySphere(sphere.radius, M, r, &t)) continue; 
        
        if(t < hit->distance) 
        {
            hit->hit = 1;
            hit->index = index;
            hit->distance = t;
        }
    } 
}

void testTriangleIntersection(
float4 eye,
float4 ray,
int4 node,
__global Triangle* triangles,
Hit* hit,
__global uint* nodesContent)
{
	for(uint i = 1; i <= nodesContent[node.w] / 1000; i++)
	{
		// get the current index
		uint index = nodesContent[node.w + i + (nodesContent[node.w] % 1000)] - nodesContent[node.w] % 1000;
		Triangle triangle = triangles[index];
		
		float4 A = (float4)(triangle.coordAX, triangle.coordAY, triangle.coordAZ, 0);
		float4 B = (float4)(triangle.coordBX, triangle.coordBY, triangle.coordBZ, 0);
		float4 C = (float4)(triangle.coordCX, triangle.coordCY, triangle.coordCZ, 0);
		float t = 0;
		
		Ray r;
		r.origin = eye;
		r.dir = ray;
		
		if(!rayTriangle(A, B, C, r, &t)) continue;
		
		if(t < hit->distance)
		{
			hit->hit = 2;
            hit->index = index;
            hit->distance = t;
		}
	}
}

float4 shadeSphere(
Sphere sphere,
__global Light* lights,
int lightscount,
float4 hitpoint,
float4* color)
{
   	for(int i = 0; i < lightscount; i++)
   	{
   		Light light = lights[i];
   		float dS = sqrt(dot((float4)(sphere.coordX, sphere.coordY, sphere.coordZ, 0) - (float4)(light.coordX, light.coordY, light.coordZ, 0),
   		 					(float4)(sphere.coordX, sphere.coordY, sphere.coordZ, 0) - (float4)(light.coordX, light.coordY, light.coordZ, 0)));
		if(dS < sphere.radius) return *color;
   		float dP = sqrt(dot(hitpoint - (float4)(light.coordX, light.coordY, light.coordZ, 0), 
   							hitpoint - (float4)(light.coordX, light.coordY, light.coordZ, 0)));
   		float x = (1 + (dS - dP) / sphere.radius) * 0.5;
   		*color *= x;
   		*color += 0.1f;   					
   	}
   	return *color;
}


__kernel void ray3dTKD(
__write_only image2d_t texture,
__global int4* nodes,
__global uint* nodesContent,
__global int* flags,
__global uint* axis,
__global float* splits,
__global float* eyePos, 
__global float* view,
__global Sphere* spheres,
__global Triangle* triangles,
__global Light* lights,
uint spherescount,
uint trianglescount,
uint lightscount) {

    uint xid = get_global_id(0);
    uint yid = get_global_id(1);
    uint w = get_global_size(0);
    uint h = get_global_size(1);
    float4 eye = (float4)(eyePos[0], eyePos[1], eyePos[2], 0);
    float4 p = normalize((float4)(2*xid/(float)w - 1, 2*yid/(float)h - 1, 4, 0));
    float4 ray;
    
    //view transformation
    ray.x = dot(p, (float4)(view[0],view[1],view[2],view[3]));
    ray.y = dot(p, (float4)(view[4],view[5],view[6],view[7]));
    ray.z = dot(p, (float4)(view[8],view[9],view[10],view[11]));
    ray.w = 0.0f;
    
    //global bounds
    float4 min = (float4)(-1000, -1000, -1000, 0);
    float4 max = (float4)(1000, 1000, 1000, 0);
    
    float tmin, tmax;
    float4 invRay = (float4)(1.0 / ray.x, 1.0 / ray.y, 1.0 / ray.z, 0);
    
    if(!intersectP(eye, ray, invRay,  min, max, &tmin, &tmax)) { write_imagef(texture, (int2)(xid, yid), (float4)(0,0,0,0) + ambientColor); return; } 
    if (tmin < 0.0f) tmin = 0.0f; //clamp near
    
    Hit hit;
    hit.hit = 0;
    hit.distance = INFINITY;
    int4 n = nodes[0];
    ToDo toDo[16];
    int pos = 0;
     while(1) {
        if(hit.distance < tmin) break;
        if(flags[n.x] != LEAF) {
            int axe = axis[n.x];
            float split = splits[n.x];
            float tplane = (split - getAxis(eye, axe)) * getAxis(invRay, axe);
            int4 c0; 
            int4 c1;
            int belowFirst = (getAxis(eye, axe) < split) || ((getAxis(eye, axe) == split) && (getAxis(ray, axe) >= 0));
            if(belowFirst) {
                c0 = nodes[n.y];
                c1 = nodes[n.z];
            } else {
                c0 = nodes[n.z];
                c1 = nodes[n.y];
            }
            if(tplane > tmax || tplane <= 0) {
                n = c0;
            } else if(tplane < tmin) {
                n = c1;
            } else {
                ToDo td;
                td.node = c1;
                td.min = tplane;
                td.max = tmax;
                toDo[pos] = td;
                pos++;
                n = c0;
                tmax = tplane;
            }
        } else {
            testSphereIntersection(eye, ray, n, spheres, &hit, nodesContent);
            testTriangleIntersection(eye, ray, n, spheres, &hit, nodesContent);
            if(pos > 0) {
                pos -= 1;
                n = toDo[pos].node;
                tmin = toDo[pos].min;
                tmax = toDo[pos].max;
            } else {
                break;
            }
        }
    }
   
    if(hit.hit == 1) 
    {
        Sphere sphere = spheres[hit.index];
        float4 color = (float4)(sphere.colorR, sphere.colorG, sphere.colorB, sphere.colorA);
        float4 hitpoint = eye + ray * hit.distance;
        
        shadeSphere(sphere, lights, lightscount, hitpoint, &color);

        write_imagef(texture, (int2)(xid, yid), color);
    } else if(hit.hit == 2)
    {
    	Triangle triangle = triangles[0];
    	float4 color = (float4)(triangle.colorR, triangle.colorG, triangle.colorB, triangle.colorA) + ambientColor;
    	
    	if(hit.index == 1) color = ambientColor + (float4)(1, 0, 0, 0);
    	
    	//else color = ambientColor + (float4)(0, 1, 0, 0);
    	//if(hit.index == 2) color = ambientColor + (float4)(0, 0, 1, 0);
    	//color = ambientColor + (float4)(1 - help, help, help * 0.2, 0) - ambientColor;
    	//color = ambientColor + (float4)(0, 1, 0, 0);
    	float factor = (1 - hit.distance / 1000) * (1 - hit.distance / 1000) * (1 - hit.distance / 1000);
    	write_imagef(texture, (int2)(xid, yid), color * factor);
    } else {
        write_imagef(texture, (int2)(xid, yid), (float4)(1,0,0,0));
    }
}
