package server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;
import message.RpcResponseMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
@Slf4j
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {
    private static final Map<Integer, DefaultPromise<Object>> promiseMap = new ConcurrentHashMap<>();

    public static void setNewPromises(Integer id, DefaultPromise<Object> promise) {
        var previousVlaue = promiseMap.putIfAbsent(id, promise);
        if (previousVlaue != null) {
            throw new IllegalArgumentException("There is already a id for id " + id);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage msg) throws Exception {
        log.info("接收到消息: {}", msg);

        DefaultPromise<Object> objectDefaultPromise = promiseMap.remove(msg.getSequenceId());
        if(objectDefaultPromise == null){
            log.error("There is no promise corrospending to " + msg.getSequenceId());
            return;
        }

        // TODO: 完善异常
        Object returnValue = msg.getReturnValue();
        objectDefaultPromise.setSuccess(returnValue);
    }
}
