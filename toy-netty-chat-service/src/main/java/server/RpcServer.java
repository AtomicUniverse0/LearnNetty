package server;

import config.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import protocol.MessageCodecSharable;
import protocol.ProtocolFrameDecoder;

public class RpcServer {
    public static void main(String[] args) {
        var LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        var MESSAGE_CODEC = new MessageCodecSharable(Config.getSERIALIZE_TYPE());

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
                                    .addLast(MESSAGE_CODEC);
                        }
                    });
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}