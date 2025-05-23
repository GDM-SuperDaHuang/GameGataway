package com.slg.module.handle;

import com.google.protobuf.ByteString;
import com.slg.module.annotation.ToMethod;
import com.slg.module.annotation.ToServer;
import com.slg.module.connection.ClientChannelManage;
import com.slg.module.connection.DHKeyInfo;
import com.slg.module.message.ErrorCodeConstants;
import com.slg.module.message.MsgResponse;
import com.slg.module.util.Pools;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;


import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import static message.Login.*;
import static message.Account.*;

//用户登录
@ToServer
public class Login {
    @Autowired
    private ClientChannelManage clientchannelManage;

    // 密钥交换
    @ToMethod(value = 3)
    public MsgResponse keyExchangeHandle(ChannelHandlerContext ctx, KeyExchangeReq request, KeyExchangeResp resp,long userId) {
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
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        String ip = ((InetSocketAddress) socketAddress).getHostString();
        clientchannelManage.putCipher(ip, new DHKeyInfo(b, B, K));

        ByteString serverPublicKey = ByteString.copyFrom(B.toByteArray());

//        KeyExchangeResp.Builder builder = KeyExchangeResp.newBuilder()
//                .setPublicKey(serverPublicKey);

        //对象池
        KeyExchangeResp.Builder builder = Pools.KeyExchangeRespPOOL.borrow()
                .setPublicKey(serverPublicKey);


        MsgResponse msgResponse = new MsgResponse();
        msgResponse.setBody(builder);
        return msgResponse;
    }

    // 密钥验证
    @ToMethod(value = 4)//todo
    public MsgResponse keyExchangeHandle2(ChannelHandlerContext ctx, KeyVerificationReq request, long userId) throws Exception {
        MsgResponse msgResponse = new MsgResponse();
        DHKeyInfo keyInfo = clientchannelManage.getCipher("");
        if (keyInfo == null) {
            msgResponse.setErrorCode(ErrorCodeConstants.INVALID_PARAMETER);
            return msgResponse;
        }
        // 3. 获取约定的固定测试消息 (实际应用中应该从配置或常量获取)
        byte[] fixedMessageBytes = DHKeyInfo.testMsg.getBytes(StandardCharsets.UTF_8);

        // 4. 使用共享密钥加密固定消息（服务器端）
        // 4.1 派生加密密钥
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(keyInfo.getSharedKey().toByteArray());
        SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

        // 4.2 使用AES加密
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12]; // 实际应用中应该使用随机IV
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
        byte[] serverEncrypted = cipher.doFinal(fixedMessageBytes);

        // 5. 获取客户端加密的消息
        byte[] clientEncrypted = request.getTestMessage().toByteArray();

        // 6. 比较两个加密结果
        boolean success = MessageDigest.isEqual(serverEncrypted, clientEncrypted);
        ByteString serverEncrypteds = ByteString.copyFrom(serverEncrypted);
        // 7. 构建响应
        KeyVerificationResp.Builder builder = KeyVerificationResp.newBuilder()
                .setSuccess(success)
                .setEncryptedEcho(serverEncrypteds)
                .setErrorMessage(success ? "验证成功" : "验证失败");

        msgResponse.setBody(builder);
        return msgResponse;
    }

    //登录
    @ToMethod(value = 10)
    public MsgResponse loginHandle(ChannelHandlerContext ctx, LoginReq request, long userId) throws IOException, InterruptedException {
        clientchannelManage.put(ctx.channel(), 122111L);

        LoginResponse.Builder builder = LoginResponse.newBuilder()
                .setAaa(999999999)
                .setBbb(777777777);
        MsgResponse msgResponse = new MsgResponse();
        msgResponse.setBody(builder);
        msgResponse.setErrorCode(0);
        return msgResponse;
    }
}
