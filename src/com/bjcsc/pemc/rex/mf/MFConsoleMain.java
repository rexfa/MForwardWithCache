/**
 * 
 */
package com.bjcsc.pemc.rex.mf;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.bjcsc.pemc.rex.mf.admin.AdminServer;
import com.bjcsc.pemc.rex.mf.config.*;
import com.bjcsc.pemc.rex.mf.server.*;
import com.bjcsc.pemc.rex.mf.client.*;


 
import org.apache.log4j.Logger;


/**
 * @author ����
 *
 */
public class MFConsoleMain {

	private ServerConfig config;
	private Logger log;
	private Hashtable<String,ClientTransmissionManager> Clients;
	private CSClient csc=null;
	private MFNIOServer server=null;
	private com.bjcsc.pemc.rex.mf.admin.AdminServer admin;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MFConsoleMain serverMain = new MFConsoleMain();
		
		
	}
	public MFConsoleMain()
	{ 
		if(!StartServer(true))
		{
			System.exit(1);
		}
	}

	/**
	 * ��ʼ�����пͻ����̣߳�������ʱʹ��Cache������������
	 */
	public void initializationClients()
	{
		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, String>> it=ClientInfo.getAllClientInfo();
		while(it.hasNext())
		{
		    Map.Entry<String,String> entry=it.next();
		    String userName = entry.getKey();
		    String password = entry.getValue();
		    ClientTransmissionManager ctm = new ClientTransmissionManager(userName,password,config);
		    ctm.SetSendDelay(config.ClientSendDelay);
		    ctm.start();
		    //System.out.println(userName +":"+password );
		    Clients.put(userName, ctm);
		}
	}
	/**
	 * ��ʼ��
	 */
	public void initialization()
	{
		config = new ServerConfig();
		initializationLog();
	}
	
	public void initializationLog()
	{
		log=Logger.getLogger("MAIN");
		//LogUtil.logFlieNameSet(log,config);
	}
	/**
	 * ��÷�����״̬��Ϣ
	 * @return
	 */
	public ArrayList<String> GetServerStatInfo()
	{
		
		ArrayList<String> list = new ArrayList<String>();
		if(Clients!=null)
		{
			if(Clients.size()>0){
				Enumeration<ClientTransmissionManager> css = Clients.elements();
				while(css.hasMoreElements())
				{
					ClientTransmissionManager c = css.nextElement();
					list.add(c.getStatString());
				}
			}
		}
		if(csc!=null)
			list.add(csc.getStatString());
		if(server!=null)
			list.add(server.getStatString());
		if(list.size()>0)
			return list;
		else
			return null;
	}
	/**
	 * ɱ������Ľ���
	 */
	public void KillServer()
	{
		Enumeration<ClientTransmissionManager> css = Clients.elements();
		while(css.hasMoreElements())
		{
			ClientTransmissionManager c = css.nextElement();
			c.stopRun();
		}
		csc.stopRun();
		server.stopRun();
		admin.stopRun();
		log.fatal("!!!!!!!!!!!!!System.exit 0 !!!!!!!!!!!!");
		System.exit(0);
	}
	/**
	 * ������������߳�
	 */
	public void RestartServer()
	{
		ShutdownServer();
		try
		{
			Thread.sleep(3000);
		} catch (InterruptedException e){
			e.printStackTrace();
		}
		if(!StartServer(false))
		{
			System.exit(1);			
		}
	}
	/**
	 * ��ʼ����
	 * @param SystemStart �Ƿ���ϵͳ����
	 */
	public boolean StartServer(boolean SystemStart)
	{
		if(server==null&&csc==null)
		{
			initialization();

			Clients = new Hashtable<String, ClientTransmissionManager>();

			log.info("Server Start :"+config.ServerName);

			csc = new CSClient(config.AIIP,config.AIPort,config.AIUserName,config.AIPassword);
			initializationClients();//��ʼ���ͻ����߳�
			server = new MFNIOServer(config.ServerPort, config.ServerSoTimeOut);
			server.Clients = Clients;
			server.csClient = csc;
			csc.SetMFServer(server);
			csc.start();		
			if(config.EnableAdmin&&SystemStart)
			{
				admin = new com.bjcsc.pemc.rex.mf.admin.AdminServer(config.AdminPort);
				admin.SetMainClass(this);
				admin.start();
			}
			//while(!csc.isConncent()){}
			server.start();//��������¶� ����
			return true;
		}
		else
			return false;
	}
	/**
	 * �رշ���
	 */
	public void ShutdownServer()
	{
		Enumeration<ClientTransmissionManager> css = Clients.elements();
		while(css.hasMoreElements())
		{
			ClientTransmissionManager c = css.nextElement();
			c.stopRun();
		}
		Clients.clear();
		Clients = null;
		csc.stopRun();
		csc = null;
		server.stopRun();
		server = null;
		System.gc();
		log.fatal("!!!!!!!!!!!!!Shutdown Server!!!!!!!!!!!!");
	}


}
