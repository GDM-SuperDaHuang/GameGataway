//package com.slg.module.handle;
//
//import account.Account;
//import com.slg.module.annotation.ToMethod;
//import com.slg.module.annotation.ToServer;
//import com.slg.module.message.MsgResponse;
//import com.slg.module.message.SendMsg;
//import io.netty.channel.ChannelHandlerContext;
//import message.Login;
//import message.Monon;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import static Login;
//
//
//@ToServer
//public class Test {
//    private static final Logger logger = LogManager.getLogger(Test.class);
//    private SendMsg sendMsg;
//
//    public Test(SendMsg sendMsg) {
//        this.sendMsg = sendMsg;
//    }
//
//
//    @ToMethod(value = 1)
//    public MsgResponse diy(ChannelHandlerContext ctx, Login.LoginReq request, long userId) throws IOException, InterruptedException {
//        Monon.messs ss = request.getSs();
//        String data = ss.getData();
//
//        Account.LoginResponse.Builder builder = Account.LoginResponse.newBuilder()
//                .setAaa(999999999)
//                .setBbb(777777777);
////        MSG.LoginRequest.parseFrom()
//        MsgResponse msgResponse = new MsgResponse();
//        msgResponse.setBody(builder);
//        msgResponse.setErrorCode(0);
//        return msgResponse;
//    }
//
//
//    @ToMethod(value = 2)
//    public MsgResponse ffff(ChannelHandlerContext ctx, DIY.FriendRequest request, long userId) throws IOException {
//        ArrayList<Long> longs = new ArrayList<>();
//        longs.add(110L);
//        longs.add(211L);
//        DIY.FriendsResponse.Builder friendsResponse = DIY.FriendsResponse.newBuilder()
//                .addAllUserIdList(longs);
//        MsgResponse msgResponse = new MsgResponse();
//        msgResponse.setBody(friendsResponse);
//        msgResponse.setErrorCode(0);
//        return msgResponse;
//    }
//
//}