
public class RunChordDHT {

  public static void main(String args[]) {
    System.out.println("Creating Chord network...");

    try {
      IChordNodeServer n1 = new ChordNode("lancashire");
      IChordNodeServer n2 = new ChordNode("yorkshire", "lancashire");
      IChordNodeServer n3 = new ChordNode("cheshire", "lancashire");
      IChordNodeServer n4 = new ChordNode("lincolnshire", "lancashire");
      IChordNodeServer n5 = new ChordNode("derbyshire", "lancashire");
      IChordNodeServer n6 = new ChordNode("nottinghamshire", "lancashire");
      IChordNodeServer n7 = new ChordNode("cumbria", "lancashire");
      IChordNodeServer n8 = new ChordNode("durham", "lancashire");
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("...Network ready.");
  }

}
