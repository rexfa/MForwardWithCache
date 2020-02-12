package com.bjcsc.pemc.rex.mf.config;

import java.util.Properties;

public final class AdminConfig
{
	public String AdminName,AdminPassword;
	public int Interval;
	
	public AdminConfig()
	{
		reloadConfig();
	}
	public void reloadConfig()
	{
		Properties cfg = LoadConfig.load("mfadminconfig.xml");
		AdminName = cfg.getProperty("AdminName");
		AdminPassword = cfg.getProperty("AdminPassword");
		Interval = Integer.parseInt(cfg.getProperty("Interval"));
	}
}
