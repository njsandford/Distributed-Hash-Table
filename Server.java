/*
One instance of this class is created for each incoming HTTP request (which maps to exactly one REST-annotated function of the class)
   - that instance is discarded at the end of the HTTP request
   - note that instance variables are therefore of no use because instances only exist for the duration of a single function call
   - any state must therefore be in "static" class variables; this matches the REST philosophy of "no per-client state at the server"
*/

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import javax.ws.rs.core.MediaType;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.ConnectIOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;

import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.core.header.FormDataContentDisposition;

import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletResponse;

import java.net.URI;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import java.util.Random;

import java.io.FileWriter;
import java.io.BufferedWriter;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import org.w3c.dom.*;

//everything on /base/files/ will hit this class
@Path("/files")
public class Server {

	private static final String PREFIX = "/var/lib/tomcat8/webapps/myapp/files/";

	public static byte[] convertInputStreamToByteArrary(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		final int BUF_SIZE = 1024;
		byte[] buffer = new byte[BUF_SIZE];
		int bytesRead = -1;
		while ((bytesRead = in.read(buffer)) > -1) {
			out.write(buffer, 0, bytesRead);
		}
		in.close();
		byte[] byteArray = out.toByteArray();
		return byteArray;
	}

	/**
	 * Donloads the results of any given completed task as an XML document.
	 * Takes two parameters: task (the type of task), and filename (the name of the task that was carried out).
	 */
	@GET
	@Path("/completedTasks/download/{task}/{file}")
	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	public Response downloadResult(@PathParam("task") String task, @PathParam("file") String fileName) {
		try {
			// Lookup the results of the task submission using a random 'worker' node in the DHT network.
	    Registry registry = LocateRegistry.getRegistry();
			IChordNodeServer nodeServer = (IChordNodeServer) registry.lookup(randomWorkerNodeName());

			// Access the results of the task submission in the form of a byte[] representing the contents of an XML file.
	    byte[] fileData = nodeServer.get(fileName + task);

			// Return the XML file as a response.xs
			return Response
            .ok(fileData, MediaType.APPLICATION_OCTET_STREAM)
            .header("content-disposition","attachment; filename = " + fileName + ".xml")
            .build();

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();

			return Response.status(Response.Status.NOT_FOUND).entity("<p>" + exceptionAsString + "</p>").build();
		}
	}

	/**
	 * Displays the results of any given completed task.
	 * Takes two parameters: task (the type of task), and filename (the name of the task that was carried out).
	 */
	@GET
	@Path("/completedTasks/{task}/{file}")
	@Produces({ MediaType.TEXT_HTML})//MediaType.APPLICATION_OCTET_STREAM })
	public Response getResult(@PathParam("task") String task, @PathParam("file") String fileName) {
		try {
			// Lookup the results of the task submission using a random 'worker' node in the DHT network.
	    Registry registry = LocateRegistry.getRegistry();
	    IChordNodeServer nodeServer = (IChordNodeServer) registry.lookup(randomWorkerNodeName());

			// Access the results of the task submission in the form of a byte[] representing the contents of an XML file.
	    byte[] getBytes = nodeServer.get(fileName + task);

	    // Convert byte[] back to XML document.
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    Document result = builder.parse(new ByteArrayInputStream(getBytes));
	    Element root = result.getDocumentElement();

	    String html = "";

			// Depending on the task specified, generate the relevant html content from the results of the file analysis:
			switch (task) {
		    case "words":
					NodeList wordCountList = root.getElementsByTagName("WordCount");
			    NodeList modeWordList = root.getElementsByTagName("ModeWord");
			    NodeList averageWordLengthList = root.getElementsByTagName("AverageWordLength");
			    String wordCount = wordCountList.item(0).getFirstChild().getTextContent();
			    String modeWord = modeWordList.item(0).getFirstChild().getTextContent();
			    String averageWordLength = averageWordLengthList.item(0).getFirstChild().getTextContent();

			    html = "<p>WordCount: " + wordCount + ", ModeWord: " + modeWord + ", AverageWordLength: " + averageWordLength + "</p>";
					break;
				case "letters":
					NodeList charCountList = root.getElementsByTagName("CharacterCount");
				  NodeList blankCharsList = root.getElementsByTagName("BlankCharacters");
				  NodeList modeCharacterList = root.getElementsByTagName("ModeCharacter");
				  String charCount = charCountList.item(0).getFirstChild().getTextContent();
				  String blankChars = blankCharsList.item(0).getFirstChild().getTextContent();
				  String modeCharacter = modeCharacterList.item(0).getFirstChild().getTextContent();

				  html = "<p>CharacterCount: " + charCount + ", BlankCharacters: " + blankChars + ", ModeCharacter: " + modeCharacter + "</p>";
					break;
				case "lines":
					NodeList lineCountList = root.getElementsByTagName("LineCount");
				  NodeList blankLinesList = root.getElementsByTagName("BlankLines");
				  NodeList aveCharsPerLineList = root.getElementsByTagName("AverageCharsPerLine");
				  String lineCount = lineCountList.item(0).getFirstChild().getTextContent();
				  String blankLines = blankLinesList.item(0).getFirstChild().getTextContent();
				  String aveCharsPerLine = aveCharsPerLineList.item(0).getFirstChild().getTextContent();

				  html = "<p>LineCount: " + lineCount + ", BlankLines: " + blankLines + ", AverageCharsPerLine: " + aveCharsPerLine + "</p>";
					break;
				default:
					return Response.status(Response.Status.NOT_FOUND).entity("").build();
			}
			return Response.status(Response.Status.OK).entity(html).build();
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
      //e.printStackTrace();
			return Response.status(Response.Status.NOT_FOUND).entity("<p>" + exceptionAsString + "</p>").build();
		}
	}

	/**
	 * Display a HTML page with a list of all completed tasks for each task type.
	 * Accesses a 'completedTasks' XML document containing all the tasks which have been completed for each task type.
	 */
	@GET
	@Path("/completedTasks")
	@Produces({ MediaType.TEXT_HTML })
	public Response listCompletedTasks() {
		try {
			File wordsXml = new File(PREFIX + "completedTasks.xml");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(wordsXml);
			Element root = doc.getDocumentElement();
			NodeList fileList;

			// Get a predefined html template file for the completedTasks page, and convert it into a string to modify.
			String htmlString = new String(Files.readAllBytes(Paths.get("/var/lib/tomcat8/webapps/myapp/completedTasks.html")), StandardCharsets.UTF_8);

			// Get the page content for the words analysis tasks:
			String wordContent = "";
			fileList = root.getElementsByTagName("Words");
			if (fileList.getLength() != 0) {
				for (int i = 0, len = fileList.getLength(); i < len; i++) {
						Element file = (Element)fileList.item(i);
						String fileName = file.getFirstChild().getTextContent();
						wordContent += "<form action='completedTasks/download/words/" + fileName + "'><p><a href='completedTasks/words/" + fileName + "'>" + fileName + "</a><input type='submit' value='Download' /></p></form>";
				}
			} else {
				wordContent += "<p>No word analysis tasks have been completed.</p>";
			}

			// Get the page content for the letters analysis tasks:
			String letterContent = "";
			fileList = root.getElementsByTagName("Letters");
			if (fileList.getLength() != 0) {
				for (int i = 0, len = fileList.getLength(); i < len; i++) {
						Element file = (Element)fileList.item(i);
						String fileName = file.getFirstChild().getTextContent();
						letterContent += "<form action='completedTasks/download/letters/" + fileName + "'><p><a href='completedTasks/letters/" + fileName + "'>" + fileName + "</a><input type='submit' value='Download' /></p></form>";
				}
			} else {
				letterContent += "<p>No letter analysis tasks have been completed.</p>";
			}

			// Get the page content for the lines analysis tasks:
			String lineContent = "";
			fileList = root.getElementsByTagName("Lines");
			if (fileList.getLength() != 0) {
				for (int i = 0, len = fileList.getLength(); i < len; i++) {
					Element file = (Element)fileList.item(i);
					String fileName = file.getFirstChild().getTextContent();
					lineContent += "<form action='completedTasks/download/lines/" + fileName + "'><p><a href='completedTasks/lines/" + fileName + "'>" + fileName + "</a><input type='submit' value='Download' /></p></form>";
				}
			} else {
				lineContent += "<p>No line analysis tasks have been completed.</p>";
			}

			htmlString = htmlString.replace("$wordContent", wordContent);
			htmlString = htmlString.replace("$letterContent", letterContent);
			htmlString = htmlString.replace("$lineContent", lineContent);

			return Response.status(Response.Status.OK).entity(htmlString).build();

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			//e.printStackTrace();
			return Response.status(Response.Status.NOT_FOUND).entity("<p>" + exceptionAsString + "</p>").build();
		}
	}


	/**
	 * Append a file element to the 'completedTasks' XML document to indicate to the server that a task has been completed.
	 */
	@POST
	@Path("/appendTask")
	@Produces({ MediaType.TEXT_PLAIN })
	@Consumes({ MediaType.TEXT_PLAIN })
	public Response appendTask(String fileName) throws IOException {
		try {
			// Filename and task type sent as a single string separated by a ',' to avoid sending the data as a form.
			String[] strings = fileName.split(",");
			String task = strings[1];
			fileName = strings[0]; // No need to create a new object for the filename.
      String filePath = PREFIX + "completedTasks.xml";//PREFIX + "completedTasks/" + taskType + ".xml";
      File file = new File(filePath);

      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document doc = documentBuilder.parse(filePath);
			Element root = doc.getDocumentElement();
			Element fileEl;

			switch (task) {
				case "words":
					fileEl = doc.createElement("Words");
					fileEl.appendChild(doc.createTextNode(fileName));
					root.appendChild(fileEl);
					break;
				case "letters":
					fileEl = doc.createElement("Letters");
					fileEl.appendChild(doc.createTextNode(fileName));
					root.appendChild(fileEl);
					break;
				case "lines":
					fileEl = doc.createElement("Lines");
					fileEl.appendChild(doc.createTextNode(fileName));
					root.appendChild(fileEl);
					break;
				default:
					//throw new InvalidArgumentException();
					break;
			}

      // Write the content into an xml file - override existing file.
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
			FileOutputStream stream = new FileOutputStream(file);
      StreamResult result = new StreamResult(stream);
      transformer.transform(source, result);
			return Response.status(Response.Status.OK).entity(fileName).build();

    } catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			return Response.status(Response.Status.NOT_FOUND).entity(exceptionAsString).build();
    }
	}

	/**
	 * Submits a task to the DHT using RMI client code.
	 * Designates the task to a random 'worker' node in the network to carry out the task.
	 */
  @POST
  @Path("/submitTask")
	@Produces({ MediaType.TEXT_HTML })
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  public void submitTask(@FormDataParam("task") String task,
			@FormDataParam("name") String filename,
			@FormDataParam("content") InputStream contentStream,
			@Context HttpServletResponse servletResponse) throws IOException {
		try {
			//by default files are looked for in Tomcat's root directory, so we prepend our subdirectory on there first...
	    String filenamePrefix = PREFIX + filename;

			// Must convert InputStream to byte[] to pass via RMI.
			byte[] contentBytes = convertInputStreamToByteArrary(contentStream);

	    Registry registry = LocateRegistry.getRegistry();
			// Get a node from the RMI registry at random to be the worker node.
	    IChordNodeServer nodeServer = (IChordNodeServer) registry.lookup(randomWorkerNodeName());

			// Submit the corresponding task to the designated 'worker' node in the DHT.
			switch (task) {
				case "words":
					nodeServer.submitAsyncTask(filename, contentBytes, Task.WORDS);
					break;
				case "letters":
					nodeServer.submitAsyncTask(filename, contentBytes, Task.LETTERS);
					break;
				case "lines":
					nodeServer.submitAsyncTask(filename, contentBytes, Task.LINES);
					break;
				default:
					break;
			}
		}
		catch (Exception e) {
      e.printStackTrace();
		}
		// Redirect client back to the task submission page upon task submission.
		servletResponse.sendRedirect("../../");
  }

	/**
	 * Pick at node from the network at random to be the 'worker' node to avoid always calling the same node to carry out the analysis task.
	 * Could just use the same node everytime but if many tasks are being submitted simultaneously this could slow performance.
	 */
	private static String randomWorkerNodeName() throws RemoteException, NotBoundException {
		boolean validName = false;
		String[] boundNames = null;
		int nodeIndex = -1;
		// Keep getting a random index from the registry list until a valid node is found.
		while (!validName) {
			Registry registry = LocateRegistry.getRegistry();
			boundNames = registry.list(); // Get a list of all bound names from registry.
			int noOfNames = boundNames.length;
			// Generate random number between 0 and the number of bound names in the RMI registry.
			Random rand = new Random();
			nodeIndex = rand.nextInt(noOfNames);
			try {
				// Checking that the index corresponds to a valid name in the network.
				IChordNodeServer nodeServer = (IChordNodeServer) registry.lookup(boundNames[nodeIndex]);
				nodeServer.getKey(); // Checking that the node hasn't failed.
				validName = true; // Indicate that a valid name has been found; stop looking.
			} catch (Exception e) {
				// Not a valid node, unbind from registry for next time.
				registry.unbind(boundNames[nodeIndex]);
			}
		}
		return boundNames[nodeIndex];
	}
}
