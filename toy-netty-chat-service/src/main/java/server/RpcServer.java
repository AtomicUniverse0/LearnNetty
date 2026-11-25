package server;

import config.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import protocol.MessageCodecSharable;
import protocol.ProtocolFrameDecoder;
import server.handler.RpcRequestHandler;

public class RpcServer {
    public static void main(String[] args) throws InterruptedException {
        var LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        var MESSAGE_CODEC = new MessageCodecSharable(Config.getSERIALIZE_TYPE());
        var RPC_HANDLER = new RpcRequestHandler();

        MultiThreadIoEventLoopGroup boss = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        MultiThreadIoEventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class)
                    .group(boss, worker)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ProtocolFrameDecoder())
                                    .addLast(LOGGING_HANDLER)
                                    .addLast(MESSAGE_CODEC)
                                    .addLast(RPC_HANDLER);
                        }
                    });

            Channel channel = serverBootstrap.bind(8080).sync().channel();
            // 让当前线程一直等待服务器关闭
            channel.closeFuture().sync();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}