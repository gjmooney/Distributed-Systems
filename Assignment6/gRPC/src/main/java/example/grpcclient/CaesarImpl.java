package example.grpcclient;

import io.grpc.stub.StreamObserver;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import service.*;

import java.io.*;
import java.util.Iterator;

public class CaesarImpl extends CaesarGrpc.CaesarImplBase {
    JSONObject messageList;

    public CaesarImpl() {
        this.messageList = readMessageList();
    }

    public void encrypt(MessageReq req, StreamObserver<MessageRes> responseObserver) {
        System.out.println("Encrypting message");

        MessageRes response;

        try {
            String encrypted = caesarCipher(req.getMessage(), req.getKey(), true);
            response = MessageRes.newBuilder()
                    .setMessage(encrypted)
                    .setOk(true)
                    .build();
            messageList.put(encrypted, req.getKey());
            saveMessagesList();
        } catch (Exception e) {
            response = MessageRes.newBuilder()
                    .setOk(false)
                    .setError("Unable to decrypt")
                    .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void decrypt(MessageReq req, StreamObserver<MessageRes> responseObserver) {
        System.out.println("Decrypting password");

        MessageRes response;
        try {
            String decrypted = caesarCipher(req.getMessage(), req.getKey(), false);

            response = MessageRes.newBuilder()
                    .setMessage(decrypted)
                    .setOk(true)
                    .build();
        } catch (Exception e) {
            response = MessageRes.newBuilder()
                    .setOk(false)
                    .setError("Unable to decrypt")
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public String caesarCipher(String message, int key, boolean encrypt) {
        int numOffset;
        int letterOffset;
        if (encrypt) {
            letterOffset = key;
            numOffset = key;
        } else {
            letterOffset = 26 - (key % 26);
            numOffset = 10 - (key % 10);
        }
        StringBuilder newMessage = new StringBuilder();
        for (char letter : message.toCharArray()) {
            if (letter >= 'a' && letter <= 'z') {
                newMessage.append((char) ('a' + ((letter - 'a' + letterOffset) % 26)));
            } else if (letter >= 'A' && letter <= 'Z') {
                newMessage.append((char) ('A' + ((letter - 'A' + letterOffset) % 26)));
            } else if (letter >= '0' && letter <= '9') {
                newMessage.append((char) ('0' + ((letter - '0' + numOffset) % 10)));
            } else {
                newMessage.append(letter);
            }
        }

        return newMessage.toString();
    }

    public void listMessages(Empty req, StreamObserver<MessageList> responseObserver) {
        MessageList.Builder res = MessageList.newBuilder();
        Iterator<String> messages = messageList.keys();

        while (messages.hasNext()) {
            String key = messages.next();
            CaesarMessage message = CaesarMessage.newBuilder()
                    .setMessage(key)
                    .setKey(messageList.getInt(key))
                    .build();
            res.addMessage(message);
        }

        MessageList response = res.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public JSONObject readMessageList() {
        BufferedReader messageListReader = null;
        JSONTokener tokener;
        try {
            File file = new File("src/main/resources/messages.txt");

            messageListReader = new BufferedReader(new FileReader(file));
            tokener = new JSONTokener(messageListReader);
            return new JSONObject(tokener);
        } catch (FileNotFoundException e) {
            System.out.println("No messages yet");
            return new JSONObject();
        } catch (JSONException e) {
            System.out.println("Message file is blank");
            return new JSONObject();
        }
        finally {
            if (messageListReader != null) {
                try {
                    messageListReader.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveMessagesList() {
        File file = new File("src/main/resources/messages.txt");
        FileWriter fileWriter = null;
        try {
            if (file.createNewFile()) {
                System.out.println("New message list created");
            }
            fileWriter = new FileWriter("src/main/resources/messages.txt");
            fileWriter.write(messageList.toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Problem saving message list");
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("problem closing message list");
            }
        }
    }
}
