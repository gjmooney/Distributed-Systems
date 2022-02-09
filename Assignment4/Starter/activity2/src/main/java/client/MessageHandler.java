package client;

import buffers.ResponseProtos.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler extends Thread{
    private Response response;
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private final InputStream in;

    public MessageHandler(InputStream in) {
        this.in = in;
    }

    @Override
     public void run() {
        while (keepRunning.get()) {
            try {
                response = Response.parseDelimitedFrom(in);
                if (response.getResponseType() == Response.ResponseType.WON
                    || response.getResponseType() == Response.ResponseType.BYE) {
                    keepRunning.set(false);
                }
            } catch (IOException e) {
                System.out.println("MessageHandler");
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println("Lost server");
                keepRunning.set(false);
                System.exit(2);
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
