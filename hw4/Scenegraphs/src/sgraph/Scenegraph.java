package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import util.IVertexData;
import util.PolygonMesh;

import java.util.*;

/**
 * A specific implementation of this scene graph. This implementation is still independent
 * of the rendering technology (i.e. OpenGL)
 * @author Amit Shesh
 */
public class Scenegraph<VertexType extends IVertexData> implements IScenegraph<VertexType> {
    /**
     * The root of the scene graph tree
     */
    protected INode root;
    /**
     * A map to store the (name,mesh) pairs. A map is chosen for efficient search
     */
    protected Map<String,util.PolygonMesh<VertexType>> meshes;
    /**
     * A map to store the (name,node) pairs. A map is chosen for efficient search
     */
    protected Map<String,INode> nodes;
    protected Map<String,String> textures;
    /**
     * The associated renderer for this scene graph. This must be set before attempting to
     * render the scene graph
     */
    protected IScenegraphRenderer renderer;

    public Scenegraph() {
        root = null;
        meshes = new HashMap<String,util.PolygonMesh<VertexType>>();
        nodes = new HashMap<String,INode>();
        textures = new HashMap<String,String>();
    }

    public void dispose()
    {
        renderer.dispose();
    }

    /**
     * Sets the renderer, and then adds all the meshes to the renderer.
     * This function must be called when the scene graph is complete, otherwise not all of its
     * meshes will be known to the renderer
     * @param renderer The {@link IScenegraphRenderer} object that will act as its renderer
     * @throws Exception
     */
    @Override
    public void setRenderer(IScenegraphRenderer renderer) throws Exception {
        this.renderer = renderer;

        //now add all the meshes
        for (String meshName:meshes.keySet())
            this.renderer.addMesh(meshName,meshes.get(meshName));
    }

    /**
     * Set the root of the scenegraph, and then pass a reference to this scene graph object
     * to all its node. This will enable any node to call functions of its associated scene graph
     * @param root
     */
    @Override
    public void makeScenegraph(INode root) {
        this.root = root;
        this.root.setScenegraph(this);
    }

    /**
     * Draw this scene graph. It delegates this operation to the renderer
     * @param modelView
     */
    @Override
    public void draw(Stack<Matrix4f> modelView, int[] lights) {
        if ((root!=null) && (renderer!=null))
            renderer.draw(root, modelView, lights);
    }

    @Override
    public void addPolygonMesh(String name, util.PolygonMesh<VertexType> mesh) {
        meshes.put(name,mesh);
    }

    @Override
    public void animate(float time, int move_x, int motion) {
        INode body = root.getNode("walk");
        if (body != null)
            body.setAnimationTransform(new Matrix4f().translate(move_x, 0, 0));

        for (int i = 1; i <=6; i++){
            INode roll = root.getNode("roll"+i);
            if (roll != null)
                roll.setAnimationTransform(new Matrix4f().rotate((float) (-time * 0.1 * motion), 0, 1,0));
        }

        //for (int i = 1; i <= 4; i++) {
        //    INode arm = root.getNode("rotate_arm"+i);
//
        //    if (arm != null)
        //        arm.setAnimationTransform(new Matrix4f().
        //            rotate((float) (-time * 0.01) % (float) 2, 0, 0, 1));
        //}
        INode arm = root.getNode("rotate_arm1");
        int ARM_ANGLE = 90;

        if (arm != null)
            arm.setAnimationTransform(new Matrix4f().
            //rotate((float) (-time * .01) % (float) 2, 0, 0, 1));
                rotate((float) Math.toRadians(ARM_ANGLE * Math.sin(time * .01f)) -
                (float)Math.toRadians(70), 0, 0, 1));

        INode hand = root.getNode("rotate_arm2");
        if (hand != null) {
            hand.setAnimationTransform(new Matrix4f()
                .rotate((float) Math.toRadians(ARM_ANGLE * Math.sin(time * .01f)) -
                    (float)Math.toRadians(70), 0, 0, 1)
                .translate((-9.0f * (float) Math.cos(ARM_ANGLE)) ,
                    (-9.0f * (float) Math.sin(ARM_ANGLE)) - 100, -100));
        }

    }

    @Override
    public void addNode(String name, INode node) {
        nodes.put(name,node);
    }

    @Override
    public INode getRoot() {
        return root;
    }

    @Override
    public Map<String, PolygonMesh<VertexType>> getPolygonMeshes() {
       Map<String,util.PolygonMesh<VertexType>> meshes = new HashMap<String,PolygonMesh<VertexType>>(this.meshes);
       return meshes;
    }

    @Override
    public Map<String, INode> getNodes() {
        Map<String,INode> nodes = new TreeMap<String,INode>();
        nodes.putAll(this.nodes);
        return nodes;
    }

    @Override
    public void addTexture(String name, String path) {
        textures.put(name,path);
    }
}
