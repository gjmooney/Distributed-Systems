package client;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with the current state
 *     -> modal means that it opens the GUI and suspends background processes. Processing 
 *        still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */
public class ClientGui implements client.OutputPanel.EventHandlers {
  JDialog frame;
  PicturePanel picturePanel;
  OutputPanel outputPanel;
  static ObjectOutputStream outputStream;
  static ObjectInputStream inputStream;
  static Socket serverSock;
  static int state; //track which point of the game we're in

  /**
   * Construct dialog
   */
  public ClientGui() {
    frame = new JDialog();
    frame.setLayout(new GridBagLayout());
    frame.setMinimumSize(new Dimension(500, 500));
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    // setup the top picture frame
    picturePanel = new PicturePanel();
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weighty = 0.25;
    frame.add(picturePanel, c);

    // setup the input, button, and output area
    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.weighty = 0.75;
    c.weightx = 1;
    c.fill = GridBagConstraints.BOTH;
    outputPanel = new OutputPanel();
    outputPanel.addEventHandlers(this);
    frame.add(outputPanel, c);
  }

  /**
   * Shows the current state in the GUI
   * @param makeModal - true to make a modal window, false disables modal behavior
   */
  public void show(boolean makeModal) {
    frame.pack();
    frame.setModal(makeModal);
    frame.setVisible(true);
  }

  /**
   * Creates a new game and set the size of the grid 
   * @param dimension - the size of the grid will be dimension x dimension
   */
  public void newGame(int dimension) {
    picturePanel.newGame(dimension);
    outputPanel.setOutput("Started new game with a " + dimension + "x" + dimension + " board.");
  }

  /**
   * Insert an image into the grid at position (col, row)
   * 
   * @param image - decoded image sent from server
   * @param row - the row to insert into
   * @param col - the column to insert into
   * @return true if successful, false if an invalid coordinate was provided
   * @throws IOException An error occured with your image file
   */
  public boolean insertImage(ImageIcon image, int row, int col) throws IOException {
    String error = "";
    try {
      // insert the image
      picturePanel.insertImage(image, row, col);
      return true;
    } catch(PicturePanel.InvalidCoordinateException e) {
      // put error in output
      error = "Error receiving image from server.\n";
      error += e.toString();
    }
    outputPanel.setOutput(error);
    return false;
  }

  /**
   * Submit button handling
   * 
   * Change this to whatever you need
   */
  @Override
  public void submitClicked() {
    // Pulls the input box text
    String input = outputPanel.getInputText();
    // if has input
    if (input.length() > 0) {
      // append input to the output panel
      outputPanel.setOutput(input);
      sendToServer(state, input);
      // clear input text box
      outputPanel.setInputText("");
    }

    try {
      receiveFromServer();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Key listener for the input text box
   * 
   * Change the behavior to whatever you need
   */
  @Override
  public void inputUpdated(String input) {
    if (input.equals("surprise")) {
      outputPanel.setOutput("You found me!");
    }
  }

  public void establishConnection(String[] args) {
    serverSock = null;
    int port = 8080;

    if (args.length != 2) {
      System.out.println("Expected two arguments: <host(String)> <port(int)>");
      System.exit(1);
    }
    String host = args[0];
    try {
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      System.out.println("Port number must be an integer");
      System.exit(2);
    }

    try {
      serverSock = new Socket(host, port);
      outputStream = new ObjectOutputStream(serverSock.getOutputStream());
      inputStream = new ObjectInputStream(serverSock.getInputStream());
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Could not establish connection over port: " + port);
      System.exit(2);
    }
  }

  public void receiveFromServer() {
    System.out.println("Receiving from server");
    try {
      String jsonData = (String) inputStream.readObject();
      JSONTokener jsonTokener = new JSONTokener(jsonData);
      JSONObject jsonObj = new JSONObject(jsonTokener);
      JSONObject headerJSON = (JSONObject) jsonObj.get("header");
      JSONObject payloadJSON = (JSONObject) jsonObj.get("payload");
      Map<String, Object> header = headerJSON.toMap();
      Map<String, Object> payload = payloadJSON.toMap();
      if (header.get("state").equals(5)) {
        serverSock.close();
        inputStream.close();
        outputStream.close();
        System.exit(0);
      }

      outputPanel.setPoints((Integer) payload.get("score"));

      if (!(payload.get("image").equals("unableToEncode"))) {
        ImageIcon image = decodeImage((String) payload.get("image"));
        insertImage(image, 0 ,0);
      }

      outputPanel.setOutput((String) payload.get("text"));
      outputStream.flush();

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Error receiving from server");
      System.exit(1);
    }
  }

  public static void sendToServer(int state, String message) {
    try {
      JSONObject objectToSend = new JSONObject();
      JSONObject objHeader = new JSONObject();
      JSONObject objPayload = new JSONObject();
      objHeader.put("state", state);
      objHeader.put("type", "text");
      objHeader.put("ok", true);
      objPayload.put("text", message);

      objectToSend.put("header", objHeader);
      objectToSend.put("payload", objPayload);

      outputStream.writeObject(objectToSend.toString());

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Error sending to server");
    }
  }

  //Adapted from AdvanceCustomProtocol example
  public ImageIcon decodeImage(String imageString) throws IOException {
    Base64.Decoder decoder = Base64.getDecoder();
    byte[] bytes = decoder.decode(imageString);
    ImageIcon icon;
    try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
      BufferedImage image = ImageIO.read(bais);
      icon = new ImageIcon(image);
    }
    return icon;
  }

  public static void main(String[] args) {
    // create the frame
    ClientGui main = new ClientGui();
    main.establishConnection(args);

    // setup the UI to display on image
    main.newGame(1);

    try {
      main.receiveFromServer();
      main.show(false);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Error receiving from server in main");
      System.exit(5);
    }
    // show the GUI dialog as modal
    //main.show(true); // you should not have your logic after this. You main logic should happen whenever "submit" is clicked
  }
}
