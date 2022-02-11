/**
  File: ThreadedServer.java
  Author: Student in Fall 2020B
  Description: ThreadedServer class in package taskone.
*/

package taskone;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Class: ThreadedServer
 * Description: ThreadedServer tasks.
 */
class ThreadedServer extends Thread {
    private Socket servSock;
    private int id;
    private static StringList strings = new StringList();

    public ThreadedServer(Socket sock, int id) {
        this.servSock = sock;
        this.id = id;
    }

    synchronized public void run() {
        Performer performer = new Performer(servSock, strings);
        performer.doPerform();
    }

    public static void main(String[] args) throws Exception {
        Socket sock = null;
        int id = 0;
        int port;


        if (args.length != 1) {
            // gradle runServer -Pport=9099 -q --console=plain
            System.out.println("Usage: gradle runServer -Pport=9099 -q --console=plain");
            System.exit(1);
        }
        port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be an integer");
            System.exit(2);
        }
        ServerSocket server = new ServerSocket(port);
        System.out.println("ThreadedServer Started...");
        try {
            while (true) {
                System.out.println("Accepting a Request...");
                sock = server.accept();
                System.out.println("Accepted request on a new thread: Client - " + id++);

                Performer performer = new Performer(sock, strings);
                performer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sock != null) {
                System.out.println("close socket of client ");
                sock.close();
            }
        }

    }
}
