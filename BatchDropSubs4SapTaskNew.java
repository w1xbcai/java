package com.huawei.bes.om.ctz.order1.batch.dropsubs.business;

import java.io.File;
import java.util.regex.Pattern;

import com.huawei.bes.common.config.ConfigHelper;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
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
    
    @SuppressWarnings("deprecation")
    @Override
    protected boolean initBatchFileDir()
    {
        romoteInputPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.FTP_SRC_PATH");
        remoteInputHisPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.FTP_SRC_HIS_PATH");
        remoteErrorPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.REMOTE_DROPSUBS_ERR_PATH");
        remoteSuccPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.REMOTE_DROPSUBS_SUCC_PATH");
        localTempPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.LOCAL_DROPSUBS_PATH");
        if (ValidateUtils.isEmptyString(romoteInputPath)
                || ValidateUtils.isEmptyString(remoteInputHisPath)
                || ValidateUtils.isEmptyString(remoteErrorPath)
                || ValidateUtils.isEmptyString(remoteSuccPath)
                || ValidateUtils.isEmptyString(localTempPath))
        {
            LOG.error("BatchDropSubs file cfg error");
            return false;
        }
        romoteInputPath = addFileSeparator(romoteInputPath);
        remoteInputHisPath = addFileSeparator(remoteInputHisPath);
        remoteErrorPath = addFileSeparator(remoteErrorPath);
        remoteSuccPath = addFileSeparator(remoteSuccPath);
        localTempPath = addFileSeparator(localTempPath);
        
        if (!FtpServicePool.getFtpConect(ftpType).isDirExist(romoteInputPath, remoteInputHisPath, remoteErrorPath, remoteSuccPath))
        {
            FtpServicePool.getFtpConect(ftpType).makeDir(romoteInputPath, remoteInputHisPath, remoteErrorPath, remoteSuccPath);
        }
        
        if (!FtpServicePool.getFtpConect(ftpType).isDirExist(romoteInputPath, remoteInputHisPath, remoteErrorPath, remoteSuccPath))
        {
            return false;
        }
        
        File temp = new File(localTempPath);
        if (temp.exists() && temp.isDirectory())
        {
            remoteFilePath = romoteInputPath;
            return true;
        }
        return false;
    }
    

    @Override
    public boolean dealExecuteException(TaskItem arg0, Throwable arg1)
    {
        
        return false;
    }

    @Override
    public boolean dealExpired(TaskItem arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean dealTimeout(TaskItem arg0)
    {
        // TODO Auto-generated method stub
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

    @Override
    protected boolean dealRemoteFormatErrorFile(String batchFile)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean formatRemoteFileToBatch(String batchFile,
            StringBuffer batchBuffer, StringBuffer batchDescBuffer)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void dealRemoteErrorFile(String batchFile)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected boolean checkRemoteFile(String batchFile)
    {
        String ftpSrcFilePath = romoteInputPath + batchFile;
//        FtpServicePool.getFtpConect(ftpType).
        if (batchFile.endsWith(".csv")
                && Pattern.matches("\\w{0,50}.csv", batchFile)
                && !ftpSrcFileNameHisList.contains(ftpSrcFileName))
        {
            LOG.debug("file name is ok");
            return false;
        }
        return false;
    }

}
