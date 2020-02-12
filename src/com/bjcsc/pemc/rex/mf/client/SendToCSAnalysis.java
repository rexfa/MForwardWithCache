package com.bjcsc.pemc.rex.mf.client;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import com.bjcsc.pemc.rex.mf.util.StringUtil;


/**
 * ͨ�������ϴ���Ϣ ������Ϣ���豸
 * @author Rex
 *
 */
public class SendToCSAnalysis extends Thread
{
	private ArrayBlockingQueue<byte[]> msgQueue = new ArrayBlockingQueue<byte[]>(3000);

	private Logger log;
	private volatile boolean stop = false; // ֹͣ��ǣ�ͬ�����Ʊ���
	private CSClient csClient;
	
	public SendToCSAnalysis(CSClient csClient)
	{
		log=Logger.getLogger("CLIENT");
		this.csClient = csClient;
	}
	/**
	 * ����֮ǰ���绺�����
	 * @param msg
	 */
	protected void SendToCSWithCache(byte[] msg)
	{
		try	{
			if(msgQueue.size()<msgQueue.remainingCapacity())
			{
				msgQueue.put(msg);}
			else{
				log.fatal("Queue Overflow :"+ StringUtil.toHexString(msg));
			}
		} catch (InterruptedException e){
			log.fatal("Queue Err"+e.getMessage()+" Msg : "+StringUtil.toHexString(msg));
		}
	}
	public void run()
	{
		byte[] sendMsg;
		boolean sendSuccess;
		while(!stop)
		{
			if(!msgQueue.isEmpty()&&csClient.IsLogin()){
				if(csClient!=null){
					sendMsg = msgQueue.poll();
					sendSuccess = csClient.SendToServer(sendMsg,false);
					if(!sendSuccess)//����ʧ�� ���·ŵ����м���
					{
						SendToCSWithCache(sendMsg);
						try	{
							Thread.sleep(500);
						} catch (InterruptedException e){
							log.fatal("Sleep Err"+e.getMessage());
						}
					}
				}
	    	}
			else{
				try	{
					Thread.sleep(500);
				} catch (InterruptedException e){
					log.fatal("Sleep Err"+e.getMessage());
				}
			}
		}
	}
	/**
	 * �õ����ڶ��д�С
	 * @return
	 */
	protected int GetMsgQueueSize()
	{
		return msgQueue.size();
	}

}
