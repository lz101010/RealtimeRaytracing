package util;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
/**
 * 
 * @author Lukas Zeller
 * @brief simple camera class
 *
 */
public class Camera {
    private Matrix4f view = new Matrix4f();
    protected Vector3f eyePos = new Vector3f(0, 0, -1), viewDir = new Vector3f(0, 0, 1), upDir = new Vector3f(0, 1, 0), sideDir = new Vector3f(1, 0, 0);
    protected float phi, theta;
    
    /**
     * ctor, sets the view matrix
     */
    public Camera() {
        this.view.setIdentity();
        this.updateView();
    }
    
    /**
     * view matrix getter
     * @return view matrix
     */
    public Matrix4f getView() {
        return this.view;
    }
    
    /**
     * side-direction getter
     * @return side-direction
     */
    public Vector3f getSideDir() {
        return new Vector3f(this.sideDir);
    }
    
    /**
     * view-direction getter
     * @return view-direction
     */
    public Vector3f getViewDir() {
        return new Vector3f(this.viewDir);
    }
    
    /**
     * up-direction getter
     * @return up-direction
     */
    public Vector3f getUpDir() {
        return new Vector3f(this.upDir);
    }
    
    /**
     * camera position getter
     * @return camera position
     */
    public Vector3f getEyePos() {
        return new Vector3f(this.eyePos);
    }
    
    /**
     * updates current view matrix
     */
    public void updateView() {
        MatrixUtil.lookToLH(this.eyePos, this.viewDir, this.upDir, this.view);
    }
}
