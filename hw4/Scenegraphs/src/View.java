import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import org.joml.*;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class View {
    private int WINDOW_WIDTH, WINDOW_HEIGHT;
    private Stack<Matrix4f> modelView;
    private Matrix4f projection, trackballTransform, cameraTransform;
    private float trackballRadius;
    private Vector2f mousePos;

    private util.ShaderProgram program;
    private util.ShaderLocationsVault shaderLocations;
    private int projectionLocation;
    private sgraph.IScenegraph<VertexAttrib> scenegraph;
    private Vector3f eye, look_at, up;

    private int time =  0;
    private int move_x = 0;
    private int x_max = 120, x_min = -80;
    private int motion = 1;

    public int[] lights = new int[10];

    AWTGLReadBufferUtil screenCaptureUtil;

    public View() {
        Arrays.fill(lights,1);
        projection = new Matrix4f();
        modelView = new Stack<>();
        trackballRadius = 300;
        trackballTransform = new Matrix4f();
        scenegraph = null;

        cameraTransform = new Matrix4f();
        eye = new Vector3f(0, 50, 200);
        look_at = new Vector3f(0, 50, 0);
        up = new Vector3f(0, 1, 0);
    }

    public void initScenegraph(GLAutoDrawable gla, InputStream in) throws Exception {
        GL3 gl = gla.getGL().getGL3();

        if (scenegraph != null)
            scenegraph.dispose();

        program.enable(gl);

        scenegraph = sgraph.SceneXMLReader.importScenegraph(in, new VertexAttribProducer());

        sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
        renderer.setContext(gla);
        Map<String, String> shaderVarsToVertexAttribs = new HashMap<String, String>();
        shaderVarsToVertexAttribs.put("vPosition", "position");
        shaderVarsToVertexAttribs.put("vNormal", "normal");
        shaderVarsToVertexAttribs.put("vTexCoord", "texcoord");
        renderer.initShaderProgram(program, shaderVarsToVertexAttribs);
        scenegraph.setRenderer(renderer);
        program.disable(gl);
    }

    public void init(GLAutoDrawable gla) throws Exception {
        GL3 gl = gla.getGL().getGL3();
        program = new util.ShaderProgram();
        program.createProgram(gl, "shaders/gouraud-multiple.vert",
                "shaders/gouraud-multiple.frag");

        shaderLocations = program.getAllShaderVariables(gl);
        projectionLocation = shaderLocations.getLocation("projection");
    }

    public void draw(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();

        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(gl.GL_DEPTH_TEST);

        program.enable(gl);

        while (!modelView.empty())
            modelView.pop();

        modelView.push(new Matrix4f());
        modelView.peek().mul(cameraTransform).lookAt(eye, look_at, up)
                .mul(trackballTransform);

        //Supply the shader with all the matrices it expects.
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
        gl.glUniformMatrix4fv(projectionLocation, 1, false, projection.get(fb16));

//        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES


        time = (time + 1) % (Integer.MAX_VALUE);
        if (move_x >= x_max || move_x <= x_min)
            motion *= -1;
        move_x += motion;

        scenegraph.animate(time, move_x, motion);
        scenegraph.draw(modelView, lights);

        gl.glFlush();
        program.disable(gl);
    }

    public void captureFrame(String filename,GLAutoDrawable gla) throws
            FileNotFoundException,IOException {
        if (screenCaptureUtil==null) {
            screenCaptureUtil = new AWTGLReadBufferUtil(gla.getGLProfile(),false);
        }

        File f = new File(filename);
        GL3 gl = gla.getGL().getGL3();

        BufferedImage image = screenCaptureUtil.readPixelsToBufferedImage(gl,true);
        OutputStream file = null;
        file = new FileOutputStream(filename);

        ImageIO.write(image,"png",file);

    }

    public void mousePressed(int x, int y) {
        mousePos = new Vector2f(x, y);
    }

    public void mouseReleased(int x, int y){ }

    public void mouseDragged(int x, int y) {
        Vector2f newM = new Vector2f(x, y);

        Vector2f delta = new Vector2f(newM.x - mousePos.x, newM.y - mousePos.y);
        mousePos = new Vector2f(newM);

        trackballTransform = new Matrix4f().rotate(delta.x / trackballRadius, 0, 1, 0)
                .rotate(delta.y / trackballRadius, 1, 0, 0)
                .mul(trackballTransform);
    }

    public void move_camera(float dir){
        cameraTransform = new Matrix4f()
                .translate(0, 0, dir)
                .mul(cameraTransform);
    }

    public void node_no(float dir){
        cameraTransform = new Matrix4f()
                .rotate((float) Math.toRadians(dir * 10), 0, 1, 0)
                .mul(cameraTransform);
    }

    public void node_yes(float dir){
        cameraTransform = new Matrix4f()
                .rotate((float) Math.toRadians(dir * 30), 1, 0, 0)
                .mul(cameraTransform);

    }

    public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        projection = new Matrix4f().perspective((float) Math.toRadians(120.0f),
                (float) width / height, 0.1f, 10000.0f);
        // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

    }

    public void dispose(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();
    }
}
