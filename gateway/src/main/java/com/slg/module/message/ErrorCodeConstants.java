package com.slg.module.message;

//错误码
public final class ErrorCodeConstants {
    // 防止实例化该类
    private ErrorCodeConstants() {
    }

    // 通用成功码
    public static final int SUCCESS = 0;

    // 通用错误码
    public static final int SERIALIZATION_METHOD_LACK = 1;//序列化方法缺失
    public static final int ESTABLISH_CONNECTION_FAILED = 2;//建立连接失败
    public static final int GATE_FORWARDING_FAILED = 3;//网关转发失败

    // 数据库相关错误码
    public static final int DATABASE_CONNECTION_ERROR = 1100;
    public static final int DATABASE_QUERY_ERROR = 1101;

    // 用户认证相关错误码
    public static final int AUTHENTICATION_FAILED = 1200;
    public static final int TOKEN_EXPIRED = 1201;

    // 参数验证相关错误码
    public static final int INVALID_PARAMETER = 1300;
    public static final int MISSING_PARAMETER = 1301;

} 