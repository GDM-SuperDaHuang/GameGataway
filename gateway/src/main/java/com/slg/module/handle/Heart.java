package com.slg.module.handle;
import com.slg.module.annotation.ToMethod;
import com.slg.module.annotation.ToServer;
import com.slg.module.connection.ClientChannelManage;
import com.slg.module.message.MsgResponse;
import com.slg.module.util.SystemTimeCache;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;

import static message.Heart.*;
//心跳
@ToServer
public class Heart {


    //心跳
    @ToMethod(value = 11)
    public MsgResponse HeartHandle(ChannelHandlerContext ctx, HeartReq req, long userId) throws IOException, InterruptedException {
        long now = SystemTimeCache.currentTimeMillis();
        ClientChannelManage.getInstance().updateHearTime(userId, now);
        HeartResp.Builder builder = HeartResp.newBuilder();
        MsgResponse msgResponse = MsgResponse.newInstance(builder);
        return msgResponse;
    }
}
