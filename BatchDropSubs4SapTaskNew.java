package com.huawei.bes.om.ctz.order1.batch.dropsubs.business;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.huawei.bes.common.adapter.bdf.sequence.SequenceUtil;
import com.huawei.bes.common.batchtask.configbatch.app.model.vo.BatchDefineDTO;
import com.huawei.bes.common.config.ConfigHelper;
import com.huawei.bes.common.context.ContextHelper;
import com.huawei.bes.common.exception.BESException;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
import com.huawei.bes.common.parse.FileParser;
import com.huawei.bes.common.task.intf.TaskItem;
import com.huawei.bes.common.utils.time.DateTimeHelper;
import com.huawei.bes.common.utils.validate.ValidateUtils;
import com.huawei.bes.oh.base.batch.schema.app.model.xmlvo.batchservice.BatchServiceInfoXMLVO;
import com.huawei.bes.oh.base.batch.schema.app.model.xmlvo.batchtype.BatchExtParamInfoXMLVO;
import com.huawei.bes.oh.biz.orderex4telecom.constant.OrderConstant.OrderCreateConsts;
import com.huawei.bes.oh.biz.orderex4telecom.utils.SMEmployeeHelper;
import com.huawei.bes.om.ctz.order.cz4colombiamobile1.vo.OmBatchOtherSysExEntity;
import com.huawei.bes.om.ctz.order.cz4colombiamobile1.vo.OmBatchOtherSysExEntityDTO;
import com.huawei.bes.om.ctz.order1.batch.common.utils.ExecBatchByFileBase;
import com.huawei.bes.om.ctz.order1.batch.common.utils.FtpServicePool;
import com.huawei.bes.om.ctz.order1.batch.createsubs.business.bo.OmBatchOtherSysBOImpService;
import com.huawei.bes.om.ctz.order1.batch.dropsubs.constant.Batch4DropSubsConsts;
import com.huawei.bes.sm.orgstaff.domain.employee.view.SMEmployeeDTO;
import com.huawei.bes.sm.orgstaff.intf.SMOrgIntfBOService;
import com.huawei.soa.bdf.integration.util.ServiceHelper;
import com.huawei.soa.daf.util.DafJsonUtils;

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


    @Override
    protected void initBatchData()
    {
        
        
    }

    /**
     * 初始化上下文信息
     * @param taskItem
     */
    private void initCommonContext(TaskItem taskItem)
    {
        //取定时任务配置的BeId和BizBeId
        ContextHelper.getContextAccessor().setBeId(taskItem.getBeId());
        ContextHelper.getContextAccessor().setBizBeId(taskItem.getBeId());
        //其他上下文信息取默认操作员的，基线为101
        String operator = ConfigHelper.getShareConfig().getString(OrderCreateConsts.DEFAULT_OPER_KEY);
        ContextHelper.getContextAccessor().setOperator(operator);
        SMEmployeeDTO employeeVO = SMEmployeeHelper.getEmployee(new BigDecimal(operator));
        String channelType = this.getChannelType(employeeVO.getOrgId());
        LOG.debug("#BatchDropSubs4SapTask channelType is", channelType);
        ContextHelper.getContextAccessor().setChannelId(channelType);
        ContextHelper.getContextAccessor().setChannelType(channelType);
        ContextHelper.getContextAccessor().setDeptId(String.valueOf(employeeVO.getOrgId()));
        ContextHelper.getContextAccessor().setBizDeptId(String.valueOf(employeeVO.getOrgId()));
        ContextHelper.getContextAccessor().setLoginId(employeeVO.getEmployeeCode());
        ContextHelper.getContextAccessor().setBeCode(taskItem.getBeId());
        if (null != employeeVO.getbRegionId())
        {
            ContextHelper.getContextAccessor().setRegionId(employeeVO.getbRegionId().toString());
        }
        if (null != Locale.getDefault())
        {
            ContextHelper.getContextAccessor().setLocale(Locale.getDefault().toString());
        }
    }
    
    /**
     * 根据orgId查询渠道类型
     * @param orgId
     * @return
     */
    private String getChannelType(BigDecimal orgId)
    {
        LOG.debug("#getChannelType orgId ", orgId);
        SMOrgIntfBOService service = (SMOrgIntfBOService) ServiceHelper.getService(SMOrgIntfBOService.SERVICE_NAME);
        String salesChannel = service.querySalesChannelByOperOrOrgId(null, orgId);
        LOG.debug("#getChannelType return ", salesChannel);
        return salesChannel;
    }
    
    private void recordBatchInfo(String uploadLssFileName, String[] fileParseResult, long taskId)
    {
        OmBatchOtherSysBOImpService omBatchOtherSys = (OmBatchOtherSysBOImpService) ServiceHelper
                .getService(OmBatchOtherSysBOImpService.SERVICE_NAME);
        List<OmBatchOtherSysExEntityDTO> ombatch = new ArrayList<OmBatchOtherSysExEntityDTO>();
        OmBatchOtherSysExEntityDTO omBatchOtherSysExEntity = new OmBatchOtherSysExEntityDTO();
        omBatchOtherSysExEntity.setBatch4othersysid(new BigDecimal(SequenceUtil
                .next(OmBatchOtherSysExEntity.VO_TYPE_NAME)));
        omBatchOtherSysExEntity.setBatchno(new BigDecimal(taskId));
        omBatchOtherSysExEntity.setFilename(uploadLssFileName.replaceFirst(".txt", ".csv"));
        omBatchOtherSysExEntity.setBusinesscode(Batch4DropSubsConsts.BATCH_BUSINESS_CODE);
        omBatchOtherSysExEntity.setStatus("0");
        omBatchOtherSysExEntity.setStatusdate(DateTimeHelper.getNowDate());
        omBatchOtherSysExEntity.setCreatedate(DateTimeHelper.getNowDate());
        omBatchOtherSysExEntity.setExfield1(fileParseResult[0]);
        ombatch.add(omBatchOtherSysExEntity);
        omBatchOtherSys.insertOmBatchOtherSys(ombatch);
    }
    
    private String buildDefDtoContext(BatchDefineDTO defDto)
    {
        LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext begin");
        BatchServiceInfoXMLVO batchServiceInfo = new BatchServiceInfoXMLVO();
        batchServiceInfo.setBeId(new BigDecimal(ContextHelper.getContextAccessor().getBeId()));
        batchServiceInfo.setCreateOperId(new BigDecimal(ContextHelper.getContextAccessor().getOperator()));
        batchServiceInfo.setBusinessCode(Batch4DropSubsConsts.BATCH_BUSINESS_CODE);
        batchServiceInfo.setRegionId(new BigDecimal(ContextHelper.getContextAccessor().getBeId()));
        batchServiceInfo.setCreateDeptId(new BigDecimal(ContextHelper.getContextAccessor().getDeptId()));
        batchServiceInfo.setSalesDepartId(new BigDecimal(ContextHelper.getContextAccessor().getDeptId()));

        this.buildBatchExtParamInfo(batchServiceInfo);

        if (LOG.isDebugEnabled())
        {
            LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext batchServiceInfo", JSON.toJSONString(batchServiceInfo));
        }
        String batchServiceJsonString = DafJsonUtils.marshal(batchServiceInfo);
        LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext batchServiceJsonString:", batchServiceJsonString);
        String temp = "{\"BusinessCode\":\"" + defDto.getBusiCode() + "\",\"ReqHeader\":";
        String temp2 = "}";
        StringBuffer retString = new StringBuffer();
        retString.append(temp).append(batchServiceJsonString).append(temp2);
        LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext retString:", retString.toString());
        return retString.toString();
    }

    private void buildBatchExtParamInfo(BatchServiceInfoXMLVO batchServiceInfo)
    {
        LOG.debug("#BatchDropSubs4SapTask buildBatchExtParamInfo begin");
        List<BatchExtParamInfoXMLVO> batchExtParamInfo = new ArrayList<BatchExtParamInfoXMLVO>();
        batchServiceInfo.setBatchExtParamInfo(batchExtParamInfo);

        BatchExtParamInfoXMLVO sendSms = new BatchExtParamInfoXMLVO();
        batchExtParamInfo.add(sendSms);
        sendSms.setParamName("SendSms");
        sendSms.setParamValue(ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.DROPSUBS_SMS_FLAG"));

        BatchExtParamInfoXMLVO idle = new BatchExtParamInfoXMLVO();
        batchExtParamInfo.add(idle);
        idle.setParamName("idle");
        idle.setParamValue("Y");

        BatchExtParamInfoXMLVO accessChanelType = new BatchExtParamInfoXMLVO();
        batchExtParamInfo.add(accessChanelType);
        accessChanelType.setParamName("AccessChanelType");
        accessChanelType.setParamValue(ContextHelper.getContextAccessor().getChannelType());
        LOG.debug("#BatchDropSubs4SapTask buildBatchExtParamInfo end");
    }
}
