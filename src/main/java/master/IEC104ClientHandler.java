package master;

import cn.hutool.core.convert.Convert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author 舒克、舒克
 * @date 2025/3/6 17:24
 * @description: 自定义客户端协议处理器
 */
public class IEC104ClientHandler extends ChannelInboundHandlerAdapter {
     private int sendSeq = 0; // 发送序列号 N(S) ，记录当前发送的I帧序号（范围0~32767）

    @Override
    public void channelActive(ChannelHandlerContext ctx)  {
        // 1. 构造U帧（链路控制）：68 04 07 00 00 00
        // 字段解析：
        // 68：起始符（固定）
        // 04：APDU长度（后续4字节）
        // 07：控制域（U帧，07表示STARTDT激活）
        byte[] uFrame = new byte[]{0x68, 0x04, 0x07, 0x00, 0x00, 0x00};
        ctx.writeAndFlush(Unpooled.copiedBuffer(uFrame));
        System.out.println("客户端发送U帧：" + Convert.toStr(uFrame));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)  {
        ByteBuf buf = (ByteBuf) msg;
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        System.out.println("客户端收到报文: " + Convert.toStr(data));

        // 3. 处理U帧确认（服务端回复0B 00）
        if (data[2] == 0x0B) {
            System.out.println("链路激活成功，开始发送I帧数据...");
            sendIFrame(ctx); // 发送第一条I帧
        }

        // 4. 处理S帧确认
        if ((data[2] & 0x01) == 0x01) { // S帧最低位为1
            // 解析N(R)：表示服务端已正确接收前N(R)-1帧
            int ackSeq = ((data[4] & 0xFF) >> 1) | ((data[5] & 0xFF) << 7);
            System.out.println("客户端收到S帧确认，N(R)=" + ackSeq);
            // 可在此处继续发送后续I帧（需根据流量控制逻辑）
        }
    }

    // 发送I帧（遥测数据示例）
    private void sendIFrame(ChannelHandlerContext ctx) {
        sendSeq = (sendSeq + 1) % 32768; // 更新发送序列号

        // 5. 构造I帧报文：68 0C [控制域] [ASDU数据]
        // 控制域字段：
        // 字节2: 发送序列号N(S)低8位（左移1位，空出最低位）
        // 字节3: 发送序列号N(S)高8位
        // 字节4: 接收序列号N(R)低8位（此处固定为0）
        // 字节5: 接收序列号N(R)高8位（固定为0）
        byte[] iFrame = new byte[]{
                0x68, 0x0C,                   // 起始符+长度（12字节）
                (byte) ((sendSeq << 1) & 0xFF), // N(S)低8位
                (byte) (sendSeq >> 7),          // N(S)高8位
                0x00, 0x00,                   // N(R)（暂时未用）
                0x64, 0x01, 0x06, 0x00,       // ASDU头（类型标识0x64=100，遥测）
                0x01, 0x00, 0x00, 0x00, 0x01  // 数据部分（值=1，品质描述=0）
        };
        ctx.writeAndFlush(Unpooled.copiedBuffer(iFrame));
        System.out.println("客户端发送I帧，N(S)=" + sendSeq);
    }

}
