package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import service.*;
import test.TestProtobuf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class EchoClient {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RockPaperScissorsGrpc.RockPaperScissorsBlockingStub blockingStub4;

  /** Construct client for accessing server using the existing channel. */
  public EchoClient(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RockPaperScissorsGrpc.newBlockingStub(channel);
  }

  public void askServerToParrot(String message) {
    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void startRps() {
    Scanner input = new Scanner(System.in);

    System.out.println("Soooo, what's your name?");
    String name = input.nextLine();

    do {
      System.out.println("GREETINGS " + name);
      System.out.println("WELCOME TO ROCK, PAPER, SCISSORS!!!");
      System.out.println("CHOOSE YOUR MEANS OF DESTRUCTION!!!");
      System.out.println("1. ROCK!");
      System.out.println("2. PAPER!");
      System.out.println("3. SCISSORS!");
      System.out.println();

      int choice = 0;
      PlayReq.Played weapon = null;
      try {
        choice = input.nextInt();
        switch (choice) {
          case 1:
            weapon = PlayReq.Played.ROCK;
            break;
          case 2:
            //request.setPlay(PlayReq.Played.PAPER);
            weapon = PlayReq.Played.PAPER;
            break;
          case 3:
            //request.setPlay(PlayReq.Played.SCISSORS);
            weapon = PlayReq.Played.SCISSORS;
            break;
          default:
            System.out.println("YOU FOOL!!!");
            break;
        }

        if (choice == 1 || choice == 2 || choice == 3) {
          PlayReq request = PlayReq.newBuilder()
                  .setName(name)
                  .setPlay(weapon)
                  .build();

          PlayRes response;
          response = blockingStub4.play(request);
        }

      } catch (InputMismatchException e) {
        System.out.println("CHOOSE WISELY!!!");
        input = new Scanner(System.in);
      }
    } while (true);

    //request needs a name and r, p, or s (called play)
    //PlayReq request = PlayReq.newBuilder()

  }

  public int rpsMenu() {
    do {
      System.out.println("GREETINGS " + name);
      System.out.println("WELCOME TO ROCK, PAPER, SCISSORS!!!");
      System.out.println("CHOOSE YOUR MEANS OF DESTRUCTION!!!");
      System.out.println("1. ROCK!");
      System.out.println("2. PAPER!");
      System.out.println("3. SCISSORS!");
      System.out.println();
    } while (true);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 5) {
      System.out
          .println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)>");
      System.exit(1);
    }
    int port = 9099;
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];
    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }

    // Create a communication channel to the server, known as a Channel. Channels
    // are thread-safe
    // and reusable. It is common to create channels at the beginning of your
    // application and reuse
    // them until the application shuts down.
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid
        // needing certificates.
        .usePlaintext().build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget).usePlaintext().build();
    try {

      EchoClient client = new EchoClient(channel, regChannel);

      do {
        System.out.println("What would you like to do?");
        System.out.println("1. Hear a joke");
        System.out.println("2. Play Rock, Paper, Scissors");
        System.out.println("3. Play with timers");
        System.out.println("4. Quit");
        System.out.println("Please enter a valid selection.");
        System.out.println();

        Scanner input = new Scanner(System.in);
        int choice = 0;
        try {
          choice = input.nextInt();

          switch (choice) {
            case 1:
              client.askForJokes(1);
              break;
            case 2:
              client.startRps();
              break;
            case 3:
              break;
            case 4:
              System.out.println("BUH BYE");
              System.exit(0);
              break;
            default:
              System.out.println("The switch broke");
              break;
          }
        } catch (InputMismatchException e) {
          input = new Scanner(System.in);
          System.out.println("Please enter a valid choice");
        }
      } while (true);

    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
