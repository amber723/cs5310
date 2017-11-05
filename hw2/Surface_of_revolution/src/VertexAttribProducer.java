public class VertexAttribProducer implements util.VertexProducer<VertexAttrib> {
  @Override
  public VertexAttrib produce() {
    return new VertexAttrib();
  }
}
