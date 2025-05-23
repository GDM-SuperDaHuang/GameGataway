package com.slg.module.handle;
import com.slg.module.annotation.ToMethod;
import com.slg.module.annotation.ToServer;
import com.slg.module.connection.ClientChannelManage;
import com.slg.module.message.Constants;
import com.slg.module.message.MsgResponse;
import com.slg.module.util.SystemTimeCache;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;

import static message.Account.*;
import static message.Login.LoginReq;
//用户登录
@ToServer
public class Heart {
    @Autowired
    private ClientChannelManage clientchannelManage;
    //心跳
    @ToMethod(value = 5)
    public MsgResponse HeartHandle(ChannelHandlerContext ctx, LoginReq request, long userId) throws IOException, InterruptedException {
        long now = SystemTimeCache.currentTimeMillis();
        Long hearTime = clientchannelManage.getHearTime(userId);
        if (hearTime + Constants.HeartTime < now) {
            clientchannelManage.remove(ctx.channel());
        }
        clientchannelManage.updateHearTime(userId, now);
        LoginResponse.Builder builder = LoginResponse.newBuilder();
        MsgResponse msgResponse = MsgResponse.newInstance(builder);
        return msgResponse;
    }
}
