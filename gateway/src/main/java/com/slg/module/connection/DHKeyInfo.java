package com.slg.module.connection;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;

public class DHKeyInfo {
    //测试消息
    private BigInteger privateKey;  // 服务器私钥
    private BigInteger publicKey;   // 服务器公钥
    private BigInteger sharedKey;   // 共享密钥

    public DHKeyInfo(BigInteger privateKey, BigInteger publicKey, BigInteger sharedKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.sharedKey = sharedKey;
    }

    public BigInteger getPrivateKey() {
        return privateKey;
    }

    public BigInteger getPublicKey() {
        return publicKey;
    }

    public BigInteger getSharedKey() {
        return sharedKey;
    }
}
