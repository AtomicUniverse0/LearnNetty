package server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import server.session.SessionFactory;

@ChannelHandler.Sharable
@Slf4j
public class QuitHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 取消服务端注册的 channel
        SessionFactory.getSession().unbind(ctx.channel());
        log.info("已经断开连接");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SessionFactory.getSession().unbind(ctx.channel());
        log.info("已经异常断开连接");
    }
}
