package server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

// 这些 CRUD 代码没必要写了
public class GroupQuitRequestMessageHandler extends SimpleChannelInboundHandler<GroupQuitRequestMessageHandler> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupQuitRequestMessageHandler msg) throws Exception {
        throw new IllegalArgumentException("d");
    }
}