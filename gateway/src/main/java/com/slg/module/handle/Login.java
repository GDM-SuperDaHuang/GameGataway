package com.slg.module.handle;

import com.google.protobuf.ByteString;
import com.slg.module.annotation.ToMethod;
import com.slg.module.annotation.ToServer;
import com.slg.module.connection.ClientChannelManage;
import com.slg.module.connection.DHKeyInfo;
import com.slg.module.message.ErrorCodeConstants;
import com.slg.module.message.MsgResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import static message.Login.*;
import static message.Account.*;

//用户登录
@ToServer
public class Login {
    public static String testMsg = "123456";

    // 密钥交换
    @ToMethod(value = 2)
    public MsgResponse keyExchangeHandle(ChannelHandlerContext ctx, KeyExchangeReq request, Long userId) {
        BigInteger g = new BigInteger(request.getG().toByteArray());
        BigInteger p = new BigInteger(request.getP().toByteArray());
        BigInteger clientPublicKey = new BigInteger(request.getPublicKey().toByteArray());
        // 1. 选择私钥 b (随机数，1 < b < p-1)
        SecureRandom random = new SecureRandom();
        BigInteger b;
        do {
            b = new BigInteger(p.bitLength() - 1, random);
        } while (b.compareTo(BigInteger.ONE) <= 0 || b.compareTo(p.subtract(BigInteger.ONE)) >= 0);

        // 2. 计算公钥 B = g^b mod p
        BigInteger B = g.modPow(b, p);
        // 3. 计算共享密钥 K = A^b mod p
        BigInteger K = clientPublicKey.modPow(b, p);

        //保存密钥信息
//        SocketAddress socketAddress = ctx.channel().remoteAddress();
//        String ip = ((InetSocketAddress) socketAddress).getHostString();
        ClientChannelManage.getInstance().putCipher(ctx.channel().id(), new DHKeyInfo(b, B, K));

        ByteString serverPublicKey = ByteString.copyFrom(B.toByteArray());
        int i = serverPublicKey.hashCode();
        KeyExchangeResp.Builder builder = KeyExchangeResp.newBuilder()
                .setPublicKey(serverPublicKey);

        //对象池
//        KeyExchangeResp.Builder builder = Pools.KeyExchangeRespPOOL.borrow()
//                .setPublicKey(serverPublicKey);
        MsgResponse msgResponse = MsgResponse.newInstance(builder);
//        Pools.KeyExchangeRespPOOL.release(builder);
        return msgResponse;
    }

    // 密钥验证
    @ToMethod(value = 3)//todo
    public MsgResponse keyVerificationHandle(ChannelHandlerContext ctx, KeyVerificationReq request, Long userId) throws Exception {
        DHKeyInfo keyInfo = ClientChannelManage.getInstance().getCipher(ctx.channel().id());
        if (keyInfo == null) {
            return MsgResponse.newInstance(ErrorCodeConstants.INVALID_PARAMETER);
        }

        ByteString testMessage = request.getTestMessage();
        // 3. 获取约定的固定测试消息 (实际应用中应该从配置或常量获取)

        String string = testMessage.toString();
//        System.out.println("Decrypted test message: " + string);
        // 6. 比较两个加密结果
        // 7. 构建响应

        //
        String respMsg = "啦啦啦啦啦";
        ByteBuf byteBuf = Unpooled.wrappedBuffer(respMsg.getBytes());
        ByteString respMsg00 = ByteString.copyFrom(byteBuf.nioBuffer());
        KeyVerificationResp.Builder builder = KeyVerificationResp.newBuilder()
                .setSuccess(true)
                .setEncryptedEcho(respMsg00);
//                .setErrorMessage(testMsg ? "验证成功" : "验证失败");
        MsgResponse msgResponse = MsgResponse.newInstance(builder, false);
        return msgResponse;
    }

    //登录
    @ToMethod(value = 4)
    public MsgResponse loginHandle(ChannelHandlerContext ctx, LoginReq request, Long userId) throws IOException, InterruptedException {
        Long userid = Long.valueOf(request.getAccount());
        userid = 123L;
        ClientChannelManage.getInstance().put(ctx.channel(), userid);
        LoginResp.Builder builder = LoginResp.newBuilder()
                .setPlayerId("1112223334445556667778889991111111111111111111111111111111111111111111111111111111111111111111111111111L");
//        LoginResp.Builder builder = Pools.LoginRespPOOL.borrow()
//                .setPlayerId("1111L");
        MsgResponse msgResponse = MsgResponse.newInstance(builder);
//        Pools.LoginRespPOOL.release(builder);
        return msgResponse;
    }
}
