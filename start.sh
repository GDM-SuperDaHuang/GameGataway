#!/bin/bash

JAR="game.jar"
LOG="game.log"
PID="game.pid"

# 检查是否已运行
if [ -f "$PID" ]; then
    ps -p $(cat "$PID") >/dev/null 2>&1 && { echo "服务已运行 (PID: $(cat "$PID"))"; exit 1; }
    rm -f "$PID"
fi

# 启动服务
echo "正在启动 Game 服务..."
nohup java -Xms512m -Xmx1024m -jar "$JAR" > "$LOG" 2>&1 &
echo $! > "$PID"

# 验证启动
sleep 1
if ! ps -p $(cat "$PID") >/dev/null 2>&1; then
    echo "启动失败！查看日志: tail -f $LOG"
    rm -f "$PID"
    exit 1
fi

echo "服务已启动 (PID: $(cat "$PID"))"
echo "查看日志: tail -f $LOG"
