package client;

import config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import message.RpcRequestMessage;
import protocol.MessageCodecSharable;
import protocol.ProtocolFrameDecoder;
import server.handler.RpcResponseHandler;
import server.service.rpc.hello.HelloService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Slf4j
public class RpcClientManager {
    // 使用这个channel 来向服务端发起RPC调用
    private static volatile Channel channel = null;
    // TODO: 这种 Lock 和 RetrantientLock 相比，谁的开销更大？
    private static final Object channelLock = new Object();

    private static Channel getChannel() throws InterruptedException {
        if(channel != null){
            return channel;
        }

        synchronized (channelLock) {
            if(channel == null) {
                channel = initChannel();
            }
        }

        return channel;
    }
    // 只调用一次
    private static Channel initChannel() throws InterruptedException {
        var LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        var MESSAGE_CODEC = new MessageCodecSharable(Config.getSERIALIZE_TYPE());
        var RpcResponseHandler = new RpcResponseHandler();

        MultiThreadIoEventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ProtocolFrameDecoder())
                                    .addLast(LOGGING_HANDLER)
                                    .addLast(MESSAGE_CODEC)
                                    .addLast(RpcResponseHandler);
                        }
                    });
            Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
            channel.closeFuture().addListener(
                    future -> {
                        group.shutdownGracefully();
                    }
            );

            return channel;
        } catch (Exception e) {
            log.error("初始化 channel 失败");
            group.shutdownGracefully();
            throw e;
        }
    }

    public static  <T> T getServiceProxy(Class<T> interfaceClass ) {
        if(!interfaceClass.isInterface()){
            throw new IllegalArgumentException(interfaceClass.getName() + " is not an interface!");
        }

        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        RpcRequestMessage rpcRequestMessage = new RpcRequestMessage(
                                interfaceClass.getName(),
                                method.getName(),
                                method.getReturnType(),
                                method.getParameterTypes(),
                                args
                        );

                        ChannelFuture channelFuture = getChannel().writeAndFlush(rpcRequestMessage);

                        channelFuture.addListener( future -> {
                            if(!future.isSuccess()){
                                log.error("error: ", future.cause());
                            }
                        });

                        return null; // 暂时返回 null, 后面会修改
                    }
                }
        );
    }

    public static void main(String[] args) throws InterruptedException {
        HelloService serviceProxy = getServiceProxy(HelloService.class);
        serviceProxy.sayHello("张三");
        serviceProxy.sayHello("李四");

        // 等待移除
        getChannel().closeFuture().sync();
    }
}