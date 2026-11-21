import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

public class NettyClient {
    public static void main(String[] args) throws InterruptedException {
        // Server用的是ServerBootstrap，后者内部有
        new Bootstrap()
                .group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
                // 与Server不同，Server用的是 NioServerSocketChannel
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        // 把 字符串 转化为 ByteBuf
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                // connect 返回的是一个 ChannelFuture
                // 这里的 connect 是一个异步非阻塞（凡是异步的都是非阻塞的）
                .connect(new InetSocketAddress("127.0.0.1", 8080))
                // 如果不阻塞的话，后续获得的 channel 是无效的channel，所以无法进行 writeAndFlush
                .sync() // 阻塞，直到连接建立
                .channel() // 代表连接对象
                .writeAndFlush("hello netty");
        // 不愿意用 sync 的话，后续还是用 addListener 来传递一个回调对象，异步处理
    }
}
