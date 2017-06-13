package com.huawei.bes.om.ctz.order1.batch.common.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FtpServicePool 
{
    private static final int POOL_MAX_SIZE = 3;
    
    private static Map<String, String[]> conParas = new HashMap<String, String[]>(); 
    
    private static Map<String, List<BatchFtpService>> cons = new HashMap<String, List<BatchFtpService>>();
    
    public static BatchFtpService getFtpConect(String key)
    {
        if (cons.containsKey(key) && (cons.get(key).size() > 0))
        {
            return cons.get(key).remove(0);
        }
        if (conParas.containsKey(key))
        {
            String[] paras = conParas.get(key);
            if (addFtpConnect(key, paras[0], 
                        Integer.valueOf(paras[1]), paras[2], paras[3]))
            {
                return cons.get(key).remove(0);
            }
        }
        return null;
    }
    
    public static void closeFtpConnect(String key, BatchFtpService ser)
    {
        cons.get(key).add(ser);
    }
    
    public static boolean addFtpConnect(String key, String ip,
                int port, String name, String pwd)
    {
        if (cons.containsKey(key))
        {
            return true;
        }
        if (cons.get(key).size() >= POOL_MAX_SIZE)
        {
            return false;
        }
        BatchFtpService ser = new BatchSftpServiceImpl();
        ser.connect(ip, port, name, pwd);
        cons.get(key).add(ser);
        return true;
    }
    
}
