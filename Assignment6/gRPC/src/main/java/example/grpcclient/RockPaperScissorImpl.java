package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;

public class RockPaperScissorImpl extends RockPaperScissorsGrpc.RockPaperScissorsImplBase {

    public RockPaperScissorImpl() {
        // constructor
    }

    public void play(PlayReq req, StreamObserver<PlayRes> responseObserver) {

    }

    public void leaderboard(Empty req, StreamObserver<LeaderboardRes> responseObserver) {

    }
}
