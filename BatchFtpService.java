import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.jcraft.jsch.SftpATTRS;

/**
 * 批量文件处理接口
 * @author wWX377030
 *
 * @date  2017-6-8
 */
public interface BatchFtpService
{
    /**
     * 打开连接
     * @param serverIp
     * @param serverPort
     * @param userName
     * @param passWd
     */
    void connect(String serverIp, int serverPort, String userName,String passWd);
    
    /**
     * 关闭连接
     */
    void close();

    /**
     * 上传文件
     * @param local
     * @param remote
     */
    void putFile(String local, String remote);
    
    /**
     * 上传文件
     * @param localPath
     * @param localName
     * @param remotePath
     * @param remoteName
     */
    void putFile(String localPath, String localName, String remotePath, String remoteName);
    
    /**
     * 上传文件
     * @param localFile
     * @param remoteName
     */
    void putFile(File localFile, String remoteName);
    
    /**
     * 上传文件
     * @param localFile
     * @param remotePath
     * @param remoteName
     */
    void putFile(File localFile, String remotePath, String remoteName);

    /**
     * 下载文件
     * @param remote
     * @param local
     */
    void getFile(String remote, String local);
    
    /**
     * 下载文件 
     * @param remotePath
     * @param remoteName
     * @param localPath
     * @param localName
     */
    void getFile(String remotePath, String remoteName, String localPath, String localName);

    /**
     * 新建文件
     * @param content
     * @param file
     */
    void newFile(String content, String file);
    
    /**
     * 新建文件
     * @param content
     * @param path
     * @param fileName
     */
    void newFile(String content, String path, String fileName);

    /**
     * 读文件
     * @param file
     * @param readCount
     * @return
     */
    String readFile(String file, int readCount);
    
    /**
     * 读文件
     * @param path
     * @param fileName
     * @param content
     * @return
     */
    String readFile(String path, String fileName, String content);

    /**
     * 移动文件
     * @param src
     * @param dest
     */
    void moveFile(String src, String dest);
    
    /**
     * 移动文件
     * @param srcPath
     * @param srcName
     * @param destPath
     * @param destName
     */
    void moveFile(String srcPath, String srcName, String destPath, String destName);

    /**
     * 删除文件
     * @param file
     */
    void deleteFile(String file);
    
    /**
     * 删除文件
     * @param path
     * @param name
     */
    void deleteFile(String path, String name);

    /**
     * 新建文件夹
     * @param dir
     */
    void makeDir(String dir);

    /**
     * 删除文件夹
     * @param dir
     */
    void deleteDir(String dir);

    /**
     * 列出文件夹下的所有文件的名称
     * @param dir
     * @return
     */
    List<String> listFileNames(String dir);

    /**
     * 取文件大小信息
     * @param file
     * @return
     */
    long getFileSize(String file);
    
    /**
     * 取文件大小信息
     * @param file
     * @return
     */
    long getFileSize(String path, String name);

    /**
     * 判断是否为大文件
     * @param file
     * @param size
     * @return
     */
    boolean isBigFile(String file, long size);
    
    /**
     * 判断是否为大文件
     * @param path
     * @param name
     * @param size
     * @return
     */
    boolean isBigFile(String path, String name, long size);

    /**
     * 复制文件
     * @param src
     * @param dest
     */
    void copyFile(String src, String dest);
    
    /**
     * 复制文件
     * @param srcPath
     * @param srcName
     * @param destPath
     * @param destName
     */
    void copyFile(String srcPath, String srcName, String destPath, String destName);

    //其他.
    /**
     * 获取输入流
     * @param file
     * @return
     */
    InputStream getInputStream(String file);

    /**
     * 获取输出流
     * @param file
     * @return
     */
    OutputStream getOutputStream(String file);

    /**
     * 获取SftpATTRS
     * @param file
     * @return
     */
    SftpATTRS getSftpATTRS(String file);

}
