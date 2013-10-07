package util;

/**
 * 
 * @author Lukas Zeller
 * @brief extended camera, move and rotate functions
 *
 */
public class FPSCamera extends Camera {
    
    public FPSCamera() {
        super();
    }
    
    /**
     * moves the camera by given vector
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     */
    public void move(float x, float y, float z) {
        eyePos.x += z * viewDir.x + x * sideDir.x;
        eyePos.y += z * viewDir.y + x * sideDir.y + y;
        eyePos.z += z * viewDir.z + x * sideDir.z;
        this.updateView();
    }
    
    /**
     * rotates the camera by given angle
     * @param dphi up/down angle
     * @param dtheta left/right angle
     */
    public void rotate(float dphi, float dtheta) {
        this.phi = (float) ((this.phi + dphi) % (Math.PI * 2));
        this.theta += dtheta;
        if(this.theta < -Math.PI / 2) this.theta = (float) (-Math.PI / 2);
        if(this.theta > +Math.PI / 2) this.theta = (float) (+Math.PI / 2);
        
        float sinPhi = (float) Math.sin(this.phi);
        float cosPhi = (float) Math.cos(this.phi);
        float sinTheta = (float) Math.sin(this.theta);
        float cosTheta = (float) Math.cos(this.theta);
        
        this.sideDir.set(cosPhi, 0, -sinPhi);
        this.upDir.set(sinPhi*sinTheta, cosTheta, cosPhi*sinTheta);
        this.viewDir.set(sinPhi*cosTheta, -sinTheta, cosPhi*cosTheta);
        
        this.updateView();
    }
}
