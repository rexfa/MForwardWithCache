package com.bjcsc.pemc.rex.mf.util;

public interface BaseOperation
{
	public void stopRun();
	public void restart();
	/**
	 * 状态String取得
	 * @return
	 */
	public String getStatString();
	/**
	 * 状态String[]取得
	 * @return
	 */
	public String[] getStats();
}
