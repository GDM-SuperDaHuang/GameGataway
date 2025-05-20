package com.slg.module.handle;

import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.HKDFParameters;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class HKDFMsssage {
    // 假设通过 DH 交换得到的共享密钥 K（示例用随机值模拟）
    public static void main(String[] args) {
        int k = 32;
        // 假设通过 DH 交换得到的共享密钥 K（示例用随机值模拟）
        byte[] sharedSecretK = new byte[k]; // 长度取决于 DH 算法
        new SecureRandom().nextBytes(sharedSecretK);

        // 从 K 派生加密密钥和 MAC 密钥
        byte[] encKey = deriveKeyHKDF(sharedSecretK, "AES_ENC_KEY", 32); // AES-256
        byte[] macKey = deriveKeyHKDF(sharedSecretK, "HMAC_KEY", 32);    // HMAC-SHA256

        // 转换为 Java 密钥对象
        SecretKeySpec aesKey = new SecretKeySpec(encKey, "AES");
        SecretKeySpec hmacKey = new SecretKeySpec(macKey, "HmacSHA256");

        System.out.println("AES 密钥派生成功: " + bytesToHex(encKey));
        System.out.println("HMAC 密钥派生成功: " + bytesToHex(macKey));
    }

    /**
     * 使用 HKDF 从共享密钥派生安全密钥
     * @param sharedSecret DH 共享密钥 K
     * @param context 上下文信息（区分密钥用途）
     * @param keyLength 所需密钥长度（字节）
     */
    public static byte[] deriveKeyHKDF(byte[] sharedSecret, String context, int keyLength) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(
                sharedSecret,               // 原始密钥
                null,                       // Salt（可选，可为 null）
                context.getBytes()          // 上下文信息
        ));
        byte[] derivedKey = new byte[keyLength];
        hkdf.generateBytes(derivedKey, 0, derivedKey.length);
        return derivedKey;
    }

    // 辅助方法：字节数组转十六进制
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
