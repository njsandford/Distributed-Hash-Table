import java.util.Vector;
import java.rmi.Remote;
import java.rmi.RemoteException;
//mport java.rmi.NotBoundException;

public interface IChordNodeServer extends Remote {

  public void join(IChordNodeServer atNode) throws RemoteException;
  public void leave() throws RemoteException;

  public void put(String key, byte[] value) throws RemoteException;
  public byte[] get(String key) throws RemoteException;
  public void remove(String key) throws RemoteException;

  public String getKeyString() throws RemoteException;
  public void setKeyString(String key) throws RemoteException;
  public int getKey() throws RemoteException;
  public void setKey(int key) throws RemoteException;

  public IChordNodeServer getPredecessor() throws RemoteException;
  public void setPredecessor(IChordNodeServer predecessor) throws RemoteException;
  public IChordNodeServer getSuccessor() throws RemoteException;
  public void setSuccessor(IChordNodeServer predecessor) throws RemoteException;
  public int getSuccessorKey() throws RemoteException;
  public int getPredecessorKey() throws RemoteException;
  public void setSuccessorKey(int key) throws RemoteException;
  public void setPredecessorKey(int key) throws RemoteException;
  public Finger getFinger(int index) throws RemoteException;
  public Finger[] getFingers() throws RemoteException;
  public Vector<Store> getDataStore() throws RemoteException;
  public void addData(String key, byte[] value) throws RemoteException;
  public void removeData(String key) throws RemoteException;

  public IChordNodeServer findSuccessor(int key) throws RemoteException;
  public void notifyNode(IChordNodeServer potentialPredecessor) throws RemoteException;
  public IChordNodeServer closestPrecedingNode(int key) throws RemoteException;
  public void updateFingerTable(IChordNodeServer s, int i) throws RemoteException;
  public void fixFingers() throws RemoteException;
  public void checkDataMoveDown() throws RemoteException;
  public void checkPredecessor() throws RemoteException;
  public void stabilise() throws RemoteException;

  public void printTopography() throws RemoteException;
  public void cmdlnInterface() throws RemoteException;

  public void submitAsyncTask(String fileName, byte[] fileData, Task task) throws RemoteException;
}
