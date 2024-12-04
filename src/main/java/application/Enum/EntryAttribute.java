package application.Enum;

/**
 * 登记项属性枚举
 */
public enum EntryAttribute {
    READ_ONLY((byte) 0x01), // 第0位 只读
    SYSTEM_FILE((byte) 0x02), // 第1位 系统文件
    NORMAL_FILE((byte) 0x04), // 第2位 普通文件
    DIRECTORY((byte) 0x08); // 第3位 目录

    private final byte value;

    EntryAttribute(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    /**
     * 判断属性是否相等
     * @param attribute 需要判断的属性
     * @return 属性是否相等
     */
    public boolean isEqual(byte attribute) {
        return (this.value & attribute) == this.value; // 只关心属性位是否被设置，而不是完全相等
    }
}
