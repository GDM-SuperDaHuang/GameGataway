# 游戏服务器框架 网关gameServer
    JDK21
    apache-maven-3.9.5
    脚本工具:protoc-28.2-win64  https://github.com/protocolbuffers/protobuf/releases

## 启动方式 
    clone GameGatewayServer，GameServer，GameCommon项目
    先在protobufFilem目录下执行脚本toJava.bat,然后对所有模块maven进行clean，install成功即可

# 模块：
# [GameCommon](GameCommon)
    公共模块:https://github.com/GDM-SuperDaHuang/GameCommon.git，protobuf文件，以及工具类的开发摘要 公共模块，protobuf文件，以及工具类的开发
# [GameGatewayServer](GameGatewayServer)
	网关服:https://github.com/GDM-SuperDaHuang/GameServer.git，路由，转发，负载均衡配置。
# [GameServer](GameServer)
	业务服:https://github.com/GDM-SuperDaHuang/GameServer.git，内置一场调用，进行具体业务等。
	
## 注解 
    @ToMethod:作用于类上，表明这个类为与客户端交互的类
    
    @ToServer:
              1，作用于方法上，表明这个方法为与客户端响应的方法；
              2，注解参数：协议唯一id
              3，注意方法参数格式(ChannelHandlerContext,proto生成类,userId),第一个参数必须为ChannelHandlerContext,第二参数必须为proto生成类,第三参数为long类型userId
              4，SendMsg.java 消息发送根据

# [entrance](entrance)
    主启动类，程序入口
# [gateway](gateway)
    网关服，开发待续....

