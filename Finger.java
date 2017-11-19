import java.util.Vector;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;

public class Finger implements Serializable {
  public int key;
  public IChordNodeServer node;

  public Finger() {}
  public Finger(int key, ChordNode node) {
    this.key = key;
    this.node = node;
  }

  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Finger finger = (Finger) o;

      if (key != finger.getKey()) return false;
      return node != null ? node.equals(finger.getNode()) : finger.getNode() == null;
  }

  @Override
  public int hashCode() {
      int result = key;
      result = 31 * result + (node != null ? node.hashCode() : 0);
      return result;
  }

  public int getKey() { return this.key; }
  public void setKey(int key) { this.key = key; }

  public IChordNodeServer getNode() { return this.node; }
  public void setNode(IChordNodeServer node) { this.node = node; }
}
