package com.bjcsc.pemc.rex.mf.util;

public interface BaseOperation
{
	public void stopRun();
	public void restart();
	/**
	 * ״̬Stringȡ��
	 * @return
	 */
	public String getStatString();
	/**
	 * ״̬String[]ȡ��
	 * @return
	 */
	public String[] getStats();
}
