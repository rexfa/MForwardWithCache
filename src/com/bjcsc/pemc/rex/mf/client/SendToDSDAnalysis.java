package com.bjcsc.pemc.rex.mf.client;


import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import com.bjcsc.pemc.rex.mf.server.MFNIOServer;

/**
 * 提供消息分拣机制，此类大大降低接受服务器消息的消耗时间
 * @author Rex
 *
 */
public class SendToDSDAnalysis extends Thread
{
	private ArrayBlockingQueue<byte[]> msgQueue = new ArrayBlockingQueue<byte[]>(3000);

	private Logger log;
	private volatile boolean stop = false; // 停止标记，同步机制变量
	private MFNIOServer mfserver;
	
	public SendToDSDAnalysis(MFNIOServer mfserver)
	{
		log=Logger.getLogger("CLIENT");

		this.mfserver = mfserver;
	}
	protected void SendToClinet(byte[] msg)
	{
		try
		{
			msgQueue.put(msg);
		} catch (InterruptedException e)
		{
			//e.printStackTrace();
			log.fatal("Queue Err"+e.getMessage());
		}
	}
	
	public void run()
	{
		while(!stop)
		{
			if(!msgQueue.isEmpty()){
				if(mfserver!=null){
					mfserver.sendMsgToAllClient(msgQueue.poll());
					//mfserver.sendMsgToAllClientTransparent(msgQueue.poll());//debug 调试用 不分拣透明传输
				}
	    	}
			else
			{
				try
				{
					Thread.sleep(500);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					log.fatal("Sleep Err"+e.getMessage());
				}
			}
		}
	}
	/**
	 * 得到现在队列大小
	 * @return
	 */
	protected int GetMsgQueueSize()
	{
		return msgQueue.size();
	}
}
