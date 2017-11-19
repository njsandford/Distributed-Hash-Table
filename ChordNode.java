
import java.util.Vector;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.ConnectIOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import com.sun.jersey.api.representation.Form;

import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

import org.w3c.dom.*;


/**
 * ChordNode Class, build using the template provided in the week 3 practical of SCC401.
 * Source: http://barryfp.com/teaching/SCC401/rsrc/week3/ChordNode.java
 */
public class ChordNode extends UnicastRemoteObject implements Runnable, IChordNodeServer {

  private static final String PREFIX = "/var/lib/tomcat8/webapps/myapp/files/";
  static final int KEY_BITS = 8;
  public boolean maintain = true;

  IChordNodeServer successor;
  int successorKey;

  IChordNodeServer predecessor;
  int predecessorKey;

  int fingerTableLength;
  Finger finger[];
  int nextFingerFix;

  Vector<Store> dataStore = new Vector<Store>();

  private int myKey;
  private String myKeyString;

  /**
   * Constructor takes a string to be the key of the node.
   */
  public ChordNode(String myKeyString) throws RemoteException {
    super();
    myKey = hash(myKeyString);
    this.myKeyString = myKeyString;

    // Set successor as itself, as currently the only node in the network.
    successor = this;
    successorKey = myKey;

    // Initialise finger table with null values.
    finger = new Finger[KEY_BITS];
    for (int i = 0; i < KEY_BITS; i++) {
      finger[i] = new Finger();
    }
    fingerTableLength = KEY_BITS;

    // Add node to registry:
    Registry registry = LocateRegistry.getRegistry();
    registry.rebind(getKeyString(), this);//stub);
    System.out.println("ChordNodeServer ready");

    // Start up the periodic maintenance thread
    new Thread(this).start();

    // Join node to single node network using itself.
    this.join(this);
  }

  /**
   * Constructor that creates new ChordNode and joins it to the network using another node's key.
   */
  public ChordNode(String myKeyString, String joinNode) throws RemoteException, NotBoundException {
    this(myKeyString);

    // Lookup other node from the registry.
    Registry registry = LocateRegistry.getRegistry();
    IChordNodeServer node = (IChordNodeServer) registry.lookup(joinNode);

    System.out.println("Joining node to network...");
    // Join node to network using existing node in the network.
    this.join(node);
  }

  // -- API functions --

  /**
   * Find the node where the data should be stored, and call the relevant method on that node to add the data.
   */
  public void put(String key, byte[] value) throws RemoteException {
    // Find the node that should hold this key.
    IChordNodeServer sNode = findSuccessor(hash(key));
    System.out.println("Put node: " + sNode.getKeyString() + ", node key: " + sNode.getKey() + ", key: " + key + ", key hash: " + hash(key));
    // Call method on the node to add the key and value to that node's local data store.
    sNode.addData(key, value);
  }

  /**
   * Find the node that should hold this key, request the corresponding value from that node's local data store, and return it.
   */
  public byte[] get(String key) throws RemoteException {
    // Find the node that should hold this key.
    IChordNodeServer sNode = findSuccessor(hash(key));
    // Iterate through the items in the nodes local data store, and if the key is found, return the corresponding value.
    for (Store store : sNode.getDataStore()) {
      System.out.println("Store key: " + store.getKey() + ", key: " + key);
      if (store.getKey().equals(key)) {
        System.out.println("value: " + store.getValue());
        return store.getValue();
      }
    }
    // The key was not found in the data store, so return null.
    return null;
  }

  /**
   * Find the node where the data is stored, and call the relevant method on that node to remove the data.
   */
  public void remove(String key) throws RemoteException {
    // Find the node that should hold this key.
    IChordNodeServer sNode = findSuccessor(hash(key));
    System.out.print("BEFORE REMOVAL: " + sNode.getDataStore());
    // Remove the item from the local data store of that node.
    sNode.removeData(key);
    System.out.print("AFTER REMOVAL: " + sNode.getDataStore());
  }

  // -- state utilities --
  public int getKey() throws RemoteException { return this.myKey; }

  public void setKey(int key) throws RemoteException { this.myKey = key; }

  public String getKeyString() throws RemoteException { return this.myKeyString; }

  public void setKeyString(String keyString) throws RemoteException { this.myKeyString = keyString; }

  public IChordNodeServer getPredecessor() throws RemoteException { return this.predecessor; }

  public void setPredecessor(IChordNodeServer newNode) throws RemoteException { this.predecessor = newNode; }

  public IChordNodeServer getSuccessor() throws RemoteException { return this.successor; }

  public void setSuccessor(IChordNodeServer newNode) throws RemoteException { this.successor = newNode; }

  public int getSuccessorKey() throws RemoteException { return this.successorKey; }

  public int getPredecessorKey() throws RemoteException { return this.predecessorKey; }

  public void setSuccessorKey(int key) throws RemoteException { this.successorKey = key; }

  public void setPredecessorKey(int key) throws RemoteException { this.predecessorKey = key; }

  public Finger getFinger(int index) throws RemoteException { return this.finger[index]; }

  public Finger[] getFingers() throws RemoteException { return this.finger; }

  public Vector<Store> getDataStore() throws RemoteException { return this.dataStore; }


  // -- Modification utilities --

  /**
   * Voluntarily leave the network so that the node can be safely terminated.
   */
  public void addData(String key, byte[] value) throws RemoteException {
    // First check if the key has already been stored in the nodes local data store.
    for (Store store : this.dataStore) {
      if (store.getKey().equals(key)) {
        // Data already exists, don't store a duplicate.
        return;
      }
    }
    this.dataStore.add(new Store(key, value));
  }

  /**
   * Remove an item from the local data store.
   */
  public void removeData(String key) throws RemoteException {
    // Iterate throught the data store to find the key.
    for (Store store : this.dataStore) {
      // Found the key, remove the item.
      if (store.getKey().equals(key)) { this.dataStore.remove(store); }
    }
  }


  // -- Topology management functions --

  /**
   * Voluntarily leave the network so that the node can be safely terminated.
   */
  public void join(IChordNodeServer atNode) throws RemoteException {
    System.out.println("--- client join request handled ---");
    predecessor = null;
    predecessorKey = 0;

    try {
      if (atNode == this) {
        // Node joining to itself:
        predecessor = this;
        predecessorKey = getKey();
        successor = this;
        successorKey = getKey();
        for (int i = 0; i < KEY_BITS; i++) {
          finger[i].setNode(this);
          finger[i].setKey(getKey());
        }
        return;
      }
      // Use joining node to find your successor.
      successor = atNode.findSuccessor(getKey());
      successorKey = successor.getKey();

      // Set predecessor to the predecessor of the successor.
      predecessor = successor.getPredecessor();
      predecessorKey = predecessor.getKey();

      // Notify the successor that you should be its predecessor.
      successor.notifyNode(this);

      // Initialise finger table, and update others that you should be in their finger tables.
      initFingerTable(atNode);
      updateOthers();

    } catch (Exception e) {
      // atNode does not exist, or has failed. Therefore this is the only node in the network.
      predecessor = this;
      predecessorKey = getKey();
      successor = this;
      successorKey = getKey();
      for (int i = 0; i < KEY_BITS; i++) {
        finger[i].setNode(this);
        finger[i].setKey(getKey());
      }
      return;
    }

    // Set maintenance loop going.
    maintain = true;
  }

  /**
   * Voluntarily leave the network so that the node can be safely terminated.
   */
  public void leave() throws RemoteException {
    // Pause the maintenance loop in the thread
    maintain = false;
    // make successor the predecessor's successor

    IChordNodeServer pred = getPredecessor();
    IChordNodeServer succ = getSuccessor();

    // Pass data to predecessor
    moveAllDataToSuccessor(succ);

    // make successor the predecessor's successor
    pred.setSuccessor(succ);
    setPredecessor(this);
    setPredecessorKey(getKey());
    //make predecessor the sucessor's predecessor
    succ.setPredecessor(pred);
    setSuccessor(this);
    setSuccessorKey(getKey());

    // Print out updated topography.
    succ.printTopography();
  }

  /**
   * Initialise the finger table.
   */
  private void initFingerTable(IChordNodeServer atNode) throws RemoteException {
    // Set first finger to the successor node.
    finger[0].setNode(successor);
    finger[0].setKey(successorKey);
    for (int i = 1; i < KEY_BITS; i++) {
      // Use other node in the network to find out what the other fingers should be.
      IChordNodeServer next = atNode.findSuccessor(getKey() + ((int) Math.pow(2, i)));
      finger[i].setNode(next);
      finger[i].setKey(next.getKey());
    }
  }

  /**
   * Update all nodes whose finger tables should refer to this node.
   * Using pseudocode from Stoica et al (2001).
   */
  private void updateOthers() throws RemoteException {
    for (int i = 0; i < KEY_BITS; i++) {
      IChordNodeServer p = closestPrecedingNode(getKey() - ((int) Math.pow(2, i-1)));
      p.updateFingerTable(this, i);
    }
  }

  /**
   * If s is the ith finger of n's finger table, update it.
   * Using pseudocode from Stoica et al (2001).
   */
  public void updateFingerTable(IChordNodeServer s, int i)  throws RemoteException {
    if (isInHalfOpenRangeL(s.getKey(), getKey(), finger[i].node.getKey())) {
      finger[i].node = s;
      finger[i].key = s.getKey();
      successor = s;
      successorKey = successor.getKey();
      predecessor.updateFingerTable(s, i);
    }
  }

  /**
   * Pass down all data to predecessor (when a node leaves the network).
   */
  private void moveAllDataToSuccessor(IChordNodeServer succ) throws RemoteException {
    Iterator it = getDataStore().iterator();
    while (it.hasNext()) {
      Store data = (Store) it.next();
      succ.addData(data.getKey(), data.getValue());
      //it.remove(); // To avoid ConcurrentModificationException, use iterator to remove item.
    }
  }
  // -- utility functions --

  /**
   * Find the next sucessor node in the network for the given key.
   * Using pseudocode from lecture slides of SCC401.
   */
  public IChordNodeServer findSuccessor(int key) throws RemoteException {
    if (successor == this || isInHalfOpenRangeR(key, getKey(), successorKey)) {
      return successor;
    }
    else {
      IChordNodeServer n0 = closestPrecedingNode(key);
      if (n0 == this) {
        return this;
      }
      else return n0.findSuccessor(key);
    }
  }

  /**
   * Find the closest predecessor node in the network for the given key.
   * Using pseudocode from lecture slides of SCC401.
   */
  public IChordNodeServer closestPrecedingNode(int key) throws RemoteException {
    for (int i = fingerTableLength - 1; i >= 0 && i < fingerTableLength; i--) {
      if (getFinger(i).getNode() != null && isInClosedRange(getFinger(i).getKey(), getKey(), key)) {
        IChordNodeServer node = getFinger(i).getNode();
        try {
          node.getKey(); // Checking if the node is still alive.
          return node;
        } catch (Exception e) {
          // Node is no longer in the network, so set the finger table element to null.
          finger[i].node = null;
          finger[i].key = 0;
          nextFingerFix = i; // Set nextFingerFix to the current finger index so that it is the next finger in the table to be fixed.
        }
      }
    }
    // No fingers in the table preceed the key given, so it's closest predecessor is this node.
    return this;
  }

  // -- range check functions; they deal with the added complexity of range wraps --

  /**
   * x is in [a,b] ?
   */
  private boolean isInOpenRange(int key, int a, int b) {
    if (b > a) { return key >= a && key <= b; }
    else { return key >= a || key <= b; }
  }

  /**
   * x is in (a,b) ?
   */
  private boolean isInClosedRange(int key, int a, int b) {
    if (b > a) { return key > a && key < b; }
    else { return key > a || key < b; }
  }

  /**
   * x is in [a,b) ?
   */
  private boolean isInHalfOpenRangeL(int key, int a, int b) {
    if (b > a) { return key >= a && key < b; }
    else { return key >= a || key < b; }
  }

  /**
   * x is in (a,b] ?
   */
  private boolean isInHalfOpenRangeR(int key, int a, int b) {
    if (b > a) { return key > a && key <= b; }
    else { return key > a || key <= b; }
  }

  // -- hash functions --

  /**
   * This function converts a string "s" to a key that can be used with the DHT's API functions
   */
  private int hash(String s) {
    int hash = 0;

    for (int i = 0; i < s.length(); i++) {
      hash = hash * 31 + (int) s.charAt(i);
    }

    if (hash < 0) { hash = hash * -1; }

    return hash % ((int) Math.pow(2, KEY_BITS));
  }


  // -- maintenance --

  /**
   * Update the predecessor of the current node.
   */
  public void notifyNode(IChordNodeServer potentialPredecessor) throws RemoteException {
    try {
      if (getPredecessor() == null || isInClosedRange(potentialPredecessor.getKey(), getPredecessor().getKey(), getKey())) {
        // The potentialPredecessor should be the current predecessor.
        setPredecessor(potentialPredecessor);
        predecessorKey = getPredecessor().getKey();
      }
    } catch (Exception e) {
      // predecessor is null or has failed, so set the current predecessor to null.
      predecessor = null;
      predecessorKey = 0;
    }
  }

  /**
   * Update the predecessor of the current successor.
   */
  public void stabilise() throws RemoteException {
    // In case of node failure, find successor.
    try {
      IChordNodeServer x;
      try {
        x = getSuccessor().getPredecessor();
      } catch (Exception e) {
        // Successor has failed or is null.
        setSuccessor(findSuccessor(getKey()));
        setSuccessorKey(successor.getKey());
        x = getSuccessor().getPredecessor();
      }
      if (x != null) {
        try {
          if (isInClosedRange(x.getKey(), getKey(), getSuccessor().getKey())) {
            // The predecessor of the successor should be the successor of this node.
            setSuccessor(x);
            successorKey = getSuccessor().getKey();
          }
        } catch (Exception e) {
          // Successor has failed or is null.
          x.notifyNode(this); // Become the predecessor of x.
          setSuccessor(x);
          successorKey = getSuccessor().getKey();
        }
      }
      // Notify the successor that you should be its predecessor.
      getSuccessor().notifyNode(this);
    } catch (RemoteException e) {
      // Successor has failed, or is null:
      successor = null;
      successorKey = 0;
    }
  }


  /**
   * Fix the current finger that nextFingerFix is pointing to.
   */
  public void fixFingers() throws RemoteException {
    // Make sure the finger index does not go out of bounds.
    if (nextFingerFix >= KEY_BITS) {
      nextFingerFix = 0;
    }
    // Find the node that should be stored in the current finger index, and update it.
    IChordNodeServer next = findSuccessor(getKey() + ((int) Math.pow(2, nextFingerFix)));
    try {
      finger[nextFingerFix].setNode(next);
      finger[nextFingerFix].setKey(next.getKey());
    } catch (Exception e) {
      // Next node has failed, or is null:
      finger[nextFingerFix].setNode(null);
      finger[nextFingerFix].setKey(0);
    }
    // Move the index to the next finger.
    nextFingerFix++;
  }

  /**
   * Check if the current predecessor has failed, if so, set the predecessor to null.
   */
  public void checkPredecessor() throws RemoteException {
    try {
      getPredecessor().getKey();
    }
    catch (Exception e) {
      // predecessor is either null, or has failed. Update to closestPrecedingNode.
      predecessor = closestPrecedingNode(predecessorKey);
      predecessorKey = predecessor.getKey();

      // Notify the predecessor that you should be it's sucsessor.
      predecessor.notifyNode(this);
    }
  }

  /**
   * Check if storing data that the current predecessor should be storing.
   * If so, move it to the predecessor.
   */
  public void checkDataMoveDown() throws RemoteException {
    // loop through the data store vector, and check if their keys are within the predecessors range.
    if (dataStore != null) {
      Iterator it = getDataStore().iterator();
      while (it.hasNext()) {
        Store data = (Store) it.next();
        int hashedKey = hash(data.getKey());
        IChordNodeServer correctNode = findSuccessor(hashedKey);
        if (correctNode.getKey() != getKey()) {
          System.out.println("checkDataMoveDown for node " + getKeyString() + ", to " + correctNode.getKeyString());
          correctNode.put(data.getKey(), data.getValue());
          it.remove(); // To avoid ConcurrentModificationException, use iterator to remove item.
        }
      }
    }
  }

  /**
   * Print the current network topography in the command line.
   * Shows the sucessor of each node, followed by all data stored in that node, along with the finger table for that node.
   */
  public void printTopography() throws RemoteException {
    System.out.println("--- Current Topography ---");
    IChordNodeServer currentNode = this;
    // Loop through successors until the original node is reached, to print out the full topography.
    while (!(currentNode.getKeyString().equals(getPredecessor().getKeyString()))) {
      printNodeDetails(currentNode);
      currentNode = currentNode.getSuccessor();
    }
    printNodeDetails(currentNode);

    System.out.println("--- End of Topography ---");
  }

  /**
   * Print the details of the given node in the network.
   */
  private void printNodeDetails(IChordNodeServer node) throws RemoteException {
    System.out.println(node.getKeyString() + " -> " + node.getSuccessor().getKeyString());
    if (node.getDataStore() != null) {
      // Print out the contents of the data store.
      for (Store data : node.getDataStore()) {
        System.out.println("[Data] key: " + data.getKey() + " value: " + data.getValue());
      }
    }
    for (int i = 0; i < KEY_BITS; i++) {
      // Print out all non-null finger table entries.
      if (node.getFinger(i).getNode() != null)
      System.out.println("[finger " + i + "] Node: " + node.getFinger(i).getNode().getKeyString() + " key: " + node.getFinger(i).getKey());
    }
  }

  /**
   * Command Line Interface for managing the node within the network.
   */
  public void cmdlnInterface() throws RemoteException {
    System.out.println("All done (press ctrl-c to quit at ant time)");
    System.out.println("To insert data, type 'put' followed by the name of the file you wish to insert, separated by a space.");
    System.out.println("To get the value of stored data, type 'get' followed by the name of the file you wish to view, separated by a space.");
    System.out.println("To remove data, type 'remove' followed by the name of the file you wish to remove, separated by a space.");
    System.out.println("To leave the network, type 'leave'.");
    System.out.println("To view the network, type 'print'.");

    usrInput: while (true) {
      Scanner scanner = new Scanner(System.in);
      String input = scanner.nextLine();
      String[] splitInput = input.split("\\s");

      if (splitInput.length > 0) {
        String cmd = splitInput[0];
        String key = "";
        Path filePath = null;
        byte[] data = null;
        try {
          if (splitInput.length > 1) {
            key = splitInput[1];
            filePath = Paths.get(key);
            data = Files.readAllBytes(filePath);
          }
          switch (cmd) {
            case "put":
              put(key, data);
              System.out.println("Successfully stored node " + key + ", " + data);
              break;
            case "get":
              System.out.println("Data: " + get(key));
              break;
            case "remove":
              remove(key);
              System.out.println("Successfully removed node " + key);
              break;
            case "leave":
              leave();
              System.out.println("Node has left the network...Press ctrl + c to terminate.");
              break usrInput; // Node has left the network, so stop looping for user input.
            case "print":
              printTopography();
              break;
            default:
              System.out.println("Please enter a valid command.");
              break;
          }
        } catch (IOException e) {
          System.out.println("Error - (" + filePath + ") File not found, please try again.");
          //return null;
        } catch (Exception e) {
          System.out.println("Error processing command, please try again.");
        }
      }
      else {
        System.out.println("Please enter a valid command.");
      }
    }
  }

  /**
   * Public method called by REST server to make asychonous method call to submit a task to the DHT node.
   */
  public void submitAsyncTask(String fileName, byte[] fileData, Task task) throws RemoteException {
    new Thread(new Runnable() {
      public void run() {
        try {
          analyseFile(fileName, fileData, task);
        }
        catch (RemoteException e) {
          System.out.println("RemoteException occurred when analysing file...");
        }
      }
    }).start();
  }

  /**
   * Analyse file depending on the type of analysis task that was requested.
   */
  private void analyseFile(String fileName, byte[] fileData, Task task) throws RemoteException {
    try {
      TextFileAnalysis analysis = new TextFileAnalysis();

      // Read in the file data.
      InputStream input = new ByteArrayInputStream(fileData);

      // Pass the data to the relevant analysis function and put the result in the DHT.
      // Notify the REST server when the task has finished.
      switch(task) {
        case WORDS:
          put(fileName + task.getValue(), analysis.wordAnalysis(fileName, input));
          notifyServerTaskComplete(fileName, task);
          break;
        case LETTERS:
          put(fileName + task.getValue(), analysis.letterAnalysis(fileName, input));
          notifyServerTaskComplete(fileName, task);
          break;
        case LINES:
          put(fileName + task.getValue(), analysis.lineAnalysis(fileName, input));
          notifyServerTaskComplete(fileName, task);
          break;
        default:
          System.out.println("Could not analyse file.");
          break;
      }
      input.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Call a method on the server to indicate that the task has been completed, so that the XML file can be appended to by the server.
   */
  private void notifyServerTaskComplete(String fileName, Task task) {
    System.out.println("Notifying server of task [" + fileName + "] completion...");
    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource service = client.resource(getBaseURI());

    // Request the 'appendTask' server POST method to notify the REST server of the task completion.
    ClientResponse post = service.path("rest").path("files/appendTask").type(MediaType.TEXT_PLAIN).accept(MediaType.TEXT_PLAIN).post(ClientResponse.class, fileName + "," + task.getValue());
    System.out.println("...Server has been notified.");
    // Print the response of the POST request.
    System.out.println(post.getEntity(String.class));
  }

  /**
   * Get the base URI for the REST server.
   */
  private static URI getBaseURI() {
		return UriBuilder.fromUri("http://localhost:8080/myapp/").build();
	}

  public void run() {
    while (maintain)
    {
      try{
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("Interrupted");
			}

			try{
				stabilise();
      } catch (Exception e) {
        System.out.println("A node has left the network. Updating...");
      }

			try{
				fixFingers();
      } catch (Exception e) {
        System.out.println("A node has left the network. Updating...");
      }

			try{
				checkPredecessor();
      } catch (Exception e) {
        System.out.println("A node has left the network. Updating...");
      }

			try{
				checkDataMoveDown();
      } catch (Exception e) {
        System.out.println("A node has left the network. Updating...");
      }
    }
  }

  public static void main(String args[]) {
    try {
      // Get the name of the node to create/join to the network.
      String keyString = args[0];
      IChordNodeServer nodeServer;

      if (args.length == 1) {
        // Creating node in a single node network:
        nodeServer = new ChordNode(keyString);
      }
      else if (args.length > 1) {
        // Creating node and joining to network:
        String atNodeKeyString = args[1];
        nodeServer = new ChordNode(keyString, atNodeKeyString);
      }
      else {
        return;
      }
      // -- wait a bit for stabilisation --
      System.out.println("Waiting for topology to stabilise...");

      try {
        Thread.sleep(7000);
      }
      catch (InterruptedException e) {
        System.out.println("Interrupted");
      }

      // Print out the current network topology:
      nodeServer.printTopography();

      // Start the command line interface to manage the node in the network:
      nodeServer.cmdlnInterface();
    }
    catch (Exception e) {
      System.err.println("Exception:");
      e.printStackTrace();
    }
  }
}
