/**
 * 
 */
package com.huawei.bes.om.ctz.order1.batch.common.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.esotericsoftware.minlog.Log;
import com.huawei.bes.common.exception.BESException;
import com.huawei.bes.common.file.ftp.constant.MsgNo;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * lss 服务器stfp协议 文件处理接口实现
 * @author wWX377030
 * 
 */
public class LssSftpFileHelper implements LssFileService
{

    /**
     * 连接Session
     */
    private Session sftpSession = null;

    /**
     * SFTP功能类
     */
    private JSch jsch = null;

    /**
     * SFTP传输渠道
     */
    private Channel sftpChannel = null;

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#connect(java.lang.String, int,
     * java.lang.String, java.lang.String)
     */
    public void connect(String serverIp, int serverPort, String userName,
            String passWd)
    {
        boolean success = false;
        try
        {
            jsch = new JSch();

            // 获得连接的Session
            sftpSession = jsch.getSession(userName, serverIp, serverPort);

            // 设置Session密码
            sftpSession.setPassword(passWd);

            Properties prop = new Properties();
            prop.setProperty("StrictHostKeyChecking", "no");

            sftpSession.setConfig(prop);

            // session连接
            sftpSession.connect();

            // 打开数据传输渠道
            sftpChannel = sftpSession.openChannel("sftp");

            // 传输渠道连接
            sftpChannel.connect();
            success = true;
        } 
        catch (JSchException ex)
        {
            Log.error("LssSftp connect error");
        } 
        finally
        {
            // 若连接不成功，则关闭连接
            if (!success)
            {
                close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#disconnect()
     */
    public void close()
    {

        // 若传输渠道连接没有关闭，则关闭渠道
        if (null != sftpChannel && sftpChannel.isConnected())
        {
            try
            {
                sftpChannel.disconnect();
            } 
            catch (Exception ex)
            {
                Log.error("close Sftp server error", ex);
            }
        }

        // 若Session连接没有关闭，则关闭Session
        if (null != sftpSession && sftpSession.isConnected())
        {
            try
            {
                sftpSession.disconnect();
            } 
            catch (Exception ex)
            {
                Log.error("sftpSession disconnect Sftp server error", ex);
            }

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#listFileNames(java.lang.String)
     */
    public List<String> listFileNames(String path)
    {
        List<String> fileNameList = new ArrayList<String>();
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        //获取文件名列表
        try
        {
            @SuppressWarnings("unchecked")
            List<LsEntry> fileInfos = channelSftp.ls(path);
            for (LsEntry fileInfo : fileInfos)
            {
                if (fileInfo.getAttrs().isDir())
                {
                    continue;
                }
                
                fileNameList.add(fileInfo.getFilename());
            }
        }
        catch (SftpException ex)
        {
            Log.error("listFileNames Sftp server error", ex);
        }
        
        return fileNameList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#downLoadFile(java.lang.String,
     * java.lang.String)
     */
    public void downLoadFileFromLss(String remoteFile, String localFile)
    {
        BufferedOutputStream output = null;
        try
        {
            output = new BufferedOutputStream(new FileOutputStream(localFile));
            ChannelSftp channelSftp = (ChannelSftp) sftpChannel;
            channelSftp.get(remoteFile, output);
        } 
        catch (Exception ex)
        {
            Log.error("downLoadFile Sftp server error", ex);
        } 
        finally
        {
            // 关闭输入流
            if (null != output)
            {
                try
                {
                    output.close();
                } 
                catch (Exception ex)
                {
                    Log.error("close output Sftp server error", ex);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#moveFile(java.lang.String, java.lang.String)
     */
    public void moveFile(String oldFile, String newFile)
    {
        ChannelSftp channelSftp = (ChannelSftp) sftpChannel;
        // 服务器端文件重命名
        try
        {
            channelSftp.rename(oldFile, newFile);
        } 
        catch (SftpException ex)
        {
            Log.error("moveFile Sftp server error", ex);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#getFileSize(java.lang.String)
     */
    public long getFileSize(String file)
    {
        try
        {
            ChannelSftp channelSftp = (ChannelSftp) sftpChannel;
            SftpATTRS attr = channelSftp.stat(file);
            return attr.getSize();
        } 
        catch (Exception ex)
        {
            Log.error("getFileSize Sftp server error", ex);
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#mkdirIfNotExist(java.lang.String)
     */
    public boolean mkdirIfNotExist(String path)
    {
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        boolean isMkDir = false;
        StringTokenizer s = new StringTokenizer(path, "/");
        StringBuffer pathName = new StringBuffer("");
        try
        {
            do
            {
                if (!s.hasMoreElements())
                {
                    break;
                }
                pathName.append(String.valueOf("/")).append((String)s.nextElement());
                if (!dirExistFlag(pathName.toString()))
                {
                    channelSftp.mkdir(pathName.toString());
                    //TODO::add log
                    channelSftp.chmod(0xFFF, pathName.toString());
                    isMkDir = true;
                }
            } while(true);
            if (isMkDir)
            {
                channelSftp.cd(pathName.toString());
                return true;
            }
        }
        catch (Exception ex)
        {
            Log.error("mkdirIfNotExist Sftp server error", ex);
        }
        return false;
    }
    
    /**
     * 检查ftp某文件夹是否存在。
     * 
     * @param dir 需要检查是否存在的文件夹
     */
    private boolean dirExistFlag(String dir)
    {
        
        boolean flag = false;
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            channelSftp.cd(dir); //切换到对应的目录
            flag = true;
        }
        catch (Exception ex)
        {
            flag = false;
        }
        
        return flag;
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#writeStrToFile(java.lang.String,
     * java.lang.String, boolean)
     */
    public void writeStrToNewFile(String msg, String dstFile)
    {
        ByteArrayInputStream inputStr = new ByteArrayInputStream(msg.getBytes());
        ChannelSftp channelSftp = (ChannelSftp) sftpChannel;
        try
        {
            channelSftp.put(inputStr, dstFile);
        } 
        catch (SftpException ex)
        {
            Log.error("writeStrToNewFile Sftp server error", ex);
        } 
        finally
        {
            // 关闭输入流
            if (null != inputStr)
            {
                try
                {
                    inputStr.close();
                } 
                catch (IOException ex)
                {
                    Log.error("close inputStr Sftp server error", ex);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see filetool.LssFileService#uploadFile(java.lang.String,
     * java.lang.String)
     */
    public void uploadFile(String localFile, String remoteFile)
    {
        InputStream input = null;

        try
        {
            ChannelSftp channelSftp = (ChannelSftp) sftpChannel;

            // 打开输入流
            input = new FileInputStream(localFile);

            // 使用指定了文件路径和文件名的路径上传
            channelSftp.put(input, remoteFile);
            // 记录成功上传记录
//            System.out.println("Uploading success, dstFile=" + remoteFile);
        } 
        catch (Exception ex)
        {
            Log.error("close Sftp server error1 ", ex);
        } 
        finally
        {
            // 关闭输入流
            if (null != input)
            {
                try
                {
                    input.close();
                } 
                catch (IOException ex)
                {
                    Log.error("close Sftp server error2", ex);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see filetool.LssFileService#uploadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void uploadFile(String localPath, String srcName, String remotePath,
            String destName)
    {
        uploadFile(localPath + srcName, remotePath + destName);
    }

    /* (non-Javadoc)
     * @see filetool.LssFileService#downLoadFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void downLoadFile(String remotePath, String srcName,
            String localPath, String destName)
    {
        downLoadFileFromLss(remotePath + srcName, localPath + destName);
        
    }

    /* (non-Javadoc)
     * @see filetool.LssFileService#moveFile(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void moveFile(String oldPath, String oldName, String newPath,
            String newName)
    {
        moveFile(oldPath + oldName, newPath + newName);
        
    }

    /* (non-Javadoc)
     * @see filetool.LssFileService#getFileSize(java.lang.String, java.lang.String)
     */
    public long getFileSize(String path, String name)
    {
        return getFileSize(path + name);
    }

    /* (non-Javadoc)
     * @see filetool.LssFileService#writeStrToNewFile(java.lang.String, java.lang.String, java.lang.String)
     */
    public void writeStrToNewFile(String msg, String path, String name)
    {
        writeStrToNewFile(msg, path + name);
    }

    @Override
    public boolean mkdirIfNotExist(String... path)
    {
        for (String onePath : path)
        {
            if (!mkdirIfNotExist(onePath))
            {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void deleteFile(String file)
    {
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            channelSftp.rm(file);
        }
        catch (SftpException ex)
        {
            throw new BESException(MsgNo.ERR_SFTP_DELETEFILE, ex);
        }
    }

    @Override
    public void deleteFile(String path, String name)
    {
        deleteFile(path + name);
    }

}
