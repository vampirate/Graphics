package ass2.spec;

import javax.media.opengl.GL2;

import com.jogamp.opengl.util.gl2.GLUT;

public class Camera {

	private Terrain myTerrain;
	
	private int tiltAngle = 0;
	private int panAngle = 315;
    private double posX = 0.0;
    private double posZ = 0.0;
    
    private final static int TILT_RATE = 10;
    private final static int PAN_RATE = 10;
    private final static double MOVEMENT_RATE = 0.1;
    
    private boolean thirdPerson = false;
    
    public Camera(Terrain terrain, int panAngle, int posX, int posZ) {
    	this.myTerrain = terrain;
    	this.panAngle = panAngle;
    	this.posX = posX;
    	this.posZ = posZ;
    }
	
    public void display(GL2 gl) {
    	//gl.glMatrixMode(GL2.GL_MODELVIEW);
        //gl.glLoadIdentity();
        gl.glPushMatrix();
        
        gl.glRotated(tiltAngle, 1, 0, 0);
        gl.glRotated(panAngle, 0, 1, 0);
        gl.glTranslated(-posX, -(myTerrain.altitude(posX, posZ) + 0.5), -posZ);
    }
    
    public void thirdPerson() {
    	thirdPerson = true;
    }
    
    public void firstPerson() {
    	thirdPerson = false;
    }
    
    public void tiltUp() {
    	tiltAngle = (tiltAngle + TILT_RATE) % 360;
    }
    
    public void tiltDown() {
		tiltAngle = (tiltAngle - TILT_RATE) % 360;
    }
    
    public void panLeft() {
    	panAngle = (panAngle + PAN_RATE) % 360;
    }
    
    public void panRight() {
    	panAngle = (panAngle - PAN_RATE) % 360;
    }
    
	public void moveForward() {
		posX += MOVEMENT_RATE * Math.cos(Math.toRadians(panAngle + 90));
    	posZ += MOVEMENT_RATE * Math.sin(Math.toRadians(panAngle + 90));
	}
	
	public void moveBackward() {
		posX -= MOVEMENT_RATE * Math.cos(Math.toRadians(panAngle + 90));
    	posZ -= MOVEMENT_RATE * Math.sin(Math.toRadians(panAngle + 90));
	}
	
	public void strafeLeft() {
		posX += MOVEMENT_RATE * Math.cos(Math.toRadians(panAngle));
    	posZ += MOVEMENT_RATE * Math.sin(Math.toRadians(panAngle));
	}
	
	public void strafeRight() {
		posX -= MOVEMENT_RATE * Math.cos(Math.toRadians(panAngle));
    	posZ -= MOVEMENT_RATE * Math.sin(Math.toRadians(panAngle));
	}
}