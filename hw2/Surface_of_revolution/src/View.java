import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;

import util.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;

public class View {
    private int WINDOW_WIDTH,WINDOW_HEIGHT;
    private Matrix4f proj;
    Stack<Matrix4f> modelView;
    private util.ObjectInstance meshObject;
    private util.Material material;

    private util.ShaderProgram program;
    int angleOfRotation;
    private ShaderLocationsVault shaderLocations;

    private int NUM_SLICE = 36;

    public View() {
        proj = new Matrix4f();
        proj.identity();

        modelView = new Stack<Matrix4f>();

        angleOfRotation = 0;
    }

    private void initObjects(GL3 gl) throws FileNotFoundException {
        util.PolygonMesh tmesh;

        List<IVertexData> vertexData = new ArrayList<IVertexData>();
        VertexAttribProducer producer = new VertexAttribProducer();

        Matrix4f m = new Matrix4f().rotate(
                (float) Math.toRadians(360/NUM_SLICE), 0,1,0);

        List<Integer> indices = new ArrayList<Integer>();

//=============================================================================
        List<Vector4f> positions = new ArrayList<Vector4f>();
        positions.add(new Vector4f(0.0f, 0.0f, 0, 1.0f));
        positions.add(new Vector4f(5.0f, 0.0f, 0, 1.0f));
        positions.add(new Vector4f(5.0f, 50.0f, 0, 1.0f));
        positions.add(new Vector4f(0.0f, 50.0f, 0, 1.0f));

        for(int i = 0; i < NUM_SLICE; i++){
            for(Vector4f pos : positions){
                IVertexData v = producer.produce();
                v.setData("position", new float[]{
                        pos.x, pos.y, pos.z, pos.w});
                vertexData.add(v);
                m.transform(pos);
            }
        }

        for(int i = 0; i < vertexData.size(); i += 4){

            indices.add(3);
            indices.add(i+2);
            indices.add(i+6);

            indices.add(i+1);
            indices.add(i+2);
            indices.add(i+6);

            indices.add(i+1);
            indices.add(i+5);
            indices.add(i+6);

            indices.add(0);
            indices.add(i+1);
            indices.add(i+5);
        }

        indices.add(1);
        indices.add(2);
        indices.add(vertexData.size() - 3);

        indices.add(vertexData.size() - 3);
        indices.add(vertexData.size() - 2);
        indices.add(2);

        int tmp = vertexData.size();

//=============================================================================
//=============================================================================

        List<Vector4f> base_pos = new ArrayList<Vector4f>();
        base_pos.add(new Vector4f(0.0f, -50.0f, 0, 1.0f));
        base_pos.add(new Vector4f(50.0f, 0.0f, 0, 1.0f));
        base_pos.add(new Vector4f(0.0f, 0.0f, 0, 1.0f));

        for(int i = 0; i < NUM_SLICE; i++){

            for(Vector4f pos : base_pos){

                IVertexData v = producer.produce();
                v.setData("position", new float[]{
                        pos.x, pos.y, pos.z, pos.w});
                vertexData.add(v);
                m.transform(pos);
            }
        }

        indices.add(tmp);
        indices.add(tmp+1);
        indices.add(tmp+4);

        for(int i = tmp ; i < vertexData.size(); i += 3){
            indices.add(tmp);
            indices.add(i+1);
            indices.add(i+4);

            indices.add(tmp + 2);
            indices.add(i+1);
            indices.add(i+4);
        }

        indices.add(tmp);
        indices.add(vertexData.size()-2);
        indices.add(tmp+1);

        indices.add(tmp+2);
        indices.add(vertexData.size()-2);
        indices.add(tmp+1);

//=============================================================================

        PolygonMesh<IVertexData> mesh = new PolygonMesh<IVertexData>();
        mesh.setVertexData(vertexData);
        mesh.setPrimitives(indices);
        mesh.setPrimitiveType(GL.GL_TRIANGLES);
        mesh.setPrimitiveSize(3);

        ObjExporter objexp = new ObjExporter();
        objexp.exportFile(mesh, new FileOutputStream("top.obj"));


        Map<String, String> shaderToVertexAttribute =
                new HashMap<String, String>();

        shaderToVertexAttribute.put("vPosition", "position");

        meshObject = new util.ObjectInstance(gl, program,
                shaderLocations, shaderToVertexAttribute,
                mesh,new String(""));


        util.Material mat =  new util.Material();

        mat.setAmbient(1,0,0);
        mat.setDiffuse(1,1,1);
        mat.setSpecular(1,1,1);

        material = mat;
    }

    public void init(GLAutoDrawable gla) throws Exception {
        GL3 gl = (GL3) gla.getGL().getGL3();

        program = new ShaderProgram();
        program.createProgram(gl,
                "shaders/default.vert",
                "shaders/default.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        initObjects(gl);
    }


    public void draw(GLAutoDrawable gla) {
        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

        angleOfRotation = (angleOfRotation+1)%360;

        //set the background color to be black
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);

        program.enable(gl);

        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("projection"),
                1, false, proj.get(fb16));

        gl.glUniform4fv(
                shaderLocations.getLocation("vColor"),
                1, material.getAmbient().get(fb4));

        modelView.push(new Matrix4f());

        modelView.push(new Matrix4f(modelView.peek()));
        modelView.peek().lookAt(
                new Vector3f(0,200,200),
                new Vector3f(0, 0,0),
                new Vector3f(0,1,0));

        modelView.push(new Matrix4f(modelView.peek()));
        modelView.peek().rotate((float)Math.toRadians(angleOfRotation),0,1,0);


        gl.glUniformMatrix4fv(
                shaderLocations.getLocation("modelview"),
                1, false, modelView.peek().get(fb16));

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_LINE); //OUTLINES

        meshObject.draw(gla);

        modelView.pop();

        gl.glFlush();
        program.disable(gl);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL3.GL_FILL); //BACK TO FILL
    }

    //this method is called from the JOGLFrame class, everytime the window resizes
    public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        proj = new Matrix4f().perspective((float)Math.toRadians(60.0f),
                (float) width/height,
                0.1f,
                10000.0f);

        // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

    }

    public void dispose(GLAutoDrawable gla) {
        meshObject.cleanup(gla);
    }
}
