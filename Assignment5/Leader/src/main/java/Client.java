import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

    public void establishConnection() {

    }

    public static void main(String[] args) {
        Socket leaderSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        int port = 8000; // default

        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        try {
            leaderSocket = new Socket(host, port);
            out = new ObjectOutputStream(leaderSocket.getOutputStream());
            in = new ObjectInputStream(leaderSocket.getInputStream());

            int choice;
            Scanner input = new Scanner(System.in);
            do {
                System.out.println();
                System.out.println("Client Menu");
                System.out.println("Please select a valid option (1, 2, or 3).");
                System.out.println("1. Get Credit");
                System.out.println("2. Pay Back Credit");
                System.out.println("3. Quit");
                System.out.println();

                try {
                    choice = input.nextInt();
                    switch (choice) {
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            break;
                    }
                } catch (InputMismatchException e) {
                    input = new Scanner(System.in);
                    System.out.println("Please Enter a valid selection (1, 2, or 3");
                }
            } while (true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
