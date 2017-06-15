package com.huawei.bes.om.ctz.order1.batch.dropsubs.business;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.huawei.bes.common.adapter.bdf.sequence.SequenceUtil;
import com.huawei.bes.common.batchtask.configbatch.app.business.bo.BatchDefineBOService;
import com.huawei.bes.common.config.ConfigHelper;
import com.huawei.bes.common.context.ContextHelper;
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
import com.huawei.bes.om.ctz.order1.batch.common.utils.LssBatchBase;
import com.huawei.bes.om.ctz.order1.batch.createsubs.business.bo.OmBatchOtherSysBOImpService;
import com.huawei.bes.om.ctz.order1.batch.dropsubs.constant.Batch4DropSubsConsts;
import com.huawei.bes.sm.orgstaff.domain.employee.view.SMEmployeeDTO;
import com.huawei.bes.sm.orgstaff.intf.SMOrgIntfBOService;
import com.huawei.soa.bdf.integration.util.ServiceHelper;
import com.huawei.soa.daf.util.DafJsonUtils;

/**
 * SAP批销
 * @author wWX377030
 *
 */
public class LssBatchDropSubs extends LssBatchBase
{
    
    private static final BesLog LOG = BesLogFactory.getLog(LssBatchBase.class);
    
    @Override
    protected void dealSubClassAfterBatch(String batchFile, long batchNo,
            StringBuffer desc, StringBuffer succContent,
            StringBuffer errorContent)
    {
        OmBatchOtherSysBOImpService omBatchOtherSys = (OmBatchOtherSysBOImpService) ServiceHelper
                .getService(OmBatchOtherSysBOImpService.SERVICE_NAME);
        List<OmBatchOtherSysExEntityDTO> ombatch = new ArrayList<OmBatchOtherSysExEntityDTO>();
        OmBatchOtherSysExEntityDTO omBatchOtherSysExEntity = new OmBatchOtherSysExEntityDTO();
        omBatchOtherSysExEntity.setBatch4othersysid(new BigDecimal(SequenceUtil.next(OmBatchOtherSysExEntity.VO_TYPE_NAME)));
        omBatchOtherSysExEntity.setBatchno(new BigDecimal(batchNo));
        omBatchOtherSysExEntity.setFilename(batchFile);
        omBatchOtherSysExEntity.setBusinesscode(Batch4DropSubsConsts.BATCH_BUSINESS_CODE);
        omBatchOtherSysExEntity.setStatus("0");// 0 初始化状态
        omBatchOtherSysExEntity.setStatusdate(DateTimeHelper.getNowDate());
        omBatchOtherSysExEntity.setCreatedate(DateTimeHelper.getNowDate());
        omBatchOtherSysExEntity.setExfield1(desc.toString());
        ombatch.add(omBatchOtherSysExEntity);
        omBatchOtherSys.insertOmBatchOtherSys(ombatch);
    }

    @Override
    protected String buildBatchContext()
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
        String temp = "{\"BusinessCode\":\"" + this.businessCode + "\",\"ReqHeader\":";
        String temp2 = "}";
        StringBuffer retString = new StringBuffer();
        retString.append(temp).append(batchServiceJsonString).append(temp2);
        LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext retString:", retString.toString());
        return retString.toString();
    }
    
    @SuppressWarnings("deprecation")
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

    @Override
    protected void dealFormatErrorContent(String batchFile,
            StringBuffer errorContent)
    {
        String timeStr = com.huawei.soa.daf.util.DateTimeHelper.toStringDateTimeFormat(DateTimeHelper.getNowDate(), "ddMMYYYYhhmmss");
        batchFile = timeStr + "_" + batchFile;
        sftpHelper.writeStrToNewFile(errorContent.toString(), remoteInputHisPath, batchFile);
    }

    @Override
    protected int formatToBatchFile(String batchFile, StringBuffer desc,
            StringBuffer succContent, StringBuffer errorContent)
    {
        List<String> allLines = FileParser.parseFile("|", batchFile);
        String col[] = null;
        String headType = "";
        int idx = 0;
        int succCount = 0;
        for (String oneLine : allLines)
        {
            col = oneLine.split(";");
//            LOG.debug("oneLine:" , oneLine , " lenght: " , col.length);
            if (!Pattern.matches("[HL]{1}", col[0]))
            {
                errorContent.append(oneLine).append(System.getProperty("line.separator"));
            }
            if (col.length == 8)
            {
                String splitStr = "|";
                succContent.append(col[3]).append(splitStr).append(col[7]);
                succContent.append(System.getProperty("line.separator"));
                if (!Pattern.matches("\\d{5,12}", col[7])
                        || !ValidateUtils.equals(headType, col[3]))
                {
                    errorContent.append(oneLine).append(System.getProperty("line.separator"));
                }
                else
                {
                    succCount++;
                }
            }
            else if ((idx == 0) && (col.length == 5))
            {
                desc.append(oneLine);
                headType = col[2];
            }
            else
            {
                errorContent.append(oneLine)
                            .append(System.getProperty("line.separator"));
            }
            idx++;
        }
        if (errorContent.length() > 0)
        {
            succCount = -1;
            succContent = new StringBuffer();
        }
        return succCount;
    }

    @Override
    protected void dealRemoteErrorFile(String batchFile)
    {
//        sftpHelper.moveFile(remoteInputPath, batchFile, remoteInputHisPath, batchFile);
        String timeStr = com.huawei.soa.daf.util.DateTimeHelper.toStringDateTimeFormat(DateTimeHelper.getNowDate(), "ddMMYYYYhhmmss");
        String errName = timeStr + "_" + batchFile;
        sftpHelper.downLoadFile(remoteInputPath, batchFile, localErrorPath, errName);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean checkBatchFileDir()
    {
        remoteInputPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.FTP_SRC_PATH");
        remoteInputHisPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.FTP_SRC_HIS_PATH");
        localTempPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.LOCAL_DROPSUBS_PATH");
        localErrorPath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.LOCAL_ERR_PATH");
        
        LOG.debug("file path remoteInputPath ", remoteInputPath);
        LOG.debug("file path remoteInputHisPath ", remoteInputHisPath);
        LOG.debug("file path localTempPath ", localTempPath);
        LOG.debug("file path localErrorPath ", localErrorPath);
        
        remoteInputPath = addFileSeparator(remoteInputPath);
        remoteInputHisPath = addFileSeparator(remoteInputHisPath);
        localTempPath = addFileSeparator(localTempPath);
        localErrorPath = addFileSeparator(localErrorPath);
        
        if (!sftpHelper.mkdirIfNotExist(remoteInputPath, remoteInputHisPath))
        {
            return false;
        }
        
        if (!mklocaldirIfNotExist(localTempPath, localErrorPath))
        {
            return false;
        }

        return true;
    }

    @Override
    protected boolean checkBatchDefine(TaskItem taskItem)
    {
        BatchDefineBOService batchDefineBOService = (BatchDefineBOService) ServiceHelper
                .getService(BatchDefineBOService.SERVICE_NAME);
        defDto = batchDefineBOService.queryDefineVOById(Batch4DropSubsConsts.BATCH_TEMPLATE_IDLE_ID);
        batchFileStruct = batchDefineBOService
                .queryBatchDefStru(Batch4DropSubsConsts.BATCH_TEMPLATE_IDLE_ID);
        if ((null == defDto) || (null == batchFileStruct))
        {
            return false;
        }
        return true;
    }
    
    /**
     * 初始化上下文信息
     * @param taskItem
     */
    @SuppressWarnings("deprecation")
    protected void initCommonContext(TaskItem taskItem)
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
    protected void dealLocalErrorFile(String batchFile)
    {
        if (!new File(localTempPath + batchFile)
            .renameTo(new File(localErrorPath + batchFile)))
        {
            LOG.debug("dealLocalErrorFile failed ", localTempPath, batchFile);
        }
    }

    @Override
    protected void dealSubClassBeforeBatch(String batchFile)
    {
        return;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean checkOtherCfg()
    {
        maxFileSize = 10 * 1024 * 1024;
        maxFileLines = ConfigHelper.getShareConfig().getInt("OM.BATCH.TLF.DROPSUBS_LINECOUNT_MAX", 10000);
        minFileSize = 5;
        return true;
    }

}
