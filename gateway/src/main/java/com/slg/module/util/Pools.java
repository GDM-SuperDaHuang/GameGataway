package com.slg.module.util;

import message.Account;
import org.springframework.stereotype.Component;

/**
 * 对象工厂管理
 */
@Component
public class Pools {
    public static final ProtobufBuilderPool<Account.KeyExchangeResp, Account.KeyExchangeResp.Builder> KeyExchangeRespPOOL = new ProtobufBuilderPool<>(Account.KeyExchangeResp::newBuilder);
}
