package application.Service;

import application.Entity.Entry;
import application.Enum.BlockStatus;
import application.Enum.EntryAttribute;
import application.Enum.EntryStructure;
import application.Manager.DiskManager;

import java.util.Arrays;

/**
 * 目录操作类，包括建立目录、列目录、删除空目录
 */
public class DirOperator {
    private static final byte Empty = (byte) '$'; // 目录空标志

    private final EntryOperator entryOperator; // 目录项操作


    public DirOperator(EntryOperator entryOperator) {
        this.entryOperator = entryOperator;
    }

    /**
     * 创建新目录
     *
     * @param dirAbsolutePath   目录完整路径
     * @param attribute 目录属性
     * @return 1:成功；0:只读属性无法创建；-1:路径不存在；-2:目录已存在；-3:无空闲磁盘块；-4:根目录已满；-5:目录名不合法
     */
    public int createDir(String dirAbsolutePath, byte attribute) throws Exception {
        // 解析目录路径，获取父目录盘块号，以检查父目录是否存在
        String[] pathComponents = dirAbsolutePath.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        // System.out.println("parentName: " + parentDirName +"("+parentDirBlockIndex+")"+ ",and dirname: " + dirAbsolutePath);
        if (parentDirBlockIndex == -1) {
            return -1;
        }

        // 检查目录名（含type）
        byte[][] name = Tools.checkNameAndType(pathComponents[pathComponents.length - 1], EntryAttribute.DIRECTORY.isEqual(attribute));
        if (name == null) {
            return -5;
        }
        byte[] dirNameBytes = new byte[EntryStructure.NAME_LENGTH.getValue()];
        System.arraycopy(name[0], 0, dirNameBytes, 0, dirNameBytes.length);

        // 检查是否有重名目录
        if (this.entryOperator.findEntryInDirectory(parentDirBlockIndex, pathComponents[pathComponents.length - 1], attribute) != null) {
            return -2;
        }

        // 寻找空闲目录项，支持子目录任意长（但根目录不支持）
        int[] result = this.entryOperator.findFreeEntry(parentDirBlockIndex);
        parentDirBlockIndex = result[0];
        int freeEntryIndex = result[1];
        if (parentDirBlockIndex == 0) {
            return -4; // 根目录不支持任意长
        }
        if (parentDirBlockIndex == -1) {
            return -3; // 没有多余的磁盘块来添加目录项，所以实际上和没有多余磁盘块给目录一样，返回-3
        }

        // 分配一个新的磁盘块给目录
        int dirBlockIndex = this.entryOperator.allocateDiskBlock();
        if (dirBlockIndex == -1) {
            return -3;
        }
        // 初始化目录
        byte[] rootDirBlock = new byte[DiskManager.BLOCK_SIZE];
        Arrays.fill(rootDirBlock, (byte) 0);
        for (int i = 0; i < DiskManager.BLOCK_SIZE; i += 8) {
            rootDirBlock[i] = BlockStatus.EMPTY_ENTRY.getValue();
        }
        this.entryOperator.setContentToEntry(dirBlockIndex, rootDirBlock);

        // 创建新的目录项并添加到父目录中
        Entry newDirEntry = new Entry(dirNameBytes, attribute, (byte) dirBlockIndex);
        this.entryOperator.addEntryToDirectory(parentDirBlockIndex, freeEntryIndex, newDirEntry);

        return 1;
    }

    /**
     * 显示目录内容
     *
     * @param dirName 目录名（含路径）
     * @return 目录内容的 Entry 数组
     */
    public String[][] listDir(String dirName) throws Exception {

        System.out.println(dirName);

        // 获取目录内容
        Entry[] entries = this.entryOperator.listDir(dirName);
        if (entries == null) {
            return null;
        }
        String[][] dirContents = new String[entries.length][];
        // 处理每个目录项
        for (int i = 0; i < entries.length; i++) {
            Entry entry = entries[i];
            String name = new String(entry.getName());
            String type = new String(entry.getType());
            String attribute = String.valueOf(entry.getAttribute());
            String startNum = String.valueOf(entry.getStartNum());
            String diskBlockLength = String.valueOf(entry.getDiskBlockLength());

            if (entry.isDirectory())  // 目录不显示类型
                dirContents[i] = new String[]{name.trim(), attribute, startNum};
            else // 将 name 和 type 合并为单个字符串
                dirContents[i] = new String[]{name.trim() + "." + type, attribute, startNum, diskBlockLength};
        }
        return dirContents;
    }

    /**
     * 删除空目录
     *
     * @param dirName 目录名（含路径）
     * @return 成功返回 1，路径不存在返回-1，非空目录返回 -7，根目录返回 -8
     */
    public int removeDir(String dirName) throws Exception {
        //寻找父目录
        String[] pathComponents = dirName.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        if (parentDirBlockIndex == -1) return -1;

        int startBlockIndex = this.entryOperator.getEntryStartNum(parentDirBlockIndex, pathComponents[pathComponents.length - 1], EntryAttribute.DIRECTORY.getValue());
        if (startBlockIndex == -1) return -1; // 目录不存在

        // 判断非空目录或根目录
        if (!isEmptyDir(startBlockIndex)) {
            return -7;
        }
        if (startBlockIndex == DiskManager.ROOT_DIR_POS){
            return -8;
        }

        // 删除目录项并回收对应空间
        this.entryOperator.dealEntry(pathComponents[pathComponents.length - 1], parentDirBlockIndex, startBlockIndex);
        return 1; // 删除成功
    }

    /**
     * 判断目录是否为空,任意长目录
     *
     * @param dirBlockIndex 目录盘块号
     * @return
     */
    public boolean isEmptyDir(int dirBlockIndex) throws Exception {
        int currentBlockIndex = dirBlockIndex;
        // 遍历当前目录所占的所有盘块
        while (currentBlockIndex != BlockStatus.END_OF_FILE.getValue()) {
            byte[] dirBlockData = this.entryOperator.getContentFromBlock(currentBlockIndex);
            // 遍历当前磁盘块中的所有目录项
            for (int i = 0; i < DiskManager.BLOCK_SIZE; i += EntryStructure.ENTRY_LENGTH.getValue()) {
                byte[] entry = Arrays.copyOfRange(dirBlockData, i, i + EntryStructure.ENTRY_LENGTH.getValue());
                if (entry[0] != BlockStatus.EMPTY_ENTRY.getValue()) {
                    return false;
                }
            }
            // 获取下一个磁盘块号
            currentBlockIndex = this.entryOperator.getNextBlockIndex(currentBlockIndex);
        }
        return true;
    }
}