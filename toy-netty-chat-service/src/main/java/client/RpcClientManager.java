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
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
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

                        // 确保发送成功，这样的实现比较方便。。。
                        ChannelFuture channelFuture = getChannel().writeAndFlush(rpcRequestMessage);
                        channelFuture.sync();
                        if(!channelFuture.isSuccess()){
                            log.error("error: ", channelFuture.cause());
                            throw new RuntimeException(channelFuture.cause());
                        }

                        // TODO: 弄懂为何必须放一个 eventLoop() 进去
                        DefaultPromise<Object> promise = new DefaultPromise<>(channel.eventLoop());
                        RpcResponseHandler.setNewPromises(rpcRequestMessage.getSequenceId(), promise);

                        promise.await(); // 即使出现异常也不会报错。
                        if(promise.isSuccess()){
                            return promise.getNow();
                        }else{
                            throw new RuntimeException(promise.cause());
                        }
                    }
                }
        );
    }

    public static void main(String[] args) throws InterruptedException {
        HelloService serviceProxy = getServiceProxy(HelloService.class);
        log.info("调用成功：" + serviceProxy.sayHello("张三"));
        log.info("调用成功: " + serviceProxy.sayHello("里斯"));

        // 等待移除
        getChannel().closeFuture().sync();
    }
}