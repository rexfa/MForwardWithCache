package com.bjcsc.pemc.rex.mf.log;


import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Formatter;
import java.util.logging.FileHandler;
import java.io.IOException;

import com.bjcsc.pemc.rex.mf.util.*;


public class ServerLog
{
	public static Logger log;
	public int LogLimit=102400;


	public ServerLog(String logName,Level logLevel,int LogLimit,String logDir)
	{
		ServerLog(logName,logLevel,LogLimit,logDir);
	}
	public ServerLog(String logName,Level logLevel,int LogLimit,String logDir,String subName)
	{
		ServerLog(logName,logLevel,LogLimit,logDir,subName);
	}

	/**
	 * ��־��������
	 * @param logName
	 * @param logLevel
	 * @param LogLimit
	 * @param logDir
	 */
	private void ServerLog(String logName,Level logLevel,int LogLimit,String logDir)
	{   
		//%h�����û���Ŀ¼
		//%g�����Զ����
		//"/" the local pathname separator
		//"%t" the system temporary directory
		//"%h" the value of the "user.home" system property
		//"%g" the generation number to distinguish rotated logs
		//"%u" a unique number to resolve conflicts
		//"%%" translates to a single percent sign "%"
		log =  Logger.getLogger(logName);
		log.setLevel(logLevel);
		this.LogLimit = LogLimit;
		try
		{
			FileHandler  fileHandler=new FileHandler(logDir+"/"+logName+"/log"+DataUtil.getDateHour()+"_%u_%g.log",LogLimit,1,true);
			fileHandler.setLevel(logLevel);
			fileHandler.setFormatter(new ServerFormatter());
			log.addHandler(fileHandler);
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
	}
	/**
	 * ��־��������
	 * @param logName
	 * @param logLevel
	 * @param LogLimit
	 * @param logDir
	 * @param subName
	 */
	private void ServerLog(String logName,Level logLevel,int LogLimit,String logDir,String subName)
	{   
		//%h�����û���Ŀ¼
		//%g�����Զ����
		//"/" the local pathname separator
		//"%t" the system temporary directory
		//"%h" the value of the "user.home" system property
		//"%g" the generation number to distinguish rotated logs
		//"%u" a unique number to resolve conflicts
		//"%%" translates to a single percent sign "%"
		log =  Logger.getLogger(logName);
		log.setLevel(logLevel);
		this.LogLimit = LogLimit;
		try
		{
			FileHandler  fileHandler=new FileHandler(logDir+"/"+logName+"/"+subName+"/log"+DataUtil.getDateHour()+"_%u_%g.log",LogLimit,1,true);
			fileHandler.setLevel(logLevel);
			fileHandler.setFormatter(new ServerFormatter());
			log.addHandler(fileHandler);
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
	}
	/**
	 * ��־��ʽ
	 * @author Rex Zhang
	 *
	 */
	public class ServerFormatter extends Formatter
	{

		@Override
		public String format(LogRecord record)
		{
			//"Level\t|\tLoggerName\t|\tMessage\t|\n" +
			return  DataUtil.getDateTime()+"Info: " + record.getSourceClassName() +//�����ĸ��������õ�����
				"\t" +
				record.getLevel() + //��SEVERE����WARNING���Ǳ��
			    "\t" + 
			    record.getLoggerName() +//���logger������
			    "\tLOG: " + 
			    record.getMessage() + //��Ϣ����
			    "\r\n";    
		}
	}

	/**
	 * ������ʵ�ַ���
	 * @param logString
	 */
	
	
	
	public void severe(String logString)
	{
		log.severe(logString);
	}
	public void warning(String logString)
	{
		log.warning(logString);
	}
	public void info(String logString)
	{
		log.info(logString);
	}
	public void config(String logString)
	{
		log.config(logString);
	}
	public void fine(String logString)
	{
		log.fine(logString);
	}
	public void finer(String logString)
	{
		log.finer(logString);
	}
	public void finest(String logString)
	{
		log.finest(logString);
	}
	
}
