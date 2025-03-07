package master;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import slave.IEC104ServerHandler;

/**
 * @author 舒克、舒克
 * @date 2025/3/6 17:09
 * @description: 客户端启动类
 */
public class IEC104Client {
    public static void main(String[] args) {
        // 1. 初始化Netty线程组
        // 客户端通常只需要一个线程 同时处理 连接 和 I/O 操作
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 2. 配置客户端引导类
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)  // 设置线程组
            .channel(NioSocketChannel.class)
           .handler(new ChannelInitializer<SocketChannel>() {
               // 3. 初始化客户端通道
               // 每个新连接建立后，会调用此方法配置协议处理器链
               // 每个连接对应一个独立的ChannelPipeline
               @Override
               protected void initChannel(SocketChannel ch) throws Exception {
                   ch.pipeline() //配置责任链模式
                           // 解决TCP粘包/拆包：基于长度字段的帧解码器
                           // 参数说明：最大长度255，长度字段偏移量2，长度字段长度1字节
                           // 跳过初始字节0
                           .addLast(new LengthFieldBasedFrameDecoder(255, 1, 1, 0, 0))
                           // - 日志处理器：记录通信报文
                           .addLast(new LoggingHandler(LogLevel.INFO))
                           // 添加自定义协议处理器
                           .addLast(new IEC104ClientHandler());
                   // 扩展点：可在此处添加更多处理器，如：
                   // - 编码器：将Java对象编码为字节流
                   // - 解码器：将字节流解析为Java对象

               }
           });
           // 4. 连接服务器
            // 启动客户端并连接至服务端地址（localhost:2404）
            // 异步连接服务端，sync() 阻塞等待连接完成。
            // 连接成功后，返回一个ChannelFuture对象，可用于监听连接状态和获取连接结果。
           ChannelFuture future = bootstrap.connect("127.0.0.1", 2404).sync();
            // 等待连接成功
            System.out.println("客户端连接成功，准备发送IEC 104协议报文...");
            // ============= 5. 阻塞等待连接关闭 =============
            // 保持主线程活跃，直到通道被关闭（如手动关闭或异常断开）
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            // 处理线程中断异常（如用户强制终止程序）
            e.printStackTrace();
        } finally {
            // ============= 6. 优雅关闭线程组 =============
            // 释放所有线程资源，确保无内存泄漏
            group.shutdownGracefully();
            System.out.println("客户端已安全关闭，释放线程资源。");
        }
    }
}
