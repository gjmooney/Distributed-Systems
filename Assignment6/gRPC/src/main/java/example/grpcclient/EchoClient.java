package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import service.*;

/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class EchoClient {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RockPaperScissorsGrpc.RockPaperScissorsBlockingStub blockingStub4;
  private final TimerGrpc.TimerBlockingStub blockingStub5;
  private final CaesarGrpc.CaesarBlockingStub blockingStub6;

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
    blockingStub5 = TimerGrpc.newBlockingStub(channel);
    blockingStub6 = CaesarGrpc.newBlockingStub(channel);

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
    boolean keepPlaying = true;

    System.out.println("Soooo, what's your name?");
    String name = input.nextLine();
    System.out.println("\nGREETINGS " + name);
    System.out.println("\nWELCOME TO ROCK, PAPER, SCISSORS!!!");


    do {
      int choice = rpsMenu();

      PlayReq.Played weapon = null;
      switch (choice) {
        case 1:
          weapon = PlayReq.Played.ROCK;
          break;
        case 2:
          weapon = PlayReq.Played.PAPER;
          break;
        case 3:
          weapon = PlayReq.Played.SCISSORS;
          break;
        case 4:
          System.out.println("OKAY!");
          break;
        case 5:
          System.out.println("FAREWELL COWARD!");
          keepPlaying = false;
          break;
        default:
          System.out.println("CURSES! HOW DID THIS HAPPEN!?");
          break;
      }

      if (choice == 1 || choice == 2 || choice == 3) {
        playRequest(name, weapon);

      } else if (choice == 4) {
        leaderboardRequest();
      }


    } while (keepPlaying);
  }

  public void playRequest(String name, PlayReq.Played weapon) {
    PlayReq playreq = PlayReq.newBuilder()
            .setName(name)
            .setPlay(weapon)
            .build();

    PlayRes response;
    try {
      response = blockingStub4.play(playreq);
      if (response.getIsSuccess()) {
        System.out.println(response.getMessage());
      } else {
        System.out.println(response.getError());
      }
    } catch (Exception e ) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void leaderboardRequest() {
    Empty lbRequest = Empty.newBuilder().build();

    try {
      LeaderboardRes lbResponse = blockingStub4.leaderboard(lbRequest);

      if (lbResponse.getIsSuccess()) {
        System.out.println("LEADERBOARD");
        System.out.println("-----------");
        System.out.println("PLAYER --- WINS --- LOSSES");
        for (LeaderboardEntry entry : lbResponse.getLeaderboardList()) {
          System.out.println(entry.getName() + " -- " + entry.getWins()
                  + " -- " + entry.getLost());
        }
        System.out.println();
      } else {
        System.out.println(lbResponse.getError());
      }

    } catch (Exception e ) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public int rpsMenu() {
    Scanner input = new Scanner(System.in);
    boolean done = false;
    int choice = 0;
    int menuNum = 1;

    do {
      System.out.println("CHOOSE YOUR MEANS OF DESTRUCTION!!!");
      System.out.println(menuNum + ". ROCK!");
      System.out.println(++menuNum + ". PAPER!");
      System.out.println(++menuNum + ". SCISSORS!");
      System.out.println(++menuNum + ". See the leaderboard!");
      System.out.println(++menuNum + ". FLEE!");
      System.out.println();

      try {
        choice = input.nextInt();
        if (choice >= 1 && choice <= menuNum) {
          done = true;

        } else {
          System.out.println("CHOOSE WISELY!!!");
        }

      } catch (InputMismatchException e) {
        System.out.println("YOU FOOL!!!");
        input = new Scanner(System.in);
      }
    } while (!done);

    return choice;
  }

  public void startTimers() {
    boolean keepGoing = true;

    System.out.println("\nHello!");
    System.out.println("\nWelcome to Timer World");

    do {
      int choice = timerMenu();

      switch (choice) {
        case 1:
          startTimer();
          break;
        case 2:
          checkTimer();
          break;
        case 3:
          stopTimer();
          break;
        case 4:
          listTimers();
          break;
        case 5:
          System.out.println("Goodbye!");
          keepGoing = false;
          break;
        default:
          System.out.println("Timer world has been attacked");
          break;
      }
    } while (keepGoing);
  }

  public void startTimer() {
    Scanner input = new Scanner(System.in);
    System.out.println("\nWhat would you like to name this timer?");
    String name = input.nextLine();
    TimerRequest request = TimerRequest.newBuilder().setName(name).build();

    TimerResponse response;
    try {
      response = blockingStub5.start(request);
      if (response.getIsSuccess()) {
        System.out.println("Timer " + response.getTimer().getName() + " has been started");
        System.out.println();
      } else {
        System.out.println();
        System.out.println(response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
    }
  }

  public void checkTimer() {
    Scanner input = new Scanner(System.in);
    System.out.println("\nPlease enter the name of the timer you would like to check");
    String name = input.nextLine();
    TimerRequest request = TimerRequest.newBuilder().setName(name).build();

    TimerResponse response;
    try {
      response = blockingStub5.check(request);
      if (response.getIsSuccess()) {
        System.out.println(response.getTimer().getSecondsPassed() + " seconds have passed since " +
                "timer " + response.getTimer().getName() + " was started.");
        System.out.println();
      } else {
        System.out.println();
        System.out.println(response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
    }
  }

  public void stopTimer() {
    Scanner input = new Scanner(System.in);
    System.out.println("\nPlease enter the name of the timer you would like to stop");
    String name = input.nextLine();
    TimerRequest request = TimerRequest.newBuilder().setName(name).build();

    TimerResponse response;
    try {
      response = blockingStub5.close(request);
      if (response.getIsSuccess()) {
        System.out.println("Timer " + name + " has been stopped");
        System.out.println();
      } else {
        System.out.println();
        System.out.println(response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
    }
  }

  public void listTimers() {
    Empty request = Empty.newBuilder().build();

    TimerList response;
    try {
      response = blockingStub5.list(request);
      System.out.println("Here's the list of active timers");
      System.out.println("--------------------------------");
      for (Time timer : response.getTimersList()) {
        System.out.println(timer.getSecondsPassed() + " seconds have passed since " +
                "timer " + timer.getName() + " was started.");
      }
      System.out.println();
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
    }

  }

  public int timerMenu() {
    Scanner input = new Scanner(System.in);
    boolean done = false;
    int choice = 0;

    String error = "Please select a valid option";

    do {
      int menuNum = 1;
      System.out.println("Please make a selection");
      System.out.println(menuNum + ". Start a timer!");
      System.out.println(++menuNum + ". Check a timer!");
      System.out.println(++menuNum + ". Stop a timer!");
      System.out.println(++menuNum + ". See all the timers!");
      System.out.println(++menuNum + ". Return to Services menu!");
      System.out.println();

      try {
        choice = input.nextInt();
        if (choice >= 1 && choice <= menuNum) {
          done = true;

        } else {
          System.out.println(error);
        }

      } catch (InputMismatchException e) {
        System.out.println(error);
        input = new Scanner(System.in);
      }
    } while (!done);

    return choice;
  }

  public void startCaesar() {
    boolean keepGoing = true;

    System.out.println("\nHello!");
    System.out.println("\nWelcome to Caesar Cipher World");

    do {
      int choice = caesarMenu();

      switch (choice) {
        case 1:
          caesar(true);
          break;
        case 2:
          caesar(false);
          break;
        case 3:
          listCiphers();
          break;
        case 4:
          System.out.println("Goodbye!");
          keepGoing = false;
          break;
        default:
          System.out.println("Et tu, Brute?");
          break;
      }
    } while (keepGoing);
  }

  public int caesarMenu() {
    Scanner input = new Scanner(System.in);
    boolean done = false;
    int choice = 0;

    String error = "Please select a valid option";

    do {
      int menuNum = 1;
      System.out.println("Please make a selection");
      System.out.println(menuNum + ". Encrypt a message!");
      System.out.println(++menuNum + ". Decrypt a message!");
      System.out.println(++menuNum + ". See all the encrypted messages!");
      System.out.println(++menuNum + ". Return to Services menu!");
      System.out.println();

      try {
        choice = input.nextInt();
        if (choice >= 1 && choice <= menuNum) {
          done = true;

        } else {
          System.out.println(error);
        }

      } catch (InputMismatchException e) {
        System.out.println(error);
        input = new Scanner(System.in);
      }
    } while (!done);

    return choice;
  }

  public void caesar(boolean encrypt) {
    String option;
    if (encrypt) {
      option = "encrypt";
    } else {
      option = "decrypt";
    }
    System.out.println("Please enter the message you would like to " + option);
    Scanner input = new Scanner(System.in);
    String message = input.nextLine();
    boolean done = false;
    int key = 0;

    String error = "Positive means more than 0, or 0, but that won't do anything";
    do {
      System.out.println();
      System.out.println("Please enter the encryption key you would like to use.");
      System.out.println("Positive numbers only PLEASE!");

      try {
        key = input.nextInt();
        if (key >= 0) {
          done = true;

        } else {
          System.out.println(error);
        }

      } catch (InputMismatchException e) {
        System.out.println(error);
        input = new Scanner(System.in);
      }
    } while (!done);

    MessageReq request = MessageReq.newBuilder()
            .setMessage(message)
            .setKey(key)
            .build();

    MessageRes response = null;
    try {
      if (encrypt) {
        response = blockingStub6.encrypt(request);
      } else {
        response = blockingStub6.decrypt(request);
      }
      if (response.getOk()) {
        System.out.println(response.getMessage());
        System.out.println();
      } else {
        System.out.println();
        System.out.println(response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
    }

    System.out.println("The " + option + "ed message:");
    System.out.println(response.getMessage());
    System.out.println();
  }

  public void listCiphers() {
    Empty request = Empty.newBuilder().build();

    MessageList response;
    try {
      response = blockingStub6.listMessages(request);
      System.out.println("Here's the list of encrypted messages and their cipher keys");
      System.out.println("--------------------------------");
      for (CaesarMessage message : response.getMessageList()) {
        System.out.println(message.getMessage() + " : " + message.getKey());
      }
      System.out.println();
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
    }
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
        System.out.println("\nWhat would you like to do?");
        System.out.println("1. Hear a joke");
        System.out.println("2. Play Rock, Paper, Scissors");
        System.out.println("3. Play with timers");
        System.out.println("4. Play with a Caesar Cipher");
        System.out.println("5. Quit");
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
              client.startTimers();
              break;
            case 4:
              client.startCaesar();
              break;
            case 5:
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
