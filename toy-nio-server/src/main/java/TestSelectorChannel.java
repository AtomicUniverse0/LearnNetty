import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
public class TestSelectorChannel {
    private static void consume (ByteBuffer source, SocketChannel socketChannel) throws IOException {
        source.flip();

        for(int i=0; i < source.limit(); ++i){
            if(RandomStringUtils.Splitor == source.get(i)){
                int length = i + 1 - source.position();

                ByteBuffer target = ByteBuffer.allocate(length);

                while(length > 0){
                    --length;

                    byte b = source.get();
                    target.put(b);
                }

                target.flip();
                log.info("receive message {} from {}", StandardCharsets.UTF_8.decode(target), socketChannel.getRemoteAddress());
            }
        }

        source.compact();
    }

    public static void main(String[] args) throws IOException {
        // OS 默认是水平触发: 只要不处理，就一直返回
        Selector selector = Selector.open();

        // 非阻塞的监听 socket
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        SelectionKey selectionKey = serverSocketChannel.register(selector, 0, null);
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);

        serverSocketChannel.bind(new InetSocketAddress(8080));

        while(true){
            selector.select();

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();

                if(key.isAcceptable()){
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel newSocketChannel = channel.accept();
                    log.info("New socket accepted: {}", newSocketChannel.getRemoteAddress());

                    newSocketChannel.configureBlocking(false);
                    ByteBuffer attachment = ByteBuffer.allocate(8);
                    newSocketChannel.register(selector, SelectionKey.OP_READ, attachment);

                }else if(key.isReadable()) {
                    // 先假设1024个字节的缓冲区
                    SocketChannel channel = (SocketChannel)key.channel();
                    ByteBuffer byteBuffer = (ByteBuffer) key.attachment();

                    int readCnt = channel.read(byteBuffer);
                    if(readCnt == -1){ // 客户端主动断开或者被强制断开都是返回 -1
                        log.info("connection with {} has been closed。", channel.getRemoteAddress());
                        key.cancel(); // 从 selector 的 key 集合中真正删除
                        continue;
                    }else if( readCnt < 0){
                        throw new RuntimeException("未知readCnt:" + readCnt);
                    }
                    assert (readCnt <= 1024);

                    consume(byteBuffer, channel);
                    // byteBuffer 经过 consume 后会变成写入状态
                    if(byteBuffer.position() == byteBuffer.limit()){ // 满了，需要扩容
                        ByteBuffer largerByteBuffer = ByteBuffer.allocate( 2 * byteBuffer.limit() );
                        byteBuffer.flip();
                        largerByteBuffer.put(byteBuffer);
                        log.info("Enlarge buffer to {} for process message from {}", largerByteBuffer.limit(), channel.getRemoteAddress());
                        key.attach(largerByteBuffer);
                    }

                }else{
                        throw new RuntimeException("not support yet");
                }
            }

        }

    }
}