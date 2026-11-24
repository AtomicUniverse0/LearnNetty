package server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import message.GroupCreateRequestMessage;
import message.GroupCreateResponseMessage;
import server.session.Group;
import server.session.GroupSession;
import server.session.GroupSessionFactory;

import java.util.List;

@ChannelHandler.Sharable
@Slf4j
public class GroupCreateRequestMessageHandler extends SimpleChannelInboundHandler<GroupCreateRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupCreateRequestMessage msg) throws Exception {
        String groupName = msg.getGroupName();
        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        Group group = groupSession.createGroup(groupName, msg.getMembers());

        // TODO: 有个BUG，应该将群主也拉进来，此时 msg.getMembers 里不含群主的，
        // 这个项目毕竟只是个学习 netty 的玩具，不必学的太精细。
        if(group == null){
            // 组名已经存在
            ctx.writeAndFlush(new GroupCreateResponseMessage(false, "组名已经存在"));
            return;
        }else{
            ctx.writeAndFlush(new GroupCreateResponseMessage(true, "成功创建群聊: groupName"));
        }

        List<Channel> membersChannel = groupSession.getMembersChannel(msg.getGroupName());
        for(var channel : membersChannel){
            channel.writeAndFlush(new GroupCreateResponseMessage(true, "您已经被拉入" + groupName));
        }
    }
}
