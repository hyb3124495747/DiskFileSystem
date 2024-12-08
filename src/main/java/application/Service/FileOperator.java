package application.Service;

import application.Entity.Entry;
import application.Enum.EntryAttribute;
import application.Enum.EntryStructure;

import java.util.Arrays;

/**
 * 文件操作，提供文件的操作接口：创建文件、读文件、写文件、关闭文件、删除文件等
 */
public class FileOperator {
    private static final byte EOF = (byte) '#'; // 文件结束标志

    private final EntryOperator entryOperator; // 目录项操作

    public FileOperator(EntryOperator entryOperator) {
        this.entryOperator = entryOperator;
    }

    /**
     * 创建新文件
     *
     * @param fileNameAndType 文件名（含Type）
     * @param attribute       文件属性
     * @return 1:成功； 0:只读属性无法创建； -1:路径不存在； -2:文件已存在； -3:无空闲磁盘块；-4：根目录已满；-5:文件名不合法
     */
    public int create_file(String fileNameAndType, byte attribute) throws Exception{
        // 检查文件名（含type）
        byte[][] nameAndType = Tools.checkNameAndType(fileNameAndType, EntryAttribute.DIRECTORY.isEqual(attribute));
        if (nameAndType == null) {
            return -5;
        }
        // 分离文件名和类型
        byte[] fileNameBytes = new byte[EntryStructure.NAME_LENGTH.getValue()];
        byte[] fileType = new byte[EntryStructure.TYPE_LENGTH.getValue()];
        System.arraycopy(nameAndType[0], 0, fileNameBytes, 0, fileNameBytes.length);
        System.arraycopy(nameAndType[1], 0, fileType, 0, fileType.length);

        // 文件属性如果是只读性质则不能建立
        if (EntryAttribute.READ_ONLY.isEqual(attribute)) {
            return 0;
        }

        // 解析文件路径，获取父目录盘块号，以检查父目录是否存在
        String[] pathComponents = fileNameAndType.split("/"); // ["", "path", "to", "your", "file.txt"]
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1)); // "/path/to/your"
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        if (parentDirBlockIndex == -1) {
            return -1;
        }

        // 检查是否有重名文件
        if (this.entryOperator.findEntryInDirectory(parentDirBlockIndex, pathComponents[pathComponents.length - 1],attribute)!= null) {
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
            return -3; // 没有多余的磁盘块来添加目录项，所以实际上和没有多余磁盘块给文件一样，返回-3
        }

        // 分配一个新的磁盘块给文件
        int fileBlockIndex = this.entryOperator.allocateDiskBlock();
        if (fileBlockIndex == -1) {
            return -3;
        }
        // 在这个新文件中写入文件结束符
        this.entryOperator.setContentToEntry(fileBlockIndex, new byte[]{EOF});

        // 创建新的目录项并添加到父目录中
        Entry newFileEntry = new Entry(fileNameBytes, fileType, attribute, (byte) fileBlockIndex, (byte) 1);
        this.entryOperator.addEntryToDirectory(parentDirBlockIndex, freeEntryIndex, newFileEntry);

        // 最后填写已打开文件表

        return 1;
    }
}
