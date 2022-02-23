package example.grpcclient;

import io.grpc.stub.StreamObserver;
import org.json.JSONObject;
import service.*;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Iterator;

public class TimerImpl extends TimerGrpc.TimerImplBase{
    JSONObject timerList;

    public TimerImpl() {
        this.timerList = new JSONObject();
    }

    public void start(TimerRequest req, StreamObserver<TimerResponse> responseObserver) {
        TimerResponse response;
        if (!timerList.has(req.getName())) {
            LocalTime timerStart = LocalTime.now();
            timerList.put(req.getName(), timerStart);
            Time timer = Time.newBuilder().setName(req.getName()).build();

            response = TimerResponse.newBuilder()
                    .setIsSuccess(true)
                    .setTimer(timer)
                    .build();

        } else {
            response = TimerResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("A timer with that name already exists")
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void check(TimerRequest req, StreamObserver<TimerResponse> responseObserver) {
        TimerResponse response;
        if (timerList.has(req.getName())) {
            LocalTime now = LocalTime.now();
            Duration dif = Duration.between((Temporal) timerList.get(req.getName()), now);
            long diff = dif.toSeconds();

            Time timer = Time.newBuilder()
                    .setName(req.getName())
                    .setSecondsPassed(diff)
                    .build();

            response = TimerResponse.newBuilder()
                    .setIsSuccess(true)
                    .setTimer(timer)
                    .build();

        } else {
            response = TimerResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("There is no timer with that name")
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    public void close(TimerRequest req, StreamObserver<TimerResponse> responseObserver) {
        TimerResponse response;
        if (timerList.has(req.getName())) {
            timerList.remove(req.getName());

            response = TimerResponse.newBuilder()
                    .setIsSuccess(true)
                    .build();

        } else {
            response = TimerResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("There is no timer with that name")
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    public void list(Empty req, StreamObserver<TimerList> responseObserver) {
        TimerList.Builder res = TimerList.newBuilder();
        Iterator<String> timers = timerList.keys();

        while (timers.hasNext()) {
            String key = timers.next();
            LocalTime now = LocalTime.now();
            Duration dif = Duration.between((Temporal) timerList.get(key), now);
            long diff = dif.toSeconds();
            Time timer = Time.newBuilder()
                    .setName(key)
                    .setSecondsPassed(diff)
                    .build();
            res.addTimers(timer);
        }

        TimerList response = res.build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
