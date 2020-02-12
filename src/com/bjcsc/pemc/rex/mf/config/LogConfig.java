package com.bjcsc.pemc.rex.mf.config;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class LogConfig
{
	public final static String  MainLog="/main/";
	public final static String  ServerLog="/server/";
	public final static String  CSclientLog="/csclient/";
	public final static String  ClinetLog="/server/client/";
	public static void Traversal()
	{
		Properties cfg = LoadConfig.load("mflogset.xml");
	
		//Properties �̳��� Hashtable��entrySet()��Hashtable�ķ�����
		//���ش� Hashtable ���������ļ��� Set ��ͼ���� collection ��ÿ��Ԫ�ض���һ�� Map.Entry
		Iterator it=cfg.entrySet().iterator();
		while(it.hasNext())
		{
		    @SuppressWarnings("unchecked")
			Map.Entry<String,String> entry=(Map.Entry<String,String>)it.next();
		    String key = entry.getKey();
		    String value = entry.getValue();
		    System.out.println(key +":"+value);
		}
	}
	public static Iterator getAllLogSet()
	{
		Properties cfg = LoadConfig.load("mflogset.xml");
		return cfg.entrySet().iterator();
	}
}
