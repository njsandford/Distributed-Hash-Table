import java.util.Vector;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

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

import org.w3c.dom.*;

/**
 * Utility class to carry out text file analysis tasks.
 */
public class TextFileAnalysis {

  /**
   * Count the total number of words, the most frequently occuring word, and the average word length.
   */
  public byte[] wordAnalysis(String fileName, InputStream in) {
    Map<String, Integer> wordFreq = new HashMap<String, Integer>();

    int wordCount = 0;
    int letterCount = 0;
    int avWordLength = 0;

    Scanner input = new Scanner(in);
    while (input.hasNext()) {
      String current = input.next();
      wordCount++;
      letterCount += current.length();
      if (wordFreq.get(current) == null) {
        wordFreq.put(current, 1);
      } else {
        wordFreq.put(current, wordFreq.get(current) + 1);
      }
    }
    input.close();

    if (wordCount != 0) {
      avWordLength = letterCount / wordCount;
    }

    String modeWord = getModeWord(wordFreq);

    return generateWordXML(fileName, wordCount, modeWord, avWordLength);
  }

  /**
   *  Count the total number of letters/characters, the number of 'space' characters, and the most frequently occuring letter.
   */
  public byte[] letterAnalysis(String fileName, InputStream input) throws IOException {
    Map<Character, Integer> charFreq = new HashMap<Character, Integer>();

    int charCount = 0;
    char modeChar = '\0';
    int blankChars = 0;

    Reader reader = new InputStreamReader(input, Charset.defaultCharset());
    Reader buffer = new BufferedReader(reader);
    while (reader.read() != -1) {
      char current = (char) reader.read();
      charCount++;
      if (charFreq.get(current) == null) {
        charFreq.put(current, 1);
      } else {
        charFreq.put(current, charFreq.get(current) + 1);
      }
      if (current == ' ') {
        blankChars++;
      }
    }
    buffer.close();
    modeChar = getModeLetter(charFreq);

    return generateLetterXML(fileName, charCount, blankChars, modeChar);
  }

  /**
   * Count the total number of lines, the number of blank lines, and the average number of characters per line.
   */
  public byte[] lineAnalysis(String fileName, InputStream input) {
    Map<String, Integer> lines = new HashMap<String, Integer>();

    int lineCount = 0;
    int letterCount = 0;
    int blankLines = 0;
    int avCharsPerLine = 0;

    Scanner scanner = new Scanner(input);
    while (scanner.hasNextLine()) {
      String current = scanner.nextLine();
      int currentLength = current.length();
      lines.put(current, currentLength);
      lineCount++;
      letterCount += currentLength;
      if (currentLength == 0) {
        blankLines++;
      }
    }

    if (lineCount != 0) {
      avCharsPerLine = letterCount / lineCount;
    }

    return generateLineXML(fileName, lineCount, blankLines, avCharsPerLine);
  }

  /**
   * Calculate the most frequently occurring word using a string-integer mapping.
   */
  private String getModeWord(Map<String, Integer> wordFreq) {
    String modeWord = "";
    int modeFreq = 0;
    for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
      if (modeFreq < entry.getValue()) {
        modeWord = entry.getKey();
        modeFreq = entry.getValue();
      }
    }
    return modeWord;
  }

  /**
   * Calculate the most frequently occurring char using a character-integer mapping.
   */
  private char getModeLetter(Map<Character, Integer> charFreq) {
    char modeChar = '\0';
    int modeFreq = 0;
    for (Map.Entry<Character, Integer> entry : charFreq.entrySet()) {
      if (modeFreq < entry.getValue()) {
        modeChar = entry.getKey();
        modeFreq = entry.getValue();
      }
    }
    return modeChar;
  }

  /**
   * Generate an XML document containing the results of the Word analysis task, and return as a byte array.
   */
  private byte[] generateWordXML(String fileName, int wordCount, String modeWord, int avWordLength) {
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      // Analysis Task elements
      Document doc = docBuilder.newDocument();
      Element analysis = doc.createElement("WordAnalysis");
      doc.appendChild(analysis);

      Element wordCountEl = doc.createElement("WordCount");
      wordCountEl.appendChild(doc.createTextNode(Integer.toString(wordCount)));
      analysis.appendChild(wordCountEl);

      Element modeWordEl = doc.createElement("ModeWord");
      modeWordEl.appendChild(doc.createTextNode(modeWord));
      analysis.appendChild(modeWordEl);

      Element avWordLengthEl = doc.createElement("AverageWordLength");
      avWordLengthEl.appendChild(doc.createTextNode(Integer.toString(avWordLength)));
      analysis.appendChild(avWordLengthEl);

      return documentToByteArray(doc);

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Generate an XML document containing the results of the Letter analysis task, and return as a byte array.
   */
  private byte[] generateLetterXML(String fileName, int charCount, int blankChars, char modeChar) {
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      Document doc = docBuilder.newDocument();
      Element analysis = doc.createElement("LetterAnalysis");
      doc.appendChild(analysis);

      Element charCountEl = doc.createElement("CharacterCount");
      charCountEl.appendChild(doc.createTextNode(Integer.toString(charCount)));
      analysis.appendChild(charCountEl);

      Element blankCharsEl = doc.createElement("BlankCharacters");
      blankCharsEl.appendChild(doc.createTextNode(Integer.toString(blankChars)));
      analysis.appendChild(blankCharsEl);

      Element modecharEl = doc.createElement("ModeCharacter");
      modecharEl.appendChild(doc.createTextNode(Character.toString(modeChar)));
      analysis.appendChild(modecharEl);

      return documentToByteArray(doc);

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Generate an XML document containing the results of the Line analysis task, and return as a byte array.
   */
  private byte[] generateLineXML(String fileName, int lineCount, int blankLines, int avCharsPerLine) {
    try {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      Document doc = docBuilder.newDocument();
      Element analysis = doc.createElement("LineAnalysis");
      doc.appendChild(analysis);

      Element lineCountEl = doc.createElement("LineCount");
      lineCountEl.appendChild(doc.createTextNode(Integer.toString(lineCount)));
      analysis.appendChild(lineCountEl);

      Element blankLinesEl = doc.createElement("BlankLines");
      blankLinesEl.appendChild(doc.createTextNode(Integer.toString(blankLines)));
      analysis.appendChild(blankLinesEl);

      Element avCharsPerLineEl = doc.createElement("AverageCharsPerLine");
      avCharsPerLineEl.appendChild(doc.createTextNode(Integer.toString(avCharsPerLine)));
      analysis.appendChild(avCharsPerLineEl);

      return documentToByteArray(doc);

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Convert a Document to a byte array.
   */
  private byte[] documentToByteArray(Document doc) {
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      StreamResult result = new StreamResult(bos);
      transformer.transform(source, result);

      return bos.toByteArray();
    } catch (TransformerException e) {
      e.printStackTrace();
      return null;
    }
  }

}
