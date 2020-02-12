package com.bjcsc.pemc.rex.mf.admin;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.io.IOException;

import com.bjcsc.pemc.rex.mf.protocol.AdminProtocol;
import com.bjcsc.pemc.rex.mf.util.BaseOperation;
import com.bjcsc.pemc.rex.mf.util.StringUtil;

import org.apache.log4j.Logger;

public class SendInfoThread extends Thread implements BaseOperation
{
	private	Logger log;
	private ArrayList<SocketChannel> SCS = new ArrayList<SocketChannel>();
	private volatile boolean stop = false; // 停止标记，同步机制变量
	private volatile int Interval = 5000; // 发送延迟，休眠时间
	private AdminServer AS;
	
	private ByteBuffer sendbuffer = ByteBuffer.allocate(2048);//debug
	
	public SendInfoThread()
	{
		log=Logger.getLogger("ADMIN");
	}
	/**
	 * 把管理服务器付给发送线程
	 * @param AS
	 */
	protected void SetAdminServer(AdminServer AS)
	{
		this.AS = AS;
		if(AS.SendInterval<5000)
			Interval=5000;
		else
			Interval = AS.SendInterval;
	}
	/**
	 * 
	 * @param sc
	 */
	protected synchronized void addSocketChannel(SocketChannel sc)
	{
		SCS.add(sc);
	}
	/**
	 * 判断是否是登录后的sc
	 * @param sc
	 */
	protected synchronized boolean isLoginSocketChannel(SocketChannel sc)
	{
		return SCS.contains(sc);
	}
	/**
	 * 
	 * @param sc
	 */
	protected synchronized void delSocketChannel(SocketChannel sc)
	{
		if(SCS.contains(sc))
		{
			if(sc.isConnected())
			{
				try
				{
					sc.close();
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally{
				
				}
			}
			SCS.remove(sc);
		}
	}
	
	public void run()
	{
		byte[] msg=null;
		
		while(!stop)
		{
			if(!SCS.isEmpty())
			{
				msg = GetSendMsg();
				try{
				for(SocketChannel sc:SCS)
				{
					
					sendToAdminPad(sc,msg);
				}
				}catch(ConcurrentModificationException cme)//循环中如果SCS中有变动则抛出此异常，主要是删除了某些sc造成
				{
					System.out.println("out:"+cme.getMessage());
				}
			}
			try
			{
				Thread.sleep(Interval);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * 获得发送数据
	 * @return
	 */
	protected byte[] GetSendMsg()
	{
		
		AdminProtocol ap = new AdminProtocol();
		ap.SetDataString(AS.GetServerInfoMsg());
		return ap.makeInfoPackage();
	}
	/**
	 * 发送消息到用户管理界面
	 * @param sc
	 * @param msg
	 */
	private void sendToAdminPad(SocketChannel sc ,byte[] msg)
    {
    	if(sc!=null)
    	{
    		//sendbuffer.clear();
    		//log.debug(new String(msg));
    		sendbuffer.put(msg);
    		sendbuffer.flip();
    		int sended = 0;
    		try
			{		
    			while(sendbuffer.hasRemaining())
    			{
     				sended = sc.write(sendbuffer);
    				if(sended < 1 )
    				{
    					log.fatal("Send err :"+StringUtil.toHexString(msg));
    				}
    			}
			}
    		catch(ClosedChannelException cce)
    		{
    			this.delSocketChannel(sc);
    			log.warn(cce.getMessage());
    		}
    		catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.warn(e.getMessage());
			}
			finally
			{
				sendbuffer.clear();
			}
    	}
    }
    @Override
	public void stopRun()
	{
		// TODO Auto-generated method stub
    	log.fatal("!!!!!!!!!Stop Admin sender!!!!!!!!!!!!");
		stop = true;
	}
	@Override
	public void restart()
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getStatString()
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String[] getStats()
	{
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * 发送登录ok
	 * @param sc
	 */
	protected void sendLoginOK(SocketChannel sc)
	{		
		AdminProtocol ap = new AdminProtocol();
		ap.SetDataString(AdminCommand.LoginOk);
		sendToAdminPad(sc,ap.makeInfoPackage());
	}
	/**
	 * 发送消息到客户端
	 * @param sc
	 * @param msg
	 */
	protected void sendMsg(SocketChannel sc,String msg)
	{		
		AdminProtocol ap = new AdminProtocol();
		if(msg.length()>28)	{msg = msg.substring(0, 28);}
		ap.SetDataString(msg);
		sendToAdminPad(sc,ap.makeInfoPackage());
	}
	/**
	 * 群发消息
	 * @param msg
	 */
	protected void sendMsg(String msg)
	{
		for(SocketChannel sc:SCS)
			sendMsg(sc,msg);
	}
	
}
