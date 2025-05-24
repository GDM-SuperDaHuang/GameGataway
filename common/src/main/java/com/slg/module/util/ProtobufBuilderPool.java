package com.slg.module.util;

import com.google.protobuf.*;
import io.netty.util.Recycler;

import java.io.IOException;
import java.io.InputStream;

/**
 * Protobuf Builder 对象池实现（基于Netty Recycler）
 *
 *
 * 使用示例：
 * // 初始化
 * ProtobufBuilderPool<MyMessage, MyMessage.Builder> pool =
 *     new ProtobufBuilderPool<>(MyMessage::newBuilder);
 *
 * // 自动管理（推荐）
 * try (var wrapper = pool.borrowAutoCloseable()) {
 *     MyMessage msg = wrapper.get().setField(...).build();
 * }
 *
 *
 *
 *线程安全：
 * 默认基于 Netty 的 Recycler 实现线程局部缓存
 * 禁止跨线程借用/归还（会导致性能下降）
 * 每个线程维护独立的对象池
 *
 * // 错误用法（忘记归还）
 * Message.Builder builder = pool.borrow();
 * builder.build(); // 内存泄漏！
 * // 正确用法（手动管理）
 * Message.Builder builder = pool.borrow();
 * try {
 *     builder.build();
 * } finally {
 *     pool.release(builder);
 * }
 * // 最佳实践（自动管理）
 * try (var wrapper = pool.borrowAutoCloseable()) {
 *     wrapper.get().build();
 * }
 *
 *
 *
 *
 * 每次借用时会自动 clear()
 * 归还时会再次 clear() 防止数据污染
 * 回收后再次使用会抛出 IllegalStateException
 */

/***
 * @param <T> Protobuf Message 类型（如 MyProto.Message）
 * @param <B> 对应的 Builder 类型（如 MyProto.Message.Builder）
 */
public class ProtobufBuilderPool<T extends MessageLite, B extends MessageLite.Builder> {

    /**
     * Builder 工厂接口
     */
    public interface BuilderFactory<B> {
        B newBuilder();
    }

    // Netty 的对象回收器
    private final Recycler<PooledProtobufBuilder> recycler;
    // 用于创建新 Builder 的工厂
    private final BuilderFactory<B> factory;

    /**
     * 构造函数
     * @param factory 用于创建新 Builder 实例的工厂
     */
    public ProtobufBuilderPool(BuilderFactory<B> factory) {
        this.factory = factory;
        this.recycler = new Recycler<PooledProtobufBuilder>() {
            @Override
            protected PooledProtobufBuilder newObject(Handle<PooledProtobufBuilder> handle) {
                return new PooledProtobufBuilder(handle, factory.newBuilder());
            }
        };
    }

    /**
     * 从对象池借用一个 Builder 实例
     * @return 可重用的 Builder 实例
     *
     * 注意：
     * 1. 必须与 release() 配对使用
     * 2. 返回的 Builder 已被 clear()
     */
    public B borrow() {
        PooledProtobufBuilder builder = recycler.get();
        builder.recycled = false;
        builder.clear();
        return (B) builder;
    }

    /**
     * 归还 Builder 到对象池
     * @param builder 要归还的 Builder 实例
     *
     * 注意：
     * 1. 只能归还从本池借出的 Builder
     * 2. 归还后会执行 clear()
     */
    public void release(B builder) {
        try {
            // 安全的类型转换方式
            if (builder instanceof ProtobufBuilderPool.PooledProtobufBuilder) {
                PooledProtobufBuilder pooled = (PooledProtobufBuilder) builder;
                pooled.recycle();
            } else {
                throw new IllegalArgumentException("Invalid builder type, must be from this pool");
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Builder not created by this pool", e);
        }
    }

    /**
     * 借用一个自动管理的 Builder（推荐使用方式）
     * @return AutoCloseable 包装器
     */
    public AutoCloseableBuilder<B> borrowAutoCloseable() {
        return new AutoCloseableBuilder<>(borrow(), this);
    }


//    // 估算池中可用对象数量
//    public int estimateIdleCount() {
//        return recycler.estimateCapacity();
//    }
//
//    // 估算活跃对象数量
//    public int estimateActiveCount() {
//        return recycler.estimateActiveCount();
//    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 池化的 Protobuf Builder 实现（内部类）
     *
     * 设计要点：
     * 1. 私有内部类完全隐藏实现细节
     * 2. 代理所有 Builder 方法调用
     * 3. 内置回收状态检查
     */
    private final class PooledProtobufBuilder implements MessageLite.Builder {
        // 回收器句柄（用于归还对象）
        private final Recycler.Handle<PooledProtobufBuilder> handle;
        // 被代理的实际 Builder
        private final B delegate;
        // 回收状态标记
        private boolean recycled;

        private PooledProtobufBuilder(Recycler.Handle<PooledProtobufBuilder> handle, B delegate) {
            this.handle = handle;
            this.delegate = delegate;
            this.recycled = false;
        }

        /**
         * 将 Builder 回收到对象池
         *
         * 注意：
         * 1. 每个 Builder 只能回收一次
         * 2. 回收前会自动 clear()
         */
        private void recycle() {
            if (recycled) {
                throw new IllegalStateException("Builder already recycled");
            }
            delegate.clear();
            recycled = true;
            handle.recycle(this);
        }

        /**
         * 检查 Builder 状态
         * @throws IllegalStateException 如果 Builder 已被回收
         */
        private void checkState() {
            if (recycled) {
                throw new IllegalStateException("Builder has been recycled");
            }
        }

        //========== 实现 MessageLite.Builder 接口 ==========//
        @Override
        public MessageLite.Builder clear() {
            checkState();
            return delegate.clear();
        }

        @Override
        public MessageLite build() {
            checkState();
            return delegate.build();
        }

        @Override
        public MessageLite buildPartial() {
            checkState();
            return delegate.buildPartial();
        }

        //todo
        @Override
        public MessageLite.Builder clone() {
            checkState();
            B clonedDelegate = (B) delegate.clone();
            return new PooledProtobufBuilder(this.handle, clonedDelegate); // 创建新的代理实例
        }

        //========== mergeFrom 系列方法 ==========//
        @Override
        public MessageLite.Builder mergeFrom(CodedInputStream input) throws IOException {
            checkState();
            return delegate.mergeFrom(input);
        }

        @Override
        public MessageLite.Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            checkState();
            delegate.mergeFrom(input, extensionRegistry);
            return this; // 返回 this 以维持链式调用
        }

        @Override
        public MessageLite.Builder mergeFrom(ByteString data) throws InvalidProtocolBufferException {
            checkState();
            return delegate.mergeFrom(data);
        }

        @Override
        public MessageLite.Builder mergeFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            checkState();
            return delegate.mergeFrom(data, extensionRegistry);
        }

        @Override
        public MessageLite.Builder mergeFrom(byte[] data) throws InvalidProtocolBufferException {
            checkState();
            return delegate.mergeFrom(data);
        }

        @Override
        public MessageLite.Builder mergeFrom(byte[] data, int off, int len) throws InvalidProtocolBufferException {
            checkState();
            return delegate.mergeFrom(data, off, len);
        }

        @Override
        public MessageLite.Builder mergeFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            checkState();
            return delegate.mergeFrom(data, extensionRegistry);
        }

        @Override
        public MessageLite.Builder mergeFrom(byte[] data, int off, int len, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            checkState();
            return delegate.mergeFrom(data, off, len, extensionRegistry);
        }

        @Override
        public MessageLite.Builder mergeFrom(InputStream input) throws IOException {
            checkState();
            return delegate.mergeFrom(input);
        }

        @Override
        public MessageLite.Builder mergeFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            checkState();
            return delegate.mergeFrom(input, extensionRegistry);
        }

        @Override
        public MessageLite.Builder mergeFrom(MessageLite other) {
            checkState();
            return delegate.mergeFrom(other);
        }

        @Override
        public boolean mergeDelimitedFrom(InputStream input) throws IOException {
            checkState();
            return delegate.mergeDelimitedFrom(input);
        }

        @Override
        public boolean mergeDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            checkState();
            return delegate.mergeDelimitedFrom(input, extensionRegistry);
        }

        @Override
        public MessageLite getDefaultInstanceForType() {
            checkState();
            return delegate.getDefaultInstanceForType();
        }

        @Override
        public boolean isInitialized() {
            checkState();
            return delegate.isInitialized();
        }
    }


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 自动关闭的 Builder 包装器（实现 AutoCloseable）
     *
     * 优势：
     * 1. 确保 Builder 一定会被归还
     * 2. 支持 try-with-resources 语法
     */
    public static class AutoCloseableBuilder<B extends MessageLite.Builder> implements AutoCloseable {
        private final B builder;
        private final ProtobufBuilderPool<?, B> pool;

        AutoCloseableBuilder(B builder, ProtobufBuilderPool<?, B> pool) {
            this.builder = builder;
            this.pool = pool;
        }

        /**
         * 获取被封装的 Builder
         */
        public B get() {
            return builder;
        }

        @Override
        public void close() {
            pool.release(builder);
        }
    }
}