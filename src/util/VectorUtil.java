package util;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

public class VectorUtil {
    
    public static float getAxis(int axis, Vector3f vector) {
        switch(axis) {
        case 0 : return vector.x;
        case 1 : return vector.y;
        case 2 : return vector.z;
        default : throw new IllegalArgumentException("axis="+axis+ " must be in {0,1,2}");
        }
    }
    
    public static void setAxis(int axis, float value, Vector3f vector) {
        switch(axis) {
        case 0 : vector.x = value; break;
        case 1 : vector.y = value; break;
        case 2 : vector.z = value; break;
        default : throw new IllegalArgumentException("axis="+axis+ " must be in {0,1,2}");
        }
    }
    
    public static void setAxis(int axis, Vector3f src, Vector3f dst) {
        switch(axis) {
        case 0 : dst.x = src.x; break;
        case 1 : dst.y = src.y; break;
        case 2 : dst.z = src.z; break;
        default : throw new IllegalArgumentException("axis="+axis+ " must be in {0,1,2}");
        }
    }
    
    public static Vector3f getMin(Vector3f v0, Vector3f v1) {
        Vector3f min = new Vector3f();
        min.x = Math.min(v0.x, v1.x);
        min.y = Math.min(v0.y, v1.y);
        min.z = Math.min(v0.z, v1.z);
        return min;
    }
    
    public static Vector3f getMax(Vector3f v0, Vector3f v1) {
        Vector3f min = new Vector3f();
        min.x = Math.max(v0.x, v1.x);
        min.y = Math.max(v0.y, v1.y);
        min.z = Math.max(v0.z, v1.z);
        return min;
    }
    
    public static float getMax(Vector3f vector) {
        return Math.max(Math.max(vector.x, vector.y), Math.max(vector.x, vector.z));
    }
    
    public static float getMin(Vector3f vector) {
        return Math.min(Math.min(vector.x, vector.y), Math.min(vector.x, vector.z));
    }
    
    public static void scale(Vector3f vector, Vector3f scale) {
        vector.x *= scale.x;
        vector.y *= scale.y;
        vector.z *= scale.z;
    }
    
    public static Vector3f normalize(Vector3f vector) {
        Vector3f v = new Vector3f(vector);
        v.normalise();
        return v;
    }
    
    public static Vector3f lerp(Vector3f s, Vector3f e, float scale) {
        Vector3f diff = new Vector3f();
        Vector3f.sub(new Vector3f(e), new Vector3f(s), diff);
        diff.scale(scale);
        return Vector3f.add(new Vector3f(s), diff, new Vector3f());
    }
    
    public static Vector3f cross(Vector3f v0, Vector3f v1) {
        return Vector3f.cross(v0, v1, new Vector3f());
    }
    
    static FloatBuffer dst = BufferUtils.createFloatBuffer(3);
    public static FloatBuffer getFromVector(Vector3f src) {
        dst.put(0, src.x); dst.put(1, src.y); dst.put(2, src.z);
        return dst;
    }
}
