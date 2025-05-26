package com.slg.module.pools;

import com.slg.module.util.ProtobufBuilderPool;
import message.Account;
import message.Login;

public class Pools {
    public static final ProtobufBuilderPool<Account.KeyExchangeResp, Account.KeyExchangeResp.Builder> KeyExchangeRespPOOL = new ProtobufBuilderPool<>(Account.KeyExchangeResp::newBuilder);
    public static final ProtobufBuilderPool<Login.LoginResp, Login.LoginResp.Builder> LoginRespPOOL = new ProtobufBuilderPool<>(Login.LoginResp::newBuilder);

}
