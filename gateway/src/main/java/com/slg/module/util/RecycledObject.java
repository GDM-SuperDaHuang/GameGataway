package com.slg.module.util;

import com.slg.module.message.MsgResponse;
import io.netty.util.Recycler;

public class RecycledObject {
    // 1. 定义 Recycler 和 Handle
    private static final Recycler<RecycledObject> RECYCLER = new Recycler<RecycledObject>() {
        @Override
        protected RecycledObject newObject(Handle<RecycledObject> handle) {
            return new RecycledObject(handle);
        }
    };

    private final Recycler.Handle<RecycledObject> handle;
//    private String data;
    private MsgResponse data;
//    private String data;


    // 2. 私有构造，通过 Handle 管理生命周期
    private RecycledObject(Recycler.Handle<RecycledObject> handle) {
        this.handle = handle;
    }

    // 3. 静态方法获取对象（从池中借用）
    public static RecycledObject newInstance() {
        return RECYCLER.get();
    }

    // 4. 归还对象到池
    public void recycle() {
        data = null; // 必须重置对象状态！
        handle.recycle(this);
    }

    // 业务方法
    public void setData(MsgResponse data) {
        this.data = data;
    }

    public MsgResponse getData() {
        return data;
    }
}
