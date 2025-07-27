#!/bin/bash

PID="game.pid"

# 检查是否运行
if [ ! -f "$PID" ]; then
    echo "服务未运行"
    exit 0
fi

PID_NUM=$(cat "$PID")
if ! ps -p "$PID_NUM" >/dev/null 2>&1; then
    echo "服务未运行 (旧 PID 文件已删除)"
    rm -f "$PID"
    exit 0
fi

# 停止服务
echo "正在停止服务 (PID: $PID_NUM)..."
kill "$PID_NUM"

# 等待服务停止
for i in {1..10}; do
    if ! ps -p "$PID_NUM" >/dev/null 2>&1; then
        echo "服务已停止"
        rm -f "$PID"
        exit 0
    fi
    sleep 1
    echo "等待服务停止 ($i/10)..."
done

# 强制终止
echo "服务未响应，正在强制终止..."
kill -9 "$PID_NUM"
rm -f "$PID"
echo "服务已强制终止"
