package com.bjcsc.pemc.rex.mf.config;

import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

//import com.bjcsc.pemc.rex.mf.server.ClientTransmissionManager;
public final class ServerConfig
{
	public String ServerName;
	public int ServerPort,ServerSoTimeOut;
	public int MaxConnections;
	public int MaxErrors;
	public int ConnectionTimeout;
	public String LogName;
	public int LogFileBack;
	public String LogMax;
	public String LogDir;
	public String LogFormat;
	public String AIUserName;
	public String AIPassword;
	public String AIIP;
	public int AIPort;
	public int ClientCache;
	public String JDBCDriverClassName;
	public String JDBCUrl;
	public String JDBCUserName;
	public String JDBCPassword;
	public boolean EnableAdmin;
	public int ClientSendDelay;
	public int AdminPort;
	
	public ArrayList<String> LogSet;
	
	public Level LogLevel;

	public ServerConfig()
	{
		reloadConfig();
	}
	public void reloadConfig()
	{
		Properties cfg = LoadConfig.load("mfserverconfig.xml");
		ServerName = cfg.getProperty("ServerName");
		ServerPort =  Integer.parseInt(cfg.getProperty("ServerPort"));
		ServerSoTimeOut = Integer.parseInt(cfg.getProperty("ServerSoTimeOut"));
		MaxConnections = Integer.parseInt(cfg.getProperty("ServerPort"));
		MaxErrors = Integer.parseInt(cfg.getProperty("MaxErrors"));
		ConnectionTimeout = Integer.parseInt(cfg.getProperty("ConnectionTimeout"));
		
		AIUserName = cfg.getProperty("AIUserName");
		AIPassword = cfg.getProperty("AIPassword");
		AIIP = cfg.getProperty("AIIP");
		AIPort = Integer.parseInt(cfg.getProperty("AIPort"));
		
		ClientCache = Integer.parseInt(cfg.getProperty("ClientCache"));
		JDBCDriverClassName =  cfg.getProperty("JDBCDriverClassName");
		JDBCUrl =  cfg.getProperty("JDBCUrl");
		JDBCUserName =  cfg.getProperty("JDBCUserName");
		JDBCPassword =  cfg.getProperty("JDBCPassword");
		
		LogName = cfg.getProperty("LogName");
		LogFileBack = Integer.parseInt(cfg.getProperty("LogFileBack"));
		LogMax = cfg.getProperty("LogMax");
		int LogLevelInt = Integer.parseInt(cfg.getProperty("LogLevel"));
		LogDir = cfg.getProperty("LogDir");
		LogFormat = cfg.getProperty("LogFormat");
		
		EnableAdmin = Boolean.valueOf(cfg.getProperty("EnableAdmin"));
		ClientSendDelay =Integer.parseInt(cfg.getProperty("ClientSendDelay"));
		AdminPort = Integer.parseInt(cfg.getProperty("AdminPort"));
		
		switch (LogLevelInt)
		{
		case 0:
			LogLevel = Level.OFF;
			break;
		case 1:
			LogLevel = Level.SEVERE;
			break;
		case 2:
			LogLevel = Level.WARNING;	
			break;
		case 3:
			LogLevel = Level.INFO;
			break;
		case 4:
			LogLevel = Level.CONFIG;
			break;
		case 5:
			LogLevel = Level.FINE;
			break;
		case 6:
			LogLevel = Level.FINER;
			break;
		case 7:
			LogLevel = Level.FINEST;
			break;
		case 8:
			LogLevel = Level.ALL;
			break;
		}

	}

}
