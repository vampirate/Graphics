package ass2.spec;

import java.awt.List;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;


public class Game extends JFrame implements GLEventListener, KeyListener{
    private Terrain myTerrain;
    private int angle = 0;
    
    //DERP
    private Camera camera;
    private int teapotAngle = 0;
    //TEXTURE STUFF
    
	private boolean day = true;
	private boolean torch = false;
	private boolean third = false;
	private double sunangle = 90;
    float[] dir;

    public Game(Terrain terrain) {
    	super("Assignment 2");
        myTerrain = terrain;
    }
    
    public void run() {
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(glp);
		GLJPanel panel = new GLJPanel();
		panel.addGLEventListener(this);
		panel.addKeyListener(this);     
		FPSAnimator animator = new FPSAnimator(60);
		animator.add(panel);
		animator.start();
		getContentPane().add(panel);
		setSize(800, 600);        
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		camera = new Camera(myTerrain, 315, 0, 0);
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        Terrain terrain = LevelIO.load(new File(args[0]));
        Game game = new Game(terrain);
        game.run();
    }

	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
    	gl.glEnable(GL2.GL_DEPTH_TEST);	//enable the depth
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); //clear the color
        
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity(); //load the identity matrix

        
        setUpTorch(gl); //set up the torch
        
        if (third) { //if the camera is third person
        	camera.thirdPerson(); //adjust it
        	gl.glTranslated(0, -0.3, 1);
        	gl.glPushMatrix();
        	gl.glRotated(teapotAngle, 1, 0, 0);
        	GLUT glut = new GLUT();
            glut.glutSolidTeapot(0.2f); //load the teapot
            gl.glPopMatrix();
        }
        
        camera.display(gl); //display it
        
        if (third) {
        	gl.glTranslated(0, -1, 0); //more adjustment 
        }
		if (day) {	//if it's day
	    	gl.glClearColor(1.0f * (float)(Math.sin(Math.toRadians(sunangle))), //adjust the background color like the sky
	    			0.98f* (float)(Math.sin(Math.toRadians(sunangle))),
	    			0.95f* (float)(Math.sin(Math.toRadians(sunangle))),
	    			0.0f); //set the background color
    	} else {
    		gl.glClearColor(-0.1f * (float)(Math.sin(Math.toRadians(sunangle))),
    				-0.1f * (float)(Math.sin(Math.toRadians(sunangle))),
    				-0.13f * (float)(Math.sin(Math.toRadians(sunangle))),
    				0.0f); //set the background color
    	}

        setUpLighting(gl); //setup the lighting

        myTerrain.draw(drawable); //draw the terrain
        gl.glPopMatrix();
	}
	
    public void setUpLighting(GL2 gl) {
    	dir = myTerrain.getSunlight();
    	double sunlength = Math.sqrt((double) (dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2])); //get the direction
    	float lightAmb[] = { 0.5f, 0.5f, 0.5f, 0.1f };
    	float lightDifAndSpec[] = { 0.8f, 0.8f, 0.7f, 1.0f };
    	if (sunangle >= 180) { //determines whether it's day or night according to the sun position
    		day = false;
    	} else {
    		day = true;
    	}
    	float lightPos[] = {0, 0, 0, 0};
    	if (day) { //set the lighting
    		lightPos[0] = -dir[0] * (float)(Math.cos(Math.toRadians(sunangle))/sunlength);
    		lightPos[1] = -dir[1] * (float)(Math.sin(Math.toRadians(sunangle))/sunlength);
    		lightPos[2] =  -dir[2] * (float)(Math.cos(Math.toRadians(sunangle))/sunlength);
    		lightPos[3] = 0;
    		lightDifAndSpec[0] = 1.0f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightDifAndSpec[1] =  1.0f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightDifAndSpec[2] =  1.0f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightDifAndSpec[3] =  1.0f;
    		lightAmb[0] = 0.5f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightAmb[1] =  0.5f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightAmb[2] =  0.5f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightAmb[3] =  0.5f;
    	} else {
    		lightPos[0] = dir[0] * (float)(Math.cos(Math.toRadians(sunangle))/sunlength);
    		lightPos[1] = dir[1] * (float)(Math.sin(Math.toRadians(sunangle))/sunlength);
    		lightPos[2] = dir[2] * (float)(Math.cos(Math.toRadians(sunangle))/sunlength);
    		lightPos[3] = 0;
    		lightDifAndSpec[0] = -0.1f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightDifAndSpec[1] =  -0.1f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightDifAndSpec[2] =  -0.3f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightDifAndSpec[3] =  -1.0f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightAmb[0] = -0.4f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightAmb[1] = -0.4f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightAmb[2] = -0.4f * (float)(Math.sin(Math.toRadians(sunangle)));
    		lightAmb[3] = 0.4f;
    	}
    	gl.glEnable(GL2.GL_LIGHT0); //enable lighting
    	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmb,0);
    	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDifAndSpec,0);
    	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightDifAndSpec,0);
    	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos,0);
    }
    
    private void setUpTorch(GL2 gl) {
    	float lightPos[] = {0, 0, 10, 1.0f};
    	float spotDirection[] = {0, 0, -1};  //set the torch position
    	gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPos,0);  
    	gl.glLightf(GL2.GL_LIGHT1, GL2.GL_SPOT_CUTOFF, 10); //adjust the cutoff bit
    	gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPOT_DIRECTION, spotDirection,0); //set the direction
    	gl.glLightf(GL2.GL_LIGHT1, 	GL2.GL_LINEAR_ATTENUATION, 0.1f); //and the tiny attenuatino
    	gl.glLightf(GL2.GL_LIGHT1, GL2.GL_SPOT_EXPONENT, 2.0f);
    	gl.glEnable(GL2.GL_LIGHTING);
    	float lightAmb2[] = {0.0f, 0.0f, 0.0f, 1.0f};
    	float lightDifAndSpec2[] = {1.0f, 1.0f, 1.0f, 1.0f};
    	float globAmb2[] = {0.05f, 0.05f, 0.05f, 1.0f};
    	
    	if (torch) {
    		lightDifAndSpec2[0] = 1.0f;
    		lightDifAndSpec2[1] = 1.0f;
    		lightDifAndSpec2[2] = 1.0f; //if the torch is on
    		lightDifAndSpec2[3] = 1.0f;
    	} else {
    		lightDifAndSpec2[0] = 0.0f; //dont emit light if it's off
    		lightDifAndSpec2[1] = 0.0f;
    		lightDifAndSpec2[2] = 0.0f;
    		lightDifAndSpec2[3] = 0.0f;
    	}
    	// Light properties.
    	gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, lightAmb2,0);
    	gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, lightDifAndSpec2,0);
    	gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, lightDifAndSpec2,0);

    	gl.glEnable(GL2.GL_LIGHT1);
    	gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, globAmb2,0);
    	gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, GL2.GL_TRUE); // enable local viewpoint.
	}
    


	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub
		
	}
	

public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
    	gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_NORMALIZE); //allows normals to be normalised automatically
        String str = "1.jpg"; //load the homer images
        myTerrain.loadTexture(gl, str, 0);
		str = "2.jpg";
		myTerrain.loadTexture(gl, str, 1);
		str = "3.jpg";
		myTerrain.loadTexture(gl, str, 2);
		str = "4.jpg";
		myTerrain.loadTexture(gl, str, 3);
		str = "5.jpg";
		myTerrain.loadTexture(gl, str, 4);
		str = "6.jpg";
		myTerrain.loadTexture(gl, str, 5);
		str = "7.jpg";
		myTerrain.loadTexture(gl, str, 6);
		str = "8.jpg";
		myTerrain.loadTexture(gl, str, 7);
		str = "9.jpg";
		myTerrain.loadTexture(gl, str, 8);
		str = "10.jpg";
		myTerrain.loadTexture(gl, str, 9);
		str = "11.jpg";
		myTerrain.loadTexture(gl, str, 10);
		str = "metal.jpg";
		myTerrain.loadTexture(gl, str, 11);
		str = "eye.jpg";
		myTerrain.loadTexture(gl, str, 12);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		System.out.println("reshape called");
		GL2 gl = drawable.getGL().getGL2();

        // calculate the aspect ratio of window
        double aspect = 1.0 * width / height;
		gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        
        // Use the GLU library to compute the new projection
        GLU glu = new GLU();
        glu.gluPerspective(90, 1.0, 0.1, 10);
        
        if(aspect >= 1){
            glu.gluOrtho2D(-aspect, aspect, -1.0, 1.0);  //so the perspective ratio stays the same
        } else {
        	glu.gluOrtho2D(-1, 1, -1.0/aspect, 1.0/aspect);
        }
        gl.glMatrixMode(GL2.GL_MODELVIEW);
	}


	public void keyPressed(KeyEvent e) {
	switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			camera.tiltUp();
			teapotAngle = (teapotAngle + 10) % 360;
			break;
		case KeyEvent.VK_DOWN:
			camera.tiltDown();
			teapotAngle = (teapotAngle - 10) % 360;
			break;
		case KeyEvent.VK_LEFT:
			camera.panLeft();
			break;
		case KeyEvent.VK_RIGHT:
			camera.panRight();
			break;
		case KeyEvent.VK_W:
			camera.moveForward();
			teapotAngle = (teapotAngle + 10) % 360;
			break;
		case KeyEvent.VK_F:
			torch = !torch;
			break;
		case KeyEvent.VK_R:
			//torch = !torch;
			break;
		case KeyEvent.VK_T:
			third = !third;
			break;
		case KeyEvent.VK_S:
			camera.moveBackward();
			teapotAngle = (teapotAngle - 10) % 360;
			break;
		case KeyEvent.VK_D:
			camera.strafeLeft();
			break;
		case KeyEvent.VK_A:
			camera.strafeRight();
			break;
		case KeyEvent.VK_Z:
			sunangle = (sunangle + 10) % 360;
			break;
		case KeyEvent.VK_X:
			if (sunangle > 180) {
			sunangle = 90;
			} else {
			sunangle = 270;
			};
			break;
		default:
			break;
	}
		
	}
	public void keyReleased(KeyEvent e) {
	}
	public void keyTyped(KeyEvent e) {
	}

}
