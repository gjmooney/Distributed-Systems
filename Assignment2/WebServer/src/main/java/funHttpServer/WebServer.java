/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;


class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(new String(readFileInBytes(file)));
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          Integer num1;
          Integer num2;
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          if (request.equals("multiply?")) {
            num1 = 6;
            num2 = 8;
            // Generate response
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("No parameters found in GET request. <br>" +
                    "Proper format is /multiply?num1=3&num2=4 <br>" +
                    "Using default values of 6 and 8. <br>" +
                    "Result is: " + num1 * num2);
          } else {
            try {
              query_pairs = splitQuery(request.replace("multiply?", ""));
              if (query_pairs.size() != 2) {
                builder.append("HTTP/1.1 414 Bad Request\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("Only use 2 numbers PLEASE!");
              } else {
                num1 = Integer.parseInt(query_pairs.get("num1"));
                num2 = Integer.parseInt(query_pairs.get("num2"));

                // Generate response
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("Result is: " + num1 * num2);
              }
            } catch (NumberFormatException e) {
              num1 = 7;
              num2 = 2;

              // Generate response
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("GET request parameters must be numbers. <br>" +
                      "Proper format is /multiply?num1=3&num2=4 <br>" +
                      "Using default values of 7 and 2. <br>" +
                      "Result is: " + num1 * num2);
            }
          }

        } else if (request.contains("github?")) {
          // pulls the query from the request and runs it with GitHub's REST API
          // check out https://docs.github.com/rest/reference/
          //
          // HINT: REST is organized by nesting topics. Figure out the biggest one first,
          //     then drill down to what you care about
          // "Owner's repo is named RepoName. Example: find RepoName's contributors" translates to
          //     "/repos/OWNERNAME/REPONAME/contributors"

          if (request.equals("github?")) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Correct format is  /github?query=users/amehlhase316/repos. <br>");
            builder.append("Replace amehlase316 with whichever username you want to see");
          } else {
            try {
              Map<String, String> query_pairs = new LinkedHashMap<String, String>();
              query_pairs = splitQuery(request.replace("github?", ""));

              JSONParser parser = new JSONParser();
              String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));

              Object repo = parser.parse(json);
              JSONArray array = (JSONArray) repo;
              JSONObject tempObj;
              JSONObject ownerObj;

              // Generate response
              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");

              for (int i = 0; i < array.size() - 1; i++) {
                tempObj = (JSONObject) array.get(i);
                ownerObj = (JSONObject) tempObj.get("owner");
                String ownerLogin = (String) ownerObj.get("login");

                builder.append("Repo full name: " + tempObj.get("full_name") + "<br>");
                builder.append("Repo ID: " + tempObj.get("id") + "<br>");
                builder.append("Owner login: " + ownerLogin + "<br><br>");
              }
            } catch (ParseException e) {
              // Generate response
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Correct format is  /github?query=users/amehlhase316/repos. <br>");
              builder.append("Replace amehlase316 with whichever username you want to see");

              System.out.println("Position: " + e.getPosition());
            } catch (NullPointerException e) {
              builder.setLength(0);
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Bad API call. Correct format is  /github?query=users/amehlhase316/repos. <br>");
              builder.append("Replace amehlase316 with whichever username you want to see");
            }
          }

        } else if (request.contains("recipe?")) {
          if (request.equals("recipe?")) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Correct format is  /recipe?query=banana&number=2. <br>");
          } else {
            try {
              Map<String, String> query_pairs = new LinkedHashMap<String, String>();
              query_pairs = splitQuery(request.replace("recipe?", ""));

              System.out.println(query_pairs);
              JSONParser parser = new JSONParser();
//              String json = fetchURL("https://api.spoonacular.com/food/ingredients/search?query=" +
//                      query_pairs.get("query") + "&number=" + query_pairs.get("number") +
//                      "&apiKey=de6bed90f6d246928993a6fb241e6462");
              String json = fetchURL("https://api.spoonacular.com/recipes/complexSearch?query=" +
                      query_pairs.get("query") + "&number=" + query_pairs.get("number") +
                      "&apiKey=de6bed90f6d246928993a6fb241e6462");
              System.out.println(json);

              Object recipeList = parser.parse(json);
              JSONObject resultsObject = (JSONObject) recipeList;
              System.out.println(resultsObject);
              JSONArray array = (JSONArray) resultsObject.get("results");
              System.out.println(array);
              JSONObject tempObj;
              System.out.println("size " + array.size());
              //System.out.println("Array first " + array.get(0));
              //System.out.println("Array second " + array.get(1));
              // Generate response

              if (array.size() > 0) {
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                for (int i = 0; i < array.size(); i++) {
                  tempObj = (JSONObject) array.get(i);
                  builder.append("Idea #" + (i + 1) + ": ");
                 // builder.append(tempObj.get("name"));
                  builder.append(tempObj.get("title"));
                  builder.append("<br><img src=\"" + tempObj.get("image") + "\"</img><br>");
                  builder.append("<br><br>");
                }
                builder.append("<br> Recipes and images from Spoontacular");
              } else {
                builder.append("HTTP/1.1 400 Bad Request\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("No results found! Try a different ingredient. <br>");
              }

            } catch (ParseException e) {
              // Generate response
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Bad API call. Correct format is  recipe?query=banana&number=2 <br>");

              System.out.println("Position: " + e.getPosition());
            } catch (NullPointerException e) {
              builder.setLength(0);
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Bad API call. Correct format is  recipe?query=banana&number=2 <br>");
            }
          }

        } else if (request.contains("sign?")) {
          Integer month = 0;
          Integer day = 0;
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          if (request.equals("sign?")) {
            // Generate response
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must provide birth day and month");
          } else {
            try {
              query_pairs = splitQuery(request.replace("sign?", ""));
              if (query_pairs.size() != 2) {
                builder.setLength(0);
                builder.append("HTTP/1.1 414 Bad Request\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("Enter ONLY a month and day! PLEASE!");
              } else {
                try {
                  month = Integer.parseInt(query_pairs.get("month"));
                  day = Integer.parseInt(query_pairs.get("day"));
                  String sign;

                  switch (month) {
                    case 1:
                      if (day <= 19) {
                        sign = "Capricorn";
                      } else {
                        sign = "Aquarius";
                      }
                      break;
                    case 2:
                      if (day <= 18) {
                        sign = "Aquarius";
                      } else {
                        sign = "Pisces";
                      }
                      break;
                    case 3:
                      if (day <= 20) {
                        sign = "Pisces";
                      } else {
                        sign = "Aries";
                      }
                      break;
                    case 4:
                      if (day <= 19) {
                        sign = "Aries";
                      } else {
                        sign = "Taurus";
                      }
                      break;
                    case 5:
                      if (day <= 20) {
                        sign = "Taurus";
                      } else {
                        sign = "Gemini";
                      }
                      break;
                    case 6:
                      if (day <= 20) {
                        sign = "Gemini";
                      } else {
                        sign = "Cancer";
                      }
                      break;
                    case 7:
                      if (day <= 22) {
                        sign = "Cancer";
                      } else {
                        sign = "Leo";
                      }
                      break;
                    case 8:
                      if (day <= 22) {
                        sign = "Leo";
                      } else {
                        sign = "Virgo";
                      }
                      break;
                    case 9:
                      if (day <= 22) {
                        sign = "Virgo";
                      } else {
                        sign = "Libra";
                      }
                      break;
                    case 10:
                      if (day <= 22) {
                        sign = "Libra";
                      } else {
                        sign = "Scorpio";
                      }
                      break;
                    case 11:
                      if (day <= 21) {
                        sign = "Scorpio";
                      } else {
                        sign = "Sagittarius";
                      }
                      break;
                    case 12:
                      if (day <= 21) {
                        sign = "Sagittarius";
                      } else {
                        sign = "Capricorn";
                      }
                      break;
                    default:
                      System.out.println("M:" + month +" d: " + day);
                      sign = "Enter a real date!";
                  }

                  // Generate response
                  builder.append("HTTP/1.1 200 OK\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("Your sign is: " + sign);
                } catch (NumberFormatException e) {
                  // Generate response
                  builder.append("HTTP/1.1 400 Bad Request\n");
                  builder.append("Content-Type: text/html; charset=utf-8\n");
                  builder.append("\n");
                  builder.append("GET request parameters must be numbers. <br>");
                  builder.append("Proper format is /sign?month=3&day=14 <br>");
                }
              }
            } catch (StringIndexOutOfBoundsException e) {
              builder.append("HTTP/1.1 400 Bad Request\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("Improper syntax. <br>");
              builder.append("Proper format is /sign?month=3&day=14 <br>");
            }
          }
        } else {
          // if the request is not recognized at all

          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException, StringIndexOutOfBoundsException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }
}
