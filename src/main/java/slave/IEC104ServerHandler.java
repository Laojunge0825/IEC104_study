package slave;

import cn.hutool.core.convert.Convert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


/**
 * @author 舒克、舒克
 * @date 2025/3/6 16:25
 * @description: 服务端协议处理器
 */
public class IEC104ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    // 接收序列号 N(R) ， 记录当前期望接收的I帧序号（范围0~32767）
    private int receiveSeq = 0;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg)  {
        // 1. 读取接收到的字节数据,将ByteBuf转换为字节数组
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        System.out.println("接收到的字节数据：" + Convert.toStr(data));

        // 2. 解析控制域（第三字节，索引为2），判断帧类型
        byte controlByte = data[2];
        if ((controlByte & 0x03) == 0x03) {
            handleUFrame(ctx, data);
        }
        // 2.2 判断帧类型：S帧（最低位为1）
        else if ((controlByte & 0x01) == 0x01) {
            handleSFrame(ctx, data);
        }
        // 2.3 判断帧类型：I帧（最低两位为00）
        else {
            handleIFrame(ctx, data);
        }

    }

    // 处理U帧（链路控制）
    private void handleUFrame(ChannelHandlerContext ctx, byte[] data) {
        // 3. 解析U帧功能：控制域第3字节为0x07表示STARTDT激活命令
        if (data[2] == 0x07) {
            // 3.1 构造U帧确认报文：68 04 0B 00 00 00
            // 字段解析：
            // 68：起始符（固定）
            // 04：APDU长度（后续4字节）
            // 0B：控制域（U帧确认，0B表示STARTDT确认）
            // 00 00：保留字段
            byte[] resp = new byte[]{0x68, 0x04, 0x0B, 0x00, 0x00, 0x00};
            ctx.writeAndFlush(Unpooled.copiedBuffer(resp));
            System.out.println("服务端回复U帧确认：STARTDT激活成功");
        }
    }

    // 处理S帧（确认接收）
    private void handleSFrame(ChannelHandlerContext ctx, byte[] data) {
        // 4. 解析接收序列号N(R)：S帧的N(R)位于第5-6字节（小端序）
        // 注意：S帧的N(R)表示接收方已正确接收的I帧序号（期望下次接收N(R)）
        int ackSeq = ((data[4] & 0xFF) >> 1) | ((data[5] & 0xFF) << 7);
        System.out.println("服务端收到S帧确认，接收序列号N(R)=" + ackSeq);
    }

    // 处理I帧（应用数据）
    private void handleIFrame(ChannelHandlerContext ctx, byte[] data) {
        // 5. 更新接收序列号N(R)：每接收一个I帧，N(R)递增（模32768）
        receiveSeq = (receiveSeq + 1) % 32768;

        // 6. 构造S帧确认报文：68 04 01 00 [N(R)] 00
        // 字段解析：
        // 01：控制域（S帧标识，最低位为1）
        // 00：保留字段
        // [N(R)]：接收序列号（低7位在字节4，高8位在字节5）
        byte[] sFrame = new byte[]{
                0x68, 0x04,
                0x01, 0x00,
                (byte) ((receiveSeq << 1) & 0xFF), // 低7位左移1位（空出最低位）
                (byte) ((receiveSeq >> 7) & 0xFF)    // 高8位直接右移7位
        };
        ctx.writeAndFlush(Unpooled.copiedBuffer(sFrame));
        System.out.println("服务端回复S帧确认，N(R)=" + receiveSeq);
    }
}
