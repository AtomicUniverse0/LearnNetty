package server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import message.ChatRequestMessage;
import message.ChatResponseMessage;
import server.session.SessionFactory;

@ChannelHandler.Sharable
@Slf4j
public class ChatRequestMessageHandler extends SimpleChannelInboundHandler<ChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatRequestMessage msg) throws Exception {

        Channel targetChannel = SessionFactory.getSession().getChannel(msg.getTo());
        if( targetChannel == null){
            // 对方没有上线
            ctx.writeAndFlush(new ChatResponseMessage(false, "目标用户未登陆"));
        }else{
            targetChannel.writeAndFlush( new ChatResponseMessage(msg.getFrom(), msg.getContent()));
        }
    }
}
