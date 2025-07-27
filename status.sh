#!/bin/bash
PID="game.pid"

# 检查是否运行
if [ ! -f "$PID" ]; then
    echo "服务未运行"
    exit 0
fi

PID_NUM=$(cat "$PID")
if ps -p "$PID_NUM" >/dev/null 2>&1; then
    echo "服务正在运行 (PID: $PID_NUM)"
    echo "资源使用: $(ps -o %cpu,%mem,rsz,vsz -p "$PID_NUM" | tail -1)"
else
    echo "服务未运行 (旧 PID 文件已删除)"
    rm -f "$PID"
fi
