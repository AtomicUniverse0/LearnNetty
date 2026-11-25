package client;

import config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import protocol.MessageCodecSharable;
import protocol.ProtocolFrameDecoder;

public class RpcClient {
    public static void main(String[] args) {
        // 各种Handler
        var LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        var MESSAGE_CODEC = new MessageCodecSharable(Config.getSERIALIZE_TYPE());

        MultiThreadIoEventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ProtocolFrameDecoder())
                                    .addLast(LOGGING_HANDLER)
                                    .addLast(MESSAGE_CODEC);
                        }
                    });
        } finally {
            group.shutdownGracefully();
        }
    }
}
