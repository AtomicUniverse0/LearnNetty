package client;

import config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import message.*;
import protocol.MessageCodecSharable;
import protocol.ProtocolFrameDecoder;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ChatClient {
    private static volatile CountDownLatch LOGIN_LATCH = null;

    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable(Config.getSERIALIZE_TYPE());
        AtomicBoolean EXIT = new AtomicBoolean(false);
        AtomicBoolean LOGIN = new AtomicBoolean(false);
        AtomicReference<String> LOGIN_REASON = new AtomicReference<>();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                      @Override
                      protected void initChannel(SocketChannel ch) throws Exception {
                          ch.pipeline().addLast(new ProtocolFrameDecoder());
                          ch.pipeline().addLast(LOGGING_HANDLER);
                          ch.pipeline().addLast(MESSAGE_CODEC);
                          ch.pipeline().addLast(new IdleStateHandler(0, 7, 0));
                          ch.pipeline().addLast(new ChannelDuplexHandler(){
                              @Override
                              public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                  if(evt instanceof IdleStateEvent idleStateEvent){
                                      if(idleStateEvent.state() == IdleState.WRITER_IDLE){
                                          log.info("已经7秒没有向服务器发送消息了，发送心跳");
                                          ctx.writeAndFlush(new PingRequestMessage());
                                      }
                                  }
                              }
                          });

                          // 先不考虑其余情况，加一个登陆的功能
                          ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                              @Override
                              public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                  log.info("收到消息: {}", msg);
                                  if(msg instanceof LoginResponseMessage loginResponseMessage){
                                      if(loginResponseMessage.isSuccess()){
                                          LOGIN.set(true);
                                          LOGIN_REASON.set("Success");
                                      }else{
                                          LOGIN_REASON.set(loginResponseMessage.getReason());
                                          LOGIN.set(false);
                                      }
                                      LOGIN_LATCH.countDown();
                                  }else{
                                      throw new IllegalArgumentException("unkonwn");
                                  }
                              }

                              // 连接建立后触发
                              @Override
                              public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                  ChatLoopArgument chatLoopArgument =
                                          ChatLoopArgument.builder()
                                                  .LOGIN(LOGIN)
                                                  .LOGIN_REASON(LOGIN_REASON)
                                                  .build();

                                  new Thread(()->{
                                      chatLoop(ctx, chatLoopArgument);
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

    // 为了使得传参美观而建立的
    @Data
    @Builder
    @AllArgsConstructor
    private static class ChatLoopArgument{
        AtomicBoolean LOGIN;
        AtomicReference<String> LOGIN_REASON;
    }

    public static void chatLoop( ChannelHandlerContext ctx, ChatLoopArgument chatLoopArgument){
        Scanner scanner = new Scanner(System.in);
        AtomicBoolean LOGIN = chatLoopArgument.getLOGIN();
        AtomicReference<String> LOGIN_REASON = chatLoopArgument.getLOGIN_REASON();

        for(;;){
            // CountDownLatch 是一次性的，每次使用必须新建一个
            LOGIN_LATCH = new CountDownLatch(1);
            System.out.println("请输入username");
            String username = scanner.next();
            System.out.println("请输入password");
            String password = scanner.next();
            scanner.nextLine(); // 消费密码后的换行，后续按整行读取命令

            LoginRequestMessage loginRequestMessage = new LoginRequestMessage(username,password);
            // 这个会逆向触发Handler
            ctx.writeAndFlush(loginRequestMessage);

            try {
                LOGIN_LATCH.await(3, TimeUnit.MINUTES);
                if(!LOGIN.get()){
                  System.out.println("Login Failed, reason: " + LOGIN_REASON.get());
                  System.out.println("Please try again.");
                  continue;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            System.out.println("Login Success, please your next action: ");

            System.out.println("==================================");
            // 私聊，对应ChatRequestMessage
            System.out.println("send [username] [content]");
            // 群里聊天，对应GroupChatRequestMessage
            System.out.println("gsend [group name] [content]");
            // 对应GroupCreateRequestMessage，第二个参数是可选的，代表群聊创建时的初始成员
            System.out.println("gcreate [group name] [m1,m2,m3...]");
            // 获取成员列表
            System.out.println("gmembers [group name]");
            // 加入群聊
            System.out.println("gjoin [group name]");
            // 退出群聊
            System.out.println("gquit [group name]");
            // 退出聊天
            System.out.println("quit");
            System.out.println("==================================");
            ChatCommandParser parser = new ChatCommandParser(username);
            while(true){
                System.out.print(">>: ");
                if (!scanner.hasNextLine()) {
                    ctx.close();
                    return;
                }
                try {
                    Message message = parser.parse(scanner.nextLine());
                    if (message instanceof QuitRequestMessage) {
                        ctx.close();
                        log.info("程序退出。");
                        return;
                    }
                    ctx.writeAndFlush(message);
                } catch (CommandParseException e) {
                    System.out.println(e.getMessage());
                }
            }
       }
    }
}
