import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

@Slf4j
public class TestClientChannel {
    private final static int loopCnt = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open();

        socketChannel.connect(new InetSocketAddress("localhost", 8080));

        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        byteBuffer.put("Hello".getBytes(StandardCharsets.UTF_8));

        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();

        for(int i = 1;  i<= loopCnt; ++i){
            String randomStr = RandomStringUtils.randomStringWithFinalSplitor(1, 100);
            byteBuffer.put(randomStr.getBytes(StandardCharsets.UTF_8));

            byteBuffer.flip();
            socketChannel.write(byteBuffer);
            byteBuffer.clear();

            log.info("Client Send message {} : {}", i, randomStr);
            Thread.sleep(500);
        }

        socketChannel.close();
        log.info("Client close connection");
    }
}
