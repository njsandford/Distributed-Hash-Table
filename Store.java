
import java.util.Vector;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;

public class Store implements Serializable {
  String key;
  byte[] value;

  public Store() {}
  public Store(String key, byte[] value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() { return this.key; }
  public void setKey(String key) { this.key = key; }

  public byte[] getValue() { return this.value; }
  public void setValue(byte[] data) { this.value = value; }
}
