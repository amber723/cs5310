import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;

import scene.SceneElement;
import util.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import sun.security.provider.certpath.Vertex;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class View {
    private int WINDOW_WIDTH, WINDOW_HEIGHT;
    private Stack<Matrix4f> modelview;
    private Matrix4f proj,trackballTransform;
    private List<SceneElement> elements;
    private ShaderLocationsVault shaderLocations;
    private ShaderProgram program;

    private int pos_x, pos_y;
    private float delta_x = 0, delta_y = 90;
    private float r = 250;


    public View() {
        proj = new Matrix4f();
        modelview = new Stack<Matrix4f>();
        trackballTransform = new Matrix4f();

        elements = new ArrayList<SceneElement>();
        shaderLocations = null;
        WINDOW_WIDTH = WINDOW_HEIGHT = 0;
    }

    public void init(GLAutoDrawable gla) throws Exception {
        GL3 gl = gla.getGL().getGL3();
        program = new ShaderProgram();
        program.createProgram(gl, "shaders/default.vert",
                "shaders/default.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        Map<String,String> shaderVariablesToVertexAttributes;

        shaderVariablesToVertexAttributes = new HashMap<String,String> ();
        shaderVariablesToVertexAttributes.put("vPosition","position");

        initObjects(gl,shaderVariablesToVertexAttributes);

        program.disable(gl);
    }

    private void initObjects(GL3 gl,Map<String,String> shaderVarsToVertexAttribs)
            throws FileNotFoundException {
        util.PolygonMesh<VertexAttrib> mesh;
        util.ObjectInstance o;
        Matrix4f transform;
        util.Material mat;
        SceneElement element;

        InputStream in;

        VertexAttribProducer producer = new VertexAttribProducer();

        mat =  new util.Material();

        //first object
        in = new FileInputStream("models/top.obj");
        mesh = util.ObjImporter.importFile(producer,in,true);
        o = new util.ObjectInstance(gl,program,shaderLocations,
                shaderVarsToVertexAttribs,
                mesh,new String("top"));
        mat.setAmbient(1,0,0); //only this one is used currently to determine color
        mat.setDiffuse(1,0,0);
        mat.setSpecular(1,0,0);
        transform = new Matrix4f().translate(50,-18,0)
                .rotate((float) Math.toRadians(45), 1, 0, 0)
                .scale(20,20,20);
        element = new SceneElement(o,transform,mat);
        elements.add(element);

        //second object
        in = new FileInputStream("models/cylinder.obj");
        mesh = util.ObjImporter.importFile(producer,in,true);
        o = new util.ObjectInstance(gl,program,shaderLocations,
                shaderVarsToVertexAttribs,
                mesh,new String("cylinder"));
        mat.setAmbient(0,1,1);
        mat.setDiffuse(0,1,1);
        mat.setSpecular(0,1,1);

        transform = new Matrix4f().mul(new Matrix4f()
                .translate(-60, -18,0)
                .rotate((float)Math.toRadians(90), 0 , 0 , 1)
                .scale(10,10,10));
        element = new SceneElement(o,transform,mat);
        elements.add(element);

        //third object
        in = new FileInputStream("models/box.obj");
        mesh = util.ObjImporter.importFile(producer,in,true);
        o = new util.ObjectInstance(gl,program,shaderLocations,
                shaderVarsToVertexAttribs,mesh,new String
                ("box"));

        mat.setAmbient(1,1,1);
        mat.setDiffuse(1,1,1);
        mat.setSpecular(1,1,1);
        transform = new Matrix4f().mul(new Matrix4f().translate(0,-75,0))
                .mul(new Matrix4f().scale(300,1,300));
        element = new SceneElement(o,transform,mat);
        elements.add(element);
    }

    public void draw(GLAutoDrawable gla) {

        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);

        //set the background color to be black
        gl.glClearColor(0, 0, 0, 1);
        program.enable(gl);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(gl.GL_DEPTH_TEST);

        FloatBuffer fb = Buffers.newDirectFloatBuffer(16);
        FloatBuffer mfb = Buffers.newDirectFloatBuffer(4);

        gl.glUniformMatrix4fv(shaderLocations.getLocation("projection"),
                1,false, proj.get(fb));

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        modelview.push(new Matrix4f());

        modelview.peek().mul(new Matrix4f().lookAt(
                new Vector3f(0,0, r),
                new Vector3f(0,0,0),
                new Vector3f(0,1, 0)));

        modelview.peek().mul(trackballTransform);

        for (SceneElement el:elements) {
            modelview.push(new Matrix4f(modelview.peek())); //save the current

            Matrix4f transform = el.getTransform();
            modelview.peek().mul(transform);

            //The total transformation is whatever was passed to it, with its own transformation
            gl.glUniformMatrix4fv(shaderLocations.getLocation("modelview"),
                    1,false, modelview.peek().get(fb));

            //set the color for all vertices to be drawn for this object
            gl.glUniform4fv(shaderLocations.getLocation("vColor"),1,el
                    .getMaterial().getAmbient().get(fb));

            el.getObject().draw(gla);
            modelview.pop();
        }

        //=====================================================================
        //create 3 walls
        //=====================================================================

        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek()
                .translate(150,-25, 0)
                .rotate((float)Math.toRadians(90), 0,0,1)
                .scale(100,1,300);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));

        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1,
                elements.get(2).getMaterial().getAmbient().get(fb));

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(2).getObject().draw(gla);
        modelview.pop();

        //=====================================================================

        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek()
                .translate(-150,-25, 0)
                .rotate((float)Math.toRadians(90), 0,0,1)
                .scale(100,1,300);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));

        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1,
                elements.get(2).getMaterial().getAmbient().get(fb));

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(2).getObject().draw(gla);
        modelview.pop();

        //=====================================================================

        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek()
                .translate(0,-25, -150)
                .rotate((float)Math.toRadians(90), 1,0,0)
                .scale(300,1,100);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));

        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1,
                elements.get(2).getMaterial().getAmbient().get(fb));

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(2).getObject().draw(gla);
        modelview.pop();

        //=====================================================================
        //Finish 3 walls
        //=====================================================================

        FloatBuffer color = FloatBuffer.wrap(new float[]{1, 1, 0, 0});
        //=====================================================================
        //create the desk 4 legs
        //=====================================================================
        int[] pos = new int[]{1, -1, 1, 1, -1, 1, -1, -1};

        for (int i = 0; i < 4; i++) {
            modelview.push(new Matrix4f(modelview.peek()));
            modelview.peek().translate(50 * pos[i * 2],
                    -50, 50 * pos[i * 2 + 1])
                    .scale(10, 50, 10);

            gl.glUniformMatrix4fv(
                    shaderLocations.getLocation("modelview"),
                    1, false, modelview.peek().get(fb16));

            gl.glUniform4fv(shaderLocations.getLocation("vColor"),1, color);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

            elements.get(2).getObject().draw(gla);
            modelview.pop();
        }

        //create desk top======================================================
        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek().translate(0, -25, 0).scale(180,5,120);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));

        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1, color);

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(2).getObject().draw(gla);
        modelview.pop();

        //=====================================================================
        //create laptop
        // ====================================================================
        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek().translate(0, -20, 0).scale(60,4,45);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));

        color = FloatBuffer.wrap(new float[]{0, 0, 1, 0});
        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1, color);

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(2).getObject().draw(gla);
        modelview.pop();

        //============================================================================
        //============================================================================
        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek()
                .translate(0, 6, -25)
                .rotate(80,1,0,0)
                .scale(60,2,45);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));

        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1, color);

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(2).getObject().draw(gla);
        modelview.pop();

        //=====================================================================
        // Finish creating laptop
        //=====================================================================

        //=====================================================================
        //create dumbbell
        // ====================================================================
        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek().translate(-80,-18, 0)
                .rotate((float)Math.toRadians(90), 0,0,1)
                .scale(10,10,10);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));
        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1,
                elements.get(1).getMaterial().getAmbient().get(fb));

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(1).getObject().draw(gla);
        modelview.pop();

        //============================================================================
        //============================================================================
        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek()
                .translate(-70, -18, 0)
                .rotate((float)Math.toRadians(90), 0,0,1)
                .scale(5,30,5);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelview.peek().get(fb16));
        gl.glUniform4fv(shaderLocations.getLocation("vColor"),1,
                elements.get(1).getMaterial().getAmbient().get(fb));

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        elements.get(1).getObject().draw(gla);
        modelview.pop();

        //=====================================================================
        // Finish creating dumbbell
        //=====================================================================

        modelview.pop();
        gl.glFlush();

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_FILL); //BACK TO FILL
        
        program.disable(gl);
    }

    public void mousePressed(int x,int y) {
        pos_x = x;
        pos_y = y;
    }

    public void mouseDragged(int x,int y) {
        delta_x = (x - pos_x) * (float)0.01;
        delta_y = (y - pos_y) * (float)0.01;

        trackballTransform = new Matrix4f()
                .rotate(delta_x, 0,1,0)
                .rotate(delta_y, 1, 0,0)
                .mul(trackballTransform);

        pos_x = x;
        pos_y = y;
    }

    public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        proj = new Matrix4f().perspective((float)Math.toRadians(120.0f),
                (float)width/height,0.1f,10000.0f);
    }

    public void dispose(GLAutoDrawable gla) {

        for (SceneElement el: elements) {
            el.getObject().cleanup(gla);
        }
    }
}
