package com.bjcsc.pemc.rex.mf.config;
import java.util.Properties;
import java.util.Iterator;
import java.util.Map;
public final class ClientInfo
{
	public static boolean isAuthorized(String username,String password)
	{
		Properties cfg = LoadConfig.load("mfclientinfo.xml");
		return password.equals(cfg.getProperty(username));
	}
	public static void Traversal()
	{
		Properties cfg = LoadConfig.load("mfclientinfo.xml");
	
		//Properties �̳��� Hashtable��entrySet()��Hashtable�ķ�����
		//���ش� Hashtable ���������ļ��� Set ��ͼ���� collection ��ÿ��Ԫ�ض���һ�� Map.Entry
		Iterator it=cfg.entrySet().iterator();
		while(it.hasNext())
		{
		    @SuppressWarnings("unchecked")
			Map.Entry<String,String> entry=(Map.Entry<String,String>)it.next();
		    String key = entry.getKey();
		    String value = entry.getValue();
		   //System.out.println(key +":"+value);
		}
	}
	public static Iterator getAllClientInfo()
	{
		Properties cfg = LoadConfig.load("mfclientinfo.xml");
		return cfg.entrySet().iterator();
	}
}
