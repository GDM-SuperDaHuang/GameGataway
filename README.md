# 游戏服务器框架 网关gameServer
    JDK21
    apache-maven-3.9.5
    脚本工具:protoc-28.2-win64  https://github.com/protocolbuffers/protobuf/releases
    nacos:docker版本：nacos/nacos-server:latest
    
# nacos的docket启动命令：
    docker run -d `
    --name nacos-standalone `
    -p 8848:8848 `
    -p 9848:9848 `
    -p 9849:9849 `
    -v E:\nacos\conf `
    -v E:\nacos\logs `
    -v E:\nacos\data `
    -e MODE=standalone `
    nacos/nacos-server:latest

## 启动方式 
    clone GameGatewayServer，GameServer，GameCommon项目
    先在protobufFilem目录下执行脚本toJava.bat,然后对所有模块maven进行clean，install成功即可

# 项目：

    

# 模块：
# [entrance](entrance)
    主启动类，程序入口
# [gateway](gateway)
    网关服，开发待续....

# QPS轻松突破8W+，最高可达26w+
# ![img.png](img.png)
