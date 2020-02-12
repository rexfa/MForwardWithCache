package com.bjcsc.pemc.rex.mf.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//import org.apache.log4j.Appender;
//import org.apache.log4j.FileAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
//import org.apache.log4j.SimpleLayout;

import com.bjcsc.pemc.rex.mf.config.LogConfig;
import com.bjcsc.pemc.rex.mf.config.ServerConfig;

public class LogUtil
{
	public static void logFlieNameSet(Logger log,ServerConfig config)
	{
		/*
		 * ��̬������־Ŀ¼
		 */
		Layout layout = new PatternLayout(config.LogFormat); 
		RollingFileAppender appender=null;
		try
		{
			DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			Date date1 = new Date();
			appender = new RollingFileAppender(layout, config.LogDir+LogConfig.MainLog+format1.format(date1)+"_log.log");
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		appender.setMaxFileSize(config.LogMax);//�����ļ���С �������� ��������ļ�
		appender.setMaxBackupIndex(config.LogFileBack);//���ı����ļ���Ŀ
		log.addAppender(appender);
	}
	/**
	 * ����log�����ļ���С������¼
	 * @param log
	 * @param config
	 * @param Prefix
	 */
	public static void logRollingFlieSet(Logger log,ServerConfig config,String Prefix)
	{
		/*
		 * ��̬������־Ŀ¼
		 */
		Layout layout = new PatternLayout(config.LogFormat); 
		RollingFileAppender appender=null;
		try
		{
			//DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			//Date date1 = new Date();
			//appender = new RollingFileAppender(layout, config.LogDir+LogConfig.ClinetLog+Prefix+"/"+format1.format(date1)+"_log.log");
			appender = new RollingFileAppender(layout, config.LogDir+LogConfig.ClinetLog+Prefix+"/"+"client.log");
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		appender.setMaxFileSize(config.LogMax);//�����ļ���С �������� ��������ļ�
		appender.setMaxBackupIndex(config.LogFileBack);//���ı����ļ���Ŀ
		log.addAppender(appender);
	}
	/**
	 * ��־�ļ��������ڹ���
	 * @param log
	 * @param config
	 * @param Prefix
	 */
	public static void logDailyRollingFileSet(Logger log,ServerConfig config,String Prefix)
	{
		/*
		 * ��̬������־Ŀ¼
		 */
		PatternLayout layout = new PatternLayout(config.LogFormat); 
		DailyRollingFileAppender appender=null;
		try
		{
			appender = new DailyRollingFileAppender(layout, config.LogDir+LogConfig.ClinetLog+Prefix+"/"+"client.log", ".yyyy-MM-dd");
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		log.addAppender(appender);
	}
	/**
	 * ��־�ļ��������ڹ���
	 * @param log
	 * @param config
	 * @param Prefix
	 * @param Filename
	 */
	public static void logDailyRollingFileSet(Logger log,ServerConfig config,String Prefix,String Filename)
	{
		/*
		 * ��̬������־Ŀ¼
		 */
		PatternLayout layout = new PatternLayout(config.LogFormat); 
		DailyRollingFileAppender appender=null;
		try
		{
			appender = new DailyRollingFileAppender(layout, config.LogDir+LogConfig.ClinetLog+Prefix+"/"+Filename+".log", ".yyyy-MM-dd");
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		log.addAppender(appender);
	}
}
