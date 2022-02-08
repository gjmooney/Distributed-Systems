package client;

import buffers.ResponseProtos.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler extends Thread{
    private Response response;
    private AtomicBoolean keepRunning = new AtomicBoolean(true);
    private InputStream in;
    int count = 1;

    public MessageHandler(InputStream in) {
        this.in = in;
    }

    @Override
     public void run() {
        while (keepRunning.get()) {
            try {
                System.out.println("handler waiting on response");
                response = Response.parseDelimitedFrom(in);
                if (response.getResponseType() == Response.ResponseType.WON
                    || response.getResponseType() == Response.ResponseType.BYE) {
                    keepRunning.set(false);
                    System.out.println("Set false in handler");
                }
                System.out.println("handler got response " + count++);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void nullOutResponse() {
        response = null;
    }

    public Response getResponse() {
        if (response == null) {
            return null;
        } else  {
            return response;
        }
    }
}
