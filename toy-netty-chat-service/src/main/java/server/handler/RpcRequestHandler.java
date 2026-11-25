package server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import message.RpcRequestMessage;
import message.RpcResponseMessage;
import server.service.rpc.ServicesFactory;

import java.lang.reflect.Method;

/*
* 这个Handler 会被用在服务端上
* */
@ChannelHandler.Sharable
@Slf4j
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage msg) throws Exception {
        RpcResponseMessage responseMessage = new RpcResponseMessage();
        try{
            String interfaceName = msg.getInterfaceName();// 全包名
            Class<?> interfaceClass = Class.forName(interfaceName);
            Object serviceInstance = ServicesFactory.getService(interfaceClass);
            if(serviceInstance == null){
                throw new IllegalArgumentException("interface " + interfaceName + " not found");
            }

            Method method = interfaceClass.getMethod(msg.getMethodName(), msg.getArgumentTypes());
            Object returnValue = method.invoke(serviceInstance, msg.getArguments());

            responseMessage.setSuccess(true);
            responseMessage.setReturnValue(returnValue);

        }catch (Exception e){
            responseMessage.setSuccess(false);
            responseMessage.setExceptionValue(e);
        }

        ctx.writeAndFlush(responseMessage);
    }
}