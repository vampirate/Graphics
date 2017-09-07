package ass2.spec;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLProfile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * COMMENT: Comment HeightMap 
 *
 * @author malcolmr
 */
public class Terrain {

	private GL2 myGL;
    private Dimension mySize;
    private double[][] myAltitude;
    private List<Tree> myTrees;
    private List<Road> myRoads;
    private float[] mySunlight;

	private int nTex = 13;
    private double i = 0;
    public int[] texID = new int[nTex];
    private int[] textures = new int[nTex];
    
    // Colours
    private static final float[] WHITE = {0.9f, 0.9f, 0.9f, 0};
    private static final float[] BLACK = {0.1f, 0.1f, 0.1f, 0};
    private static final float[] RED = {0.7f, 0, 0, 0};
    private static final float[] GREEN = {0, 0.7f, 0, 0};
    private static final float[] BLUE = {0, 0, 0.7f, 0};
    private static final float[] BROWN = {0.5f, 0.2f, 0.1f, 1};
    
    // Road
    private int roadMode = FIXED_ALTITUDE;
    private final static int FIXED_ALTITUDE = 0;
    private final static int SPINE_ALTITUDE = 1;
    private final static int EDGE_ALTITUDE = 2;
    private final static double ROAD_ALTITUDE_OFFSET = 0.01;

    /**
     * Create a new terrain
     *
     * @param width The number of vertices in the x-direction
     * @param depth The number of vertices in the z-direction
     */
    public Terrain(int width, int depth) {
        mySize = new Dimension(width, depth);
        myAltitude = new double[width][depth];
        myTrees = new ArrayList<Tree>();
        myRoads = new ArrayList<Road>();
        mySunlight = new float[3];
    }
    
    public Terrain(Dimension size) {
        this(size.width, size.height);
    }

    public Dimension size() {
        return mySize;
    }

    public List<Tree> trees() {
        return myTrees;
    }

    public List<Road> roads() {
        return myRoads;
    }

    public float[] getSunlight() {
        return mySunlight;
    }

    /**
     * Set the sunlight direction. 
     * 
     * Note: the sun should be treated as a directional light, without a position
     * 
     * @param dx
     * @param dy
     * @param dz
     */
    public void setSunlightDir(float dx, float dy, float dz) {
        mySunlight[0] = dx;
        mySunlight[1] = dy;
        mySunlight[2] = dz;        
    }
    
    /**
     * Resize the terrain, copying any old altitudes. 
     * 
     * @param width
     * @param height
     */
    public void setSize(int width, int height) {
        mySize = new Dimension(width, height);
        double[][] oldAlt = myAltitude;
        myAltitude = new double[width][height];
        
        for (int i = 0; i < width && i < oldAlt.length; i++) {
            for (int j = 0; j < height && j < oldAlt[i].length; j++) {
                myAltitude[i][j] = oldAlt[i][j];
            }
        }
    }

    /**
     * Get the altitude at a grid point
     * 
     * @param x
     * @param z
     * @return
     */
    public double getGridAltitude(int x, int z) {
        return myAltitude[x][z];
    }

    /**
     * Set the altitude at a grid point
     * 
     * @param x
     * @param z
     * @return
     */
    public void setGridAltitude(int x, int z, double h) {
        myAltitude[x][z] = h;
    }

    /**
     * Get the altitude at an arbitrary point. 
     * Non-integer points should be interpolated from neighbouring grid points
     * 
     * TO BE COMPLETED
     * 
     * @param x
     * @param z
     * @return
     */
    public double altitude(double x, double z) {
    	
        double altitude = 0;
        double maxX = this.size().getWidth() - 1; //get the max distance so it doesnt go off the grid
    	double maxZ = this.size().getHeight() - 1;
		if ((x < maxX) && (z < maxZ) && (x != 0) && (z != 0)) { //if the coordinates is not on the edge of the map
			double r1 = ((int)x + 1 - x) * (double)this.getGridAltitude((int)x, (int)z) + //interpolate to get the altitude
				(x - (int)x) * (double)this.getGridAltitude((int)x + 1, (int)z);
			double r2 = ((int)x + 1 - x) * (double)this.getGridAltitude((int)x, (int)z + 1) + 
    				(x - (int)x) * (double)this.getGridAltitude((int)x + 1, (int)z + 1);
			altitude = ((int)z + 1 - z) * r1 + (z - (int)z) * r2;
		} else { //else if it's on the edge
			altitude = this.getGridAltitude((int)x, (int)z); //just get the altitude
		}
        return altitude;
    }

    /**
     * Add a tree at the specified (x,z) point. 
     * The tree's y coordinate is calculated from the altitude of the terrain at that point.
     * 
     * @param x
     * @param z
     */
    public void addTree(double x, double z) {
        double y = altitude(x, z);
        Tree tree = new Tree(x, y, z);
        myTrees.add(tree);
    }


    /**
     * Add a road. 
     * 
     * @param x
     * @param z
     */
    public void addRoad(double width, double[] spine) {
        Road road = new Road(width, spine);
        myRoads.add(road);        
    }
    
    public void draw(GLAutoDrawable drawable) {
    	myGL = drawable.getGL().getGL2();
    	
    	i = i + 0.1;
        if (i >= 9) {
        	i = 0;
        }
        
        myGL.glBindTexture(GL.GL_TEXTURE_2D, texID[(int) i]);
        int x = 0, z = 0;	 //x and z are the coordinates
        double y1 = 1, y2 = 1, y3 = 1, y4 = 1; //y is the altitude
        for (x = 0; x < this.size().getWidth() - 1; x++) {
        	for (z = 0; z < this.size().getHeight() - 1; z++) { //loop for each grid
        		y1 = this.getGridAltitude(x, z); //get the altitude and its neighbouring altitudes
        		y2 = this.getGridAltitude(x + 1, z);
        		y3 = this.getGridAltitude(x, z + 1);
        		y4 = this.getGridAltitude(x + 1, z + 1);
        		
        		myGL.glEnable(GL2.GL_POLYGON_OFFSET_FILL);//enable the thingy that pushes the polygon back a bit so they dont fight
        		myGL.glPolygonOffset((float)-0.1,(float)-0.1); 
            	myGL.glBegin(GL2.GL_TRIANGLES); //start drawing
            	myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, WHITE, 0);
        		myGL.glNormal3d((y1 - y2) * (z + z) + (y2 - y3) * (z + z + 1) + (y3-y1) * (z + 2 + z),//set the normal for the lighting
						-1,
						(y3 - y1));
        		myGL.glTexCoord2d(0,0);
        		myGL.glVertex3d(x, y1, z); //set the vertices
        		myGL.glTexCoord2d(1,0);
        		myGL.glVertex3d(x + 1, y2, z);
        		myGL.glTexCoord2d(0,1);
        		myGL.glVertex3d(x, y3, z + 1);
        		myGL.glEnd();
        		
            	myGL.glBegin(GL2.GL_TRIANGLES);//draw another triangle
            	myGL.glNormal3d((y4 - y3) * (z + 2 + z) + (y3 - y2) * (z + 1 + z) + (y2 - y4) * (z + z + 1), 
            				-1, 
            				(y4 - y2));
            	myGL.glTexCoord2d(1,1);
        		myGL.glVertex3d(x + 1, y4, z + 1);
        		myGL.glTexCoord2d(0,1);
        		myGL.glVertex3d(x, y3, z + 1);
        		myGL.glTexCoord2d(1,0);
        		myGL.glVertex3d(x + 1, y2, z);
        		myGL.glEnd();
       		
        		myGL.glPolygonOffset((float)0.1,(float)0.1); //pushes the triangle forward a bit so they dont fight
        		myGL.glBegin(GL2.GL_TRIANGLES);//draw another
        		myGL.glNormal3d((y1 - y4) * (z + z + 1) + (y4 - y3) * (z + z + 2) + (y3 - y1) * (z + 1 + z), -1, (y3 - y1));
        		myGL.glTexCoord2d(0,0);
        		myGL.glVertex3d(x, y1, z);
        		myGL.glTexCoord2d(1,1);
        		myGL.glVertex3d(x + 1, y4, z + 1);
        		myGL.glTexCoord2d(0,1);
        		myGL.glVertex3d(x, y3, z + 1);
        		myGL.glEnd();
        		
            	myGL.glBegin(GL2.GL_TRIANGLES);//another
            	myGL.glNormal3d((y4 - y1) * (z + 1 + z) + (y1 - y2) * (z + z) + (y2 - y4) * (z + z + 1), -1, (y4 - y1));
            	myGL.glTexCoord2d(1,1);
        		myGL.glVertex3d(x + 1, y4, z + 1);
        		myGL.glTexCoord2d(0,0);
	       		myGL.glVertex3d(x, y1, z);
	       		myGL.glTexCoord2d(1,0);
	       		myGL.glVertex3d(x + 1, y2, z);
	       		myGL.glEnd();
	       		myGL.glDisable(GL2.GL_POLYGON_OFFSET_FILL);//disable the offset or else it won't work
        	}
        }
        drawTrees(drawable);
        drawRoads(drawable);
    }
    
    public void drawTrees(GLAutoDrawable drawable) {
    	//draw trees
    	int size = this.trees().size();//get how many trees there are
    	double x, z, y;

    	myGL.glBindTexture(GL.GL_TEXTURE_2D, texID[12]); //bind the texture
    	for (int count = 0; count < size; count++) { //loop for each tree
        	x = this.trees().get(count).getPosition()[0]; //get the coordinates of the trees
    		z = this.trees().get(count).getPosition()[2];
        	y = this.altitude(x, z);
    		double baseHeight1 = y; //where treetrunks starts growing
    		double baseHeight2 = y + 2; //where treetrunks end
    		
	        myGL.glBegin(GL2.GL_TRIANGLE_FAN);{ //draw the top of the tree
    			myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, GREEN, 0);
             	myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, BLACK, 0);	
    			myGL.glNormal3d(0,-1,0);
    			myGL.glVertex3d(x,baseHeight2 + 1,z);
    			double angleStep = 2*Math.PI/Tree.SLICES;
    			for (int i = 0; i <= Tree.SLICES ; i++){
    				double a0 = i * angleStep;
    				double x0 = x + Math.cos(a0) / 2;
    				double z0 = z + Math.sin(a0) / 2;
    				myGL.glTexCoord2d(x0,z0);
    				myGL.glVertex3d(x0,baseHeight2 + 1,z0);
    			}             
    		} myGL.glEnd();
    		
    		myGL.glBegin(GL2.GL_TRIANGLE_FAN);{ //draw the bottom of the leave
    			myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, GREEN, 0);
             	myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, BLACK, 0);	
    			myGL.glNormal3d(0,-1,0);
    			myGL.glVertex3d(x,baseHeight2,z);
    			double angleStep = 2*Math.PI/Tree.SLICES;
    			for (int i = 0; i <= Tree.SLICES ; i++){
    				double a0 = i * angleStep;
    				double x0 = x + Math.cos(a0) / 2;
    				double z0 = z + Math.sin(a0) / 2;
    				myGL.glTexCoord2d(x0,z0);
    				myGL.glVertex3d(x0,baseHeight2,z0);
    			}             
    		} myGL.glEnd();
	        
	        myGL.glBegin(GL2.GL_QUADS);{ //draw the leaves
             	myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, GREEN, 0);
             	myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, BLACK, 0);	
    			double angleStep = 2*Math.PI/Tree.SLICES;
    			for (int i = 0; i < Tree.SLICES; i++){
    				double a0 = i * angleStep;
    				double a1 = (i+1) * angleStep;
    				double cx0 = x + (Math.cos(a0) / 2);
    				double cz0 = z + (Math.sin(a0) / 2);
    				double cx1 = x + (Math.cos(a1) / 2);
    				double cz1 = z + (Math.sin(a1) / 2);
    				myGL.glNormal3d(-(baseHeight2 - baseHeight1) * (cz1 - cz0),0 ,(baseHeight2 - baseHeight1) * (cx1 - cx0)); 
    				myGL.glTexCoord2d(0,0);
    				myGL.glVertex3d(cx1, baseHeight2, cz1);
    				myGL.glTexCoord2d(0,1);
	                myGL.glVertex3d(cx1, baseHeight2 + 1, cz1); 
	                myGL.glTexCoord2d(1/Tree.SLICES,1);
	                myGL.glVertex3d(cx0, baseHeight2 + 1, cz0);
	                myGL.glTexCoord2d(1/Tree.SLICES,0);
	                myGL.glVertex3d(cx0, baseHeight2, cz0);
	            }
	        } myGL.glEnd();
	        
	        myGL.glBegin(GL2.GL_QUADS);{ //draw the dree trunks
	        	myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, BROWN, 0);
	        	myGL.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, BLACK, 0);	
    			double angleStep = 2*Math.PI/Tree.SLICES;
    			for (int i = 0; i < Tree.SLICES; i++){
    				double a0 = i * angleStep;
    				double a1 = (i+1) * angleStep;
    				double cx0 = x + (Math.cos(a0) / 5);
    				double cz0 = z + (Math.sin(a0) / 5);
    				double cx1 = x + (Math.cos(a1) / 5);
    				double cz1 = z + (Math.sin(a1) / 5); 
    				myGL.glNormal3d(-(baseHeight2 - baseHeight1) * (cz1 - cz0),0 ,(baseHeight2 - baseHeight1) * (cx1 - cx0)); 
    				myGL.glTexCoord2d(0,0);
      				myGL.glVertex3d(cx1, baseHeight1, cz1);
      				myGL.glTexCoord2d(0,1);
	                myGL.glVertex3d(cx1, baseHeight2, cz1);
	                myGL.glTexCoord2d(1/Tree.SLICES,1);
	                myGL.glVertex3d(cx0, baseHeight2, cz0);
	                myGL.glTexCoord2d(1/Tree.SLICES,0);
	                myGL.glVertex3d(cx0, baseHeight1, cz0);
	            }
	        } 
	        myGL.glEnd();
	   	}
    }
    
    public void drawRoads(GLAutoDrawable drawable) {
    	GL2 gl = myGL;
    	int size = this.roads().size();
    	myGL.glBindTexture(GL.GL_TEXTURE_2D, texID[11]);
    	for (int count = 0; count < size; count++) {
        	Road road = this.roads().get(count);
        	
        	double[] p1 = road.point(0);
        	double[] normal1 = road.normal(0);
        	double fixedAltitude = this.altitude(p1[0], p1[1]) + ROAD_ALTITUDE_OFFSET; //added manual offset as the polygon offset works poorly

        	gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL); 
     		gl.glPolygonOffset((float)0.1,(float)0.1); 
         	gl.glBegin(GL2.GL_QUAD_STRIP);
        	double width = (road.width()/2);
        	double roadSections = (double) Road.NUM_SECTIONS;
        	
        	if (roadMode == EDGE_ALTITUDE) { //if the roadmode is edge altitude (road tilts along slope sideways)
        		gl.glNormal3d(0,-1,0);
            	gl.glTexCoord2d(1, 1);
        		gl.glVertex3d(p1[0] - (normal1[0]*width),
        				this.altitude((p1[0] - (normal1[0]*width)), (p1[1] + (normal1[1]*width))) + ROAD_ALTITUDE_OFFSET,
        				p1[1] + (normal1[1]*width));
        		gl.glTexCoord2d(0, 1);
        		gl.glVertex3d(p1[0] + (normal1[0]*width),
    					this.altitude((p1[0] + (normal1[0]*width)), (p1[1] - (normal1[1]*width))) + ROAD_ALTITUDE_OFFSET,
    					p1[1] - (normal1[1]*width));
        	} else if (roadMode == SPINE_ALTITUDE) { //if the roadmode is spine_altitude (road follows terrain but doesnt tilt sideways)
        		gl.glNormal3d(0,-1,0);
            	gl.glTexCoord2d(1, 1);
        		gl.glVertex3d(p1[0] - (normal1[0]*width), this.altitude(p1[0], p1[1]) + ROAD_ALTITUDE_OFFSET, p1[1] + (normal1[1]*width));
        		gl.glTexCoord2d(0, 1);
        		gl.glVertex3d(p1[0] + (normal1[0]*width), this.altitude(p1[0], p1[1]) + ROAD_ALTITUDE_OFFSET, p1[1] - (normal1[1]*width));
        	} else { //if the roadmode is fixed altitude (the altitude for the whole road is fixed at the starting point, good for flat road)
        		gl.glNormal3d(0,-1,0);
            	gl.glTexCoord2d(1, 1);
        		gl.glVertex3d(p1[0] - (normal1[0]*width), fixedAltitude, p1[1] + (normal1[1]*width));
        		gl.glTexCoord2d(0, 1);
        		gl.glVertex3d(p1[0] + (normal1[0]*width), fixedAltitude, p1[1] - (normal1[1]*width));	
        	}
    		
    		//crawl along the road, drawing each point
    		for (int i = 1; (double)i/roadSections  < road.size(); i++) {
        		double t = (double)i/roadSections;
        		double[] p2 = road.point(t);
        		double[] normal2 = road.normal(t);   
        		gl.glNormal3d(0,-1,0);
        		gl.glTexCoord2d(1, 1 - i/roadSections);
        		//DERP
        		if (roadMode == EDGE_ALTITUDE) {
        			gl.glVertex3d(p2[0] - (normal2[0]*width),
        					this.altitude((p2[0] - (normal2[0]*width)), (p2[1] + (normal2[1]*width))) + ROAD_ALTITUDE_OFFSET,
        					p2[1] + (normal2[1]*width));
        			gl.glTexCoord2d(0, 1 - i/roadSections);
        			gl.glVertex3d(p2[0] + (normal2[0]*width),
        					this.altitude((p2[0] + (normal2[0]*width)), (p2[1] - (normal2[1]*width))) + ROAD_ALTITUDE_OFFSET,
        					p2[1] - (normal2[1]*width));
        		} else if (roadMode == SPINE_ALTITUDE) {
        			gl.glVertex3d(p2[0] - (normal2[0]*width), this.altitude(p2[0], p2[1]) + ROAD_ALTITUDE_OFFSET, p2[1] + (normal2[1]*width));
        			gl.glTexCoord2d(0, 1 - i/roadSections);
        			gl.glVertex3d(p2[0] + (normal2[0]*width), this.altitude(p2[0], p2[1]) + ROAD_ALTITUDE_OFFSET, p2[1] - (normal2[1]*width));
            		
        		} else {
        			gl.glVertex3d(p2[0] - (normal2[0]*width), fixedAltitude, p2[1] + (normal2[1]*width));
        			gl.glTexCoord2d(0, 1 - i/roadSections);
        			gl.glVertex3d(p2[0] + (normal2[0]*width), fixedAltitude, p2[1] - (normal2[1]*width));
                	
        		}        		
        		p1 = p2;
        		normal1 = normal2;
        	}

    		p1[0] = road.pointByIndex(road.numPoints()-2);
        	p1[1] = road.pointByIndex(road.numPoints()-1);
        	gl.glNormal3d(0,-1,0);
    		gl.glTexCoord2d(1, 0);
    		//DERP
    		if (roadMode == EDGE_ALTITUDE) {
    			gl.glVertex3d(p1[0] - (normal1[0]*width),
    					this.altitude((p1[0] - (normal1[0]*width)), (p1[1] + (normal1[1]*width))) + ROAD_ALTITUDE_OFFSET,
    					p1[1] + (normal1[1]*width));
    			gl.glTexCoord2d(0, 0);
    			gl.glVertex3d(p1[0] - (normal1[0]*width),
    					this.altitude((p1[0] - (normal1[0]*width)), (p1[1] - (normal1[1]*width))) + ROAD_ALTITUDE_OFFSET,
    					p1[1] - (normal1[1]*width));
    		} else if (roadMode == SPINE_ALTITUDE) {
    			gl.glVertex3d(p1[0] - (normal1[0]*width), this.altitude(p1[0], p1[1]) + ROAD_ALTITUDE_OFFSET, p1[1] + (normal1[1]*width));
    			gl.glTexCoord2d(0, 0);
    			gl.glVertex3d(p1[0] - (normal1[0]*width), this.altitude(p1[0], p1[1]) + ROAD_ALTITUDE_OFFSET, p1[1] - (normal1[1]*width));
    	    	
    		} else {
    			gl.glVertex3d(p1[0] - (normal1[0]*width), fixedAltitude, p1[1] + (normal1[1]*width));
    			gl.glTexCoord2d(0, 0);
    			gl.glVertex3d(p1[0] - (normal1[0]*width), fixedAltitude, p1[1] - (normal1[1]*width));
        		
    		}
    		
    		gl.glEnd();
    		gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
    	}
    }
    

    public void loadTexture(GL2 gl, String str, int count) {
		TextureData data = null;
		try {
			File file = new File(str); //load the file
			BufferedImage img = ImageIO.read(file); //read the file
			ImageUtil.flipImageVertically(img); //flip it
			data = AWTTextureIO.newTextureData(GLProfile.getDefault(), img, false);
			System.out.println("heyy file  found");
		} catch (IOException e) {
			System.out.println("heyy file not found"); //if file is not file
			e.printStackTrace();
		}
		gl.glGenTextures(nTex, textures, 0);
 		gl.glBindTexture(GL.GL_TEXTURE_2D, textures[0]);
 		texID[count] = textures[0]; //load the texture id into 
 		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0,	
				data.getInternalFormat(),
				data.getWidth(),
				data.getHeight(),
				0,
				data.getPixelFormat(),
				data.getPixelType(),
				data.getBuffer());
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR); //linear mapping
		gl.glGenerateMipmap(GL2.GL_TEXTURE_2D); 	//generate the mipmap
        gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE); //make it work with light
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_COLOR_CONTROL, GL2.GL_SEPARATE_SPECULAR_COLOR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT); //set mode to repeat
    }

    
}
