package util;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * @author Lukas
 * @brief utility class for matrix-calculations
 *
 */
public class MatrixUtil {
    
    public static Matrix4f IDENTITY = new Matrix4f();
    
    static {
        IDENTITY.setIdentity();
    }
    
    /**
     * checks whether a matrix has been initialized
     * @param mat tested matrix
     * @return new matrix, if matrix hasn't been initialized yet, mat otherwise
     */
    static Matrix4f checkMatrix(Matrix4f mat) {
        if(mat == null) return new Matrix4f();
        return mat;
    }
    
    /**
     * calculates the view matrix from given vectors
     * @param eye camera position
     * @param dir camera looking direction
     * @param up camera upwards direction
     * @param dst destination matrix
     * @return dst
     */
    public static Matrix4f lookToLH(Vector3f eye, Vector3f dir, Vector3f up, Matrix4f dst) {
        if(dst == null) dst = new Matrix4f();

        Vector3f zAxis = VectorUtil.normalize(dir);
        Vector3f yAxis = new Vector3f(up);
        Vector3f xAxis = VectorUtil.cross(yAxis, zAxis);
        xAxis.normalise();
        yAxis = VectorUtil.cross(zAxis, xAxis);
              
        dst.m00 = xAxis.x;
        dst.m10 = xAxis.y;
        dst.m20 = xAxis.z;
        dst.m30 = -(eye.x*xAxis.x+eye.y*xAxis.y+eye.z*xAxis.z);

        dst.m01 = yAxis.x;
        dst.m11 = yAxis.y;
        dst.m21 = yAxis.z;
        dst.m31 = -(eye.x*yAxis.x+eye.y*yAxis.y+eye.z*yAxis.z);
        
        dst.m02 = zAxis.x;
        dst.m12 = zAxis.y;
        dst.m22 = zAxis.z;
        dst.m32 = -(eye.x*zAxis.x+eye.y*zAxis.y+eye.z*zAxis.z);
        
        dst.m03 = 0;
        dst.m13 = 0;
        dst.m23 = 0;
        dst.m33 = 1;
        return dst;
    }
    
    static FloatBuffer MAT_BUFFER = BufferUtils.createFloatBuffer(16);
    /**
     * creates a float buffer from a given matrix
     * @param src matrix to be translated
     * @return corresponding float buffer
     */
    public static FloatBuffer getFromMatrix(Matrix4f src) {
        src.store(MAT_BUFFER);
        MAT_BUFFER.position(0);
        return MAT_BUFFER;
    }
}
