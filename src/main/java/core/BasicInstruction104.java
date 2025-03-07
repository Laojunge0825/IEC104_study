package core;

/**
 * @author 舒克、舒克
 * @date 2025/3/7 11:26
 * @description: IEC104规约基本指令封装
 */
public class BasicInstruction104 {

    /**
     * 链路启动指令F 主站发送
     */
    public static final byte[] STARTDT = new byte[] {0x68, 0x04, 0x07, 0x00, 0x00, 0x00};

    // 68040B 00 00 00
    /**
     * 初始启动确认指令 子站回复
     */
    public static final byte[] STARTDT_YES = new byte[] {0x68, 0x04, 0x0B, 0x00, 0x00, 0x00};


    /**
     * 测试命令指令  主站站发送U格式测试询问帧 主站发送
     */
    public static final byte[] TESTFR = new byte[] {0x68, 0x04, (byte) 0x43, 0x00, 0x00, 0x00};

    /**
     * 测试确认 子站回复
     */
    public static final byte[] TESTFR_YES = new byte[] {0x68, 0x04, (byte) 0x83, 0x00, 0x00, 0x00};

    /**
     * 停止指令
     */
    public static final byte[] STOPDT = new byte[] {0x68, 0x04, 0x13, 0x00, 0x00, 0x00};

    /**
     * 停止确认
     */
    public static final byte[] STOPDT_YES = new byte[] {0x68, 0x04, 0x23, 0x00, 0x00, 0x00};

    public static final byte[] TEST = new byte[] {0x68, 0x04, (byte) 0x04, 0x04, 0x04, 0x04};

}
