package server;

import config.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import protocol.MessageCodecSharable;
import protocol.ProcotolFrameDecoder;
import protocol.serialize.factory.SerializerFactory;
import server.handler.ChatRequestMessageHandler;
import server.handler.GroupCreateRequestMessageHandler;
import server.handler.LoginRequestMessageHandler;
import server.handler.QuitHandler;

@Slf4j
public class ChatServer {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();

        var LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        var MESSAGE_CODEC = new MessageCodecSharable(Config.getSERIALIZE_TYPE());
        var LOGIN_HANDLER = new LoginRequestMessageHandler();
        var SEND_HANDLER = new ChatRequestMessageHandler();
        var GROUP_CREATE_HANDLER = new GroupCreateRequestMessageHandler();
        var QUIT_HANDLER = new QuitHandler();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);

            serverBootstrap.childHandler(
                    new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // ProcotolFrameDecoder 必须在 initChannel 内新建，因为这个 handler 会
                            // 保留Handler相关的实例信息
                            ch.pipeline().addLast(new IdleStateHandler(10, 0, 0));
                            ch.pipeline().addLast(new ChannelDuplexHandler(){
                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                    if(evt instanceof IdleStateEvent idleStateEvent){
                                        if(idleStateEvent.state() == IdleState.READER_IDLE){
                                            log.info("已经10秒没有接收到数据了");
                                        }
                                    }
                                }
                            });
                            ch.pipeline().addLast(new ProcotolFrameDecoder());
                            ch.pipeline().addLast(LOGGING_HANDLER);
                            ch.pipeline().addLast(MESSAGE_CODEC);
                            ch.pipeline().addLast(LOGIN_HANDLER);
                            ch.pipeline().addLast(SEND_HANDLER);
                            ch.pipeline().addLast(GROUP_CREATE_HANDLER);
                            ch.pipeline().addLast(QUIT_HANDLER);
                        }
            });

            Channel channel = serverBootstrap.bind(8080).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

}
