package server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.ILoggerFactory;

// 这些 CRUD 代码没必要写了
public class GroupMembersRequestMessageHandler extends SimpleChannelInboundHandler<GroupMembersRequestMessageHandler> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupMembersRequestMessageHandler msg) throws Exception {
        throw new IllegalArgumentException("d");
    }
}
