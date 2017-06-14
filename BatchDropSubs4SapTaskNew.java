package com.huawei.bes.om.ctz.order1.batch.dropsubs.business;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import com.huawei.bes.common.config.ConfigHelper;
import com.huawei.bes.common.exception.BESException;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
import com.huawei.bes.common.parse.FileParser;
import com.huawei.bes.common.task.intf.TaskItem;
import com.huawei.bes.common.utils.validate.ValidateUtils;
import com.huawei.bes.om.ctz.order1.batch.common.utils.ExecBatchByFileBase;
import com.huawei.bes.om.ctz.order1.batch.common.utils.FtpServicePool;

public class BatchDropSubs4SapTaskNew extends ExecBatchByFileBase
{
    private static final BesLog LOG = BesLogFactory.getLog(BatchDropSubs4SapTaskNew.class);

    private String romoteInputPath;
    
    private String remoteInputHisPath;
    
    private String remoteErrorPath;
    
    private String remoteSuccPath;
    
    private String localTempPath;
    
    private String localErrorPath;
    
    private List<String> ftpSrcFileNameHisList;
    
    @SuppressWarnings("deprecation")
    @Override
    protected boolean initBatchFileDir()
    {
        romoteInputPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.FTP_SRC_PATH");
        remoteInputHisPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.FTP_SRC_HIS_PATH");
        remoteErrorPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.REMOTE_DROPSUBS_ERR_PATH");
        remoteSuccPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.REMOTE_DROPSUBS_SUCC_PATH");
        localTempPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.LOCAL_DROPSUBS_PATH");
        localErrorPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.LOCAL_ERR_PATH");
        
        if (ValidateUtils.isEmptyString(romoteInputPath)
                || ValidateUtils.isEmptyString(remoteInputHisPath)
                || ValidateUtils.isEmptyString(remoteErrorPath)
                || ValidateUtils.isEmptyString(remoteSuccPath)
                || ValidateUtils.isEmptyString(localTempPath)
                || ValidateUtils.isEmptyString(localErrorPath))
        {
            LOG.error("BatchDropSubs file cfg error");
            return false;
        }
        romoteInputPath = addFileSeparator(romoteInputPath);
        remoteInputHisPath = addFileSeparator(remoteInputHisPath);
        remoteErrorPath = addFileSeparator(remoteErrorPath);
        remoteSuccPath = addFileSeparator(remoteSuccPath);
        localTempPath = addFileSeparator(localTempPath);
        localErrorPath = addFileSeparator(localErrorPath);
        
        if (!FtpServicePool.getFtpConect(ftpType).isDirExist(
                romoteInputPath, remoteInputHisPath, remoteErrorPath, remoteSuccPath, localErrorPath))
        {
            FtpServicePool.getFtpConect(ftpType).makeDir(
                    romoteInputPath, remoteInputHisPath, remoteErrorPath, remoteSuccPath, localErrorPath);
        }
        
        if (!FtpServicePool.getFtpConect(ftpType).isDirExist(
                romoteInputPath, remoteInputHisPath, remoteErrorPath, remoteSuccPath, localErrorPath))
        {
            return false;
        }
        
        ftpSrcFileNameHisList = FtpServicePool.getFtpConect(ftpType).getFileNameList(remoteInputHisPath);
        
        File temp = new File(localTempPath);
        if (temp.exists() && temp.isDirectory())
        {
            remoteFilePath = romoteInputPath;
        }
        else
        {
            if (!temp.mkdir())
            {
                return false;
            }
        }
        
        File err = new File(localTempPath);
        if (err.exists() && err.isDirectory())
        {
            remoteFilePath = romoteInputPath;
        }
        else
        {
            if (!err.mkdir())
            {
                return false;
            }
        }
        
        return true;
    }
    

    @Override
    public boolean dealExecuteException(TaskItem arg0, Throwable arg1)
    {
        
        return false;
    }

    @Override
    public boolean dealExpired(TaskItem arg0)
    {
      
        return false;
    }

    @Override
    public boolean dealTimeout(TaskItem arg0)
    {
        
        return false;
    }

    @Override
    protected void dealOtherThings()
    {
        
        
    }

    @Override
    protected String getLocalFilePath()
    {
        
        return null;
    }

    @Override
    protected void addBatchTaskExtendedInfo()
    {
        
        
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean dealRemoteFormatErrorFile(String batchFile)
    {
        File source = new File(localTempPath + batchFile);  
        File dest = new File(localErrorPath + batchFile);  
        if (!source.renameTo(dest))
        {
            throw new BESException("dealRemoteFormatErrorFile rename file error");
        }
        return true;
    }

    //H;999;009;CO24;1
    //H;2017_GSM;20100000000000004403;009;TSCO064GENER0202;201703171800;NUEVO;1880005547
    @Override
    protected boolean formatRemoteFileToBatch(String batchFile,
            StringBuffer batchBuffer, StringBuffer batchDescBuffer)
    {
        String remoteFile = romoteInputPath + batchFile;
        FtpServicePool.getFtpConect(ftpType).getFile(remoteFile, localTempPath + batchFile);
        List<String> allLines = FileParser.parseFile("|", localTempPath + batchFile);
        String col[] = null;
        String headType = "";
        for (String oneLine : allLines)
        {
            LOG.debug("#BatchDropSubs4SapTask buildFileContent aData:", oneLine);
            col = oneLine.split(";");
            LOG.debug("#BatchDropSubs4SapTask buildFileContent col length:", col.length);
            if (col.length == 8)
            {
                String splitStr = "|";
                batchBuffer.append(col[3]).append(splitStr).append(col[7]);
                batchBuffer.append(File.separator);
                if (!Pattern.matches("\\d{9-11}", col[7])
                        || !ValidateUtils.equals(headType, col[3]))
                {
                    return false;
                }
            }
            else if (col.length == 5)
            {
                batchDescBuffer.append(oneLine);
                headType = col[2];
            }
            else
            {
                return false;
            }
            if ((null == col[0]) || !ValidateUtils.equals(col[0], "H"))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 移到本地错误文件,还是走统一流程.
     */
    @Override
    protected void dealRemoteErrorFile(String batchFile)
    {
        FtpServicePool.getFtpConect(ftpType).getFile(romoteInputPath + batchFile, localErrorPath + batchFile);
    }

    @Override
    protected boolean checkRemoteFile(String batchFile)
    {
        if (Pattern.matches("\\w{0,50}.csv", batchFile)
                && ((null == ftpSrcFileNameHisList)
                        || !ftpSrcFileNameHisList.contains(batchFile)))
        {
            LOG.debug("file name is not ok");
            return false;
        }
        return true;
    }

}
