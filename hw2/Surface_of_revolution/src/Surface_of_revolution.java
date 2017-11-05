import util.IVertexData;
import util.ObjExporter;
import util.PolygonMesh;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class Surface_of_revolution {
  public static void main(String[] args) throws FileNotFoundException {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.


    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });

  }

  private static void createAndShowGUI() {
    JFrame frame = new JOGLFrame("Surface of Revolution");
    frame.setVisible(true);
  }
}
