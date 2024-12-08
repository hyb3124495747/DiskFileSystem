package application;

import application.Enum.EntryAttribute;
import application.Manager.FileSystem;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        start_test();
        FileSystem fileSystem = new FileSystem();
        System.out.println("test1----------------------------------");
        test1(fileSystem);
        System.out.println("test2----------------------------------");
        test2(fileSystem);
        System.out.println("test3----------------------------------");
        test3(fileSystem);
        System.out.println("test2----------------------------------");
        test2(fileSystem);
        System.out.println("test4----------------------------------");
        test4(fileSystem);
        System.out.println("test2----------------------------------");
        test2(fileSystem);
        System.out.println("test5----------------------------------");
        test5(fileSystem);
        System.out.println("----------------------------------");
    }

    //测试用例5，删除文件夹/abc/
    private static void test5(FileSystem fileSystem) {
        try {
            int res = fileSystem.removeDir("/abc");
            System.out.println(res);
            fileSystem.debug_rootDir();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 测试用例4，创建文件夹abc，并在/abc里创建个8文件夹：a,b,c,d,e,f,g,h
    // 查看/abc
    // 再在/abc/g中创建10个文件夹：a,b,c,d,e,f,g,h,i,j
    private static void test4(FileSystem fileSystem) {
        try {
            fileSystem.createDir("/abc", EntryAttribute.DIRECTORY.getValue());
            for (int i = 0; i < 8; i++) {
                fileSystem.createDir("/abc/" + (char) ('a' + i), EntryAttribute.DIRECTORY.getValue());
            }

            String[][] res = fileSystem.listDir("/abc");
            for (String[] strings : res) {
                for (String string : strings) {
                    System.out.print(string + " ");
                }
                System.out.println();
            }

            for (int i = 0; i < 10; i++) {
                fileSystem.createDir("/abc/g/" + (char) ('a' + i), EntryAttribute.DIRECTORY.getValue());
            }
            fileSystem.debug_printDisk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 测试用例3，删除目录
    private static void test3(FileSystem fileSystem) {
        try {
            int res = fileSystem.removeDir("/2");
            System.out.println(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 测试用例2，展示目录内容
    private static void test2(FileSystem fileSystem) {
        String[][] res = fileSystem.listDir("/");
        for (String[] strings : res) {
            for (String string : strings) {
                System.out.print(string + " ");
            }
            System.out.println();
        }
    }

    // 测试用例1，创建文件和文件夹
    private static void test1(FileSystem fileSystem) {
        // 创建文件
        String res1 = fileSystem.createFile("1.tx", EntryAttribute.NORMAL_FILE.getValue());
        System.out.println(res1);
        // 创建文件夹
        String res2 = fileSystem.createDir("2", EntryAttribute.DIRECTORY.getValue());
        System.out.println(res2);

        fileSystem.debug_printDisk();
        fileSystem.debug_rootDir();
    }

    private static void start_test() {
        String filePath = "D:\\code\\Java\\OS\\DiskFileSystem\\disk.dat";
        File file = new File(filePath);

        // 检查文件是否存在并且可以删除
        if (file.exists() && !file.delete()) {
            System.out.println("Error deleting file: " + filePath);
        } else {
            System.out.println("File deleted successfully.");
        }
    }
}
