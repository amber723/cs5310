package scene;

import org.joml.Matrix4f;

import util.Material;
import util.ObjectInstance;

/**
 * This class represents a single element of a scene. An element is
 * characterized by an object instance, its material, and transformation
 */

public class SceneElement {
  private final ObjectInstance object;
  private final Matrix4f transform;
  private final Material material;

  public SceneElement(ObjectInstance obj,Matrix4f transform, Material
          material) {
    this.object = obj;
    this.transform = transform;
    this.material = new Material(material);
  }

  public ObjectInstance getObject() {
    return object;
  }

  public Matrix4f getTransform() {
    return transform;
  }

  public Material getMaterial() {
    return material;
  }
}
