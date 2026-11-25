package server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import message.LoginRequestMessage;
import message.LoginResponseMessage;
import server.service.user.UserServiceFactory;
import server.session.SessionFactory;

@ChannelHandler.Sharable
@Slf4j
public class LoginRequestMessageHandler extends SimpleChannelInboundHandler<LoginRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        boolean success = UserServiceFactory.getUserService().login(msg.getUserName(), msg.getPassword());
        log.error("Server receive login msg, username: {}, password:{}", msg.getUserName(), msg.getPassword());

        LoginResponseMessage loginResponseMessage;
        if (success) {
            SessionFactory.getSession().bind(ctx.channel(), msg.getUserName());
            loginResponseMessage = new LoginResponseMessage(true, null);
        } else {
            loginResponseMessage = new LoginResponseMessage(false, "用户名或密码错误");
        }

        ctx.writeAndFlush(loginResponseMessage);
    }
}
