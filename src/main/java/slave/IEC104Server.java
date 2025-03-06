package slave;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author 舒克、舒克
 * @date 2025/3/6 16:12
 * @description: 服务端启动类
 */
public class IEC104Server {
    public static void main(String[] args) {
        // 1. 创建Netty线程组：boosGroup 处理连接请求 ，workerGroup 处理I/O操作
        // 单线程处理连接请求
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 多线程处理I/O操作 默认线程数为cpu核数*2
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try{
            // 2. 创建服务器端启动对象，配置参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup) // 设置两个线程组
                    .channel(NioServerSocketChannel.class) // 使用NioServerSocketChannel作为服务器通道实现  NIO模型
                   .childHandler(new ChannelInitializer<SocketChannel>() {
                      @Override
                       protected void initChannel(SocketChannel ch) throws Exception {
                          // 2. 配置ChannelPipeline（责任链模式），添加自定义的处理器
                          ch.pipeline()
                                  // 2.1 解决TCP粘包/拆包：基于长度字段的帧解码器
                                  // 参数说明：最大长度255，长度字段偏移量2，长度字段长度1字节
                                  // 长度调整值-3（因IEC 104报文头为2字节起始符+1字节长度）
                                  // 跳过初始字节0（直接处理有效数据）
                                  .addLast(new LengthFieldBasedFrameDecoder(255, 2, 1, -3, 0))
                                  // 2.2 自定义协议处理器
                                  .addLast(new IEC104ServerHandler());

                      }
                   }); // 子处理器，用于处理workerGroup
            // 3. 绑定端口，开始接收连接
            ChannelFuture future = bootstrap.bind(2404).sync();
            System.out.println("服务端启动，监听端口2404...");
            future.channel().closeFuture().sync();
        }   catch (Exception e){
            e.printStackTrace();
        } finally {
            // 4. 关闭线程组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
