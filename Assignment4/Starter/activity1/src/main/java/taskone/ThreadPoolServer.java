/**
  File: ThreadPoolServer.java
  Author: Student in Fall 2020B
  Description: ThreadPoolServer class in package taskone.
*/

package taskone;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Class: ThreadPoolServer
 * Description: ThreadPoolServer tasks.
 */
class ThreadPoolServer {

    public static void main(String[] args) throws Exception {
        int port;
        StringList strings = new StringList();
        Socket sock = null;
        int id = 0;
        int poolSize;


        if (args.length != 2) {
            // gradle runServer -Pport=9099 -q --console=plain
            System.out.println("Usage: gradle runServer -Pport=9099 -Pthreads=5 -q --console=plain");
            System.exit(1);
        }
        port = -1;
        poolSize = -1;
        try {
            port = Integer.parseInt(args[0]);
            poolSize = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|threads] must be an integer");
            System.exit(2);
        }
        ServerSocket server = new ServerSocket(port);
        Executor pool = Executors.newFixedThreadPool(poolSize);
        System.out.println("ThreadPoolServer Started...");
        try {
            while (true) {
                System.out.println("Accepting a Request...");
                sock = server.accept();
                Performer performer = new Performer(sock, strings);
                pool.execute(performer);
                System.out.println("Connected to client - " + id++);

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
