package com.slg.module.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EightByteSessionIdGenerator {

    public final static Map<Long,Long> sessionIdMap = new ConcurrentHashMap<>();
    public static long getSessionIdByUserId(long userId) {
        return sessionIdMap.get(userId);
    }

    /**
     * 生成一个基于时间戳和用户ID的8字节大小的sessionId
     * @param userId 用户ID
     * @return 生成的sessionId，作为long类型
     */
    public static long generateSessionId(long userId) {
        // 获取当前时间戳（秒）
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        // 这里我们简单地将时间戳的低32位和用户ID的低16位拼接起来（注意：这可能会增加冲突的风险）
        long sessionId = ((currentTimeSeconds & 0xFFFFFFFFL) << 16) | (userId & 0xFFFF);
        sessionIdMap.put(userId,sessionId);
        return sessionId;
    }

    /**
     * 生成一个基于用户ID和时间戳的8字节大小的sessionId
     *
     * @param userId 用户ID（long类型）
     * @param timestamp 时间戳（long类型）
     * @return 生成的sessionId，作为byte数组
     */
    public static byte[] generateSessionId(long userId, long timestamp) {
        try {
            // 将用户ID和时间戳转换为字节数组
            byte[] userIdBytes = longToBytes(userId);
            byte[] timestampBytes = longToBytes(timestamp);

            // 将两者组合成一个字节数组（这里简单地将它们拼接起来）
            byte[] combinedBytes = new byte[userIdBytes.length + timestampBytes.length];
            System.arraycopy(userIdBytes, 0, combinedBytes, 0, userIdBytes.length);
            System.arraycopy(timestampBytes, 0, combinedBytes, userIdBytes.length, timestampBytes.length);

            // 计算哈希值
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(combinedBytes);

            // 截取哈希值的前8个字节
            byte[] sessionId = new byte[8];
            System.arraycopy(hashBytes, 0, sessionId, 0, 8);

            return sessionId;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    // 辅助方法：将long转换为byte数组
    private static byte[] longToBytes(long x) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) ((x >> (56 - 8 * i)) & 0xff);
        }
        return bytes;
    }

//    public static void main(String[] args) {
//        long userId = 1234567890123456789L;
//        long timestamp = System.currentTimeMillis();
//
//        byte[] sessionId = generateSessionId(userId, timestamp);
//
//        // 打印sessionId的十六进制表示
//        for (byte b : sessionId) {
//            System.out.format("%02X ", b);
//        }
//    }
}