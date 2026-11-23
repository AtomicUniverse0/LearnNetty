package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import message.LoginRequestMessage;
import protocol.MessageCodecSharable;
import protocol.ProcotolFrameDecoder;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ChatClient {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable(MessageCodecSharable.SerializeType.JDK);
        CountDownLatch WAIT_FOR_LOGIN = new CountDownLatch(1);
        AtomicBoolean LOGIN = new AtomicBoolean(false);
        AtomicBoolean EXIT = new AtomicBoolean(false);
        Scanner scanner = new Scanner(System.in);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                      @Override
                      protected void initChannel(SocketChannel ch) throws Exception {
                          ch.pipeline().addLast(new ProcotolFrameDecoder());
                          ch.pipeline().addLast(LOGGING_HANDLER);
                          ch.pipeline().addLast(MESSAGE_CODEC);

                          // 先不考虑其余情况，加一个登陆的功能
                          ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                              @Override
                              public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                  log.info("收到消息: {}", msg);
                              }

                              // 连接建立后触发
                              @Override
                              public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                  new Thread(()->{
                                      System.out.println("请输入username");
                                      String username = scanner.next();
                                      System.out.println("请输入password");
                                      String password = scanner.next();

                                      LoginRequestMessage loginRequestMessage = new LoginRequestMessage(username,password);
                                      // 这个会逆向触发Handler
                                      ctx.writeAndFlush(loginRequestMessage);

                                      try {
                                          int read = System.in.read();
                                      } catch (IOException e) {
                                          throw new RuntimeException(e);
                                      }
                                  }, "user interactive thread").start();
                              }
                          });
                      }
                  }
              );
            Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
