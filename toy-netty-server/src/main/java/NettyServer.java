import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import java.util.concurrent.Future;

public class NettyServer {
    public static void main(String[] args) {
        // 启动器，负责组装 netty组件，启动服务器

        new ServerBootstrap()
                // EventLoop 的组， 这个 MultiThreadIoEventLoopGroup 即被用于 Worker， 又被用于 Boss
                .group(new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory()),
                        new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory()))
                // 这个channel 类似于前面的 ServerSocketChannel 实现
                // channel 会调用自己的 EventLoopGroup 的 register 方法，来将自己注册到一个EventLoop上
                // 从此之后，这个EventLoop 专门负责处理这个 Channel
                .channel(NioServerSocketChannel.class)
                // childHandler 决定了 worker 能够执行哪些操作
                .childHandler(
                        new ChannelInitializer<NioSocketChannel>(){
                            @Override
                            protected void initChannel(NioSocketChannel ch) throws Exception {
                                // 在连接建立后调用
                                ch.pipeline().addLast(new StringDecoder()); // 将 Bytebuf 还原成字符串
                                ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        System.out.println(msg);
                                    }
                                });
                            }
                        }
                )
                .bind(8080);
    }
}
