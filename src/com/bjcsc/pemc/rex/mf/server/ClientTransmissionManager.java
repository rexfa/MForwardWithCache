package com.bjcsc.pemc.rex.mf.server;
//import java.util.Queue;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Date;

import javax.sql.*;
import java.sql.*;
import oracle.jdbc.driver.*; 

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

import com.bjcsc.pemc.rex.mf.protocol.AI;
import com.bjcsc.pemc.rex.mf.protocol.AdminProtocol;
import com.bjcsc.pemc.rex.mf.config.ServerConfig;
import com.bjcsc.pemc.rex.mf.util.StringUtil;
import com.bjcsc.pemc.rex.mf.util.LogUtil;
import com.bjcsc.pemc.rex.mf.util.BaseOperation;

import org.apache.log4j.Logger;


public final class ClientTransmissionManager extends Thread implements BaseOperation
{
	private volatile boolean stop = false; // ֹͣ��ǣ�ͬ�����Ʊ���
	//private volatile boolean reConncet = false;//����Ƿ����½���������
	private ArrayBlockingQueue<byte[]> msgQueue;
	private int cacheCapacity;
	private String userName="",password="";
	private int pType,pNum;//��վ���ͺ���������
	private Logger log;
	private Logger logQ;
	private final ServerConfig config;
	private ArrayList<String> SIMList = null;
	private SocketChannel csc = null;
	private ByteBuffer w_buff = ByteBuffer.allocate(1024);//д������Ϣ����
	//private AI aiFromDSD = new AI();
	private AI aiToDSD = new AI();
	private int sleepTime =1000;
	private int ClientSendDelay=10;
	private BigInteger msgNumber = new BigInteger("0"); 
	
	private java.util.Date initDate;// = new Date(); 	
	
	
	private boolean isInitialSuccess = false;
	/**
	 * �Ƿ��ʼ���ɹ�
	 * @return
	 */
	public boolean IsInitialSuccess()
	{
		return isInitialSuccess ;
	}
	/**
	 * ����ÿ����֮���ӳٷ��͵�ʱ��ms
	 * @param ClientSendDelay
	 */
	public void SetSendDelay(int ClientSendDelay)
	{
		this.ClientSendDelay=ClientSendDelay;
	}
	
	public ClientTransmissionManager(String userName,String password,ServerConfig cfg)
	{
		this.cacheCapacity = cfg.ClientCache;
		msgQueue= new ArrayBlockingQueue<byte[]>(cacheCapacity);//���������󣬱�֤��Ϣ��������ʧ
		this.userName = userName;
		this.password = password;
	
		this.config = cfg;
		byte[] p = StringUtil.toByteArray(userName);
		this.pType=p[0];
		this.pNum =p[1];
		initializationLog();
		initializationSIMList();
	}
    private void initializationLog()
	{
		log=Logger.getLogger("SCLIENT"+userName);
		LogUtil.logDailyRollingFileSet(log,config,userName);
		logQ=Logger.getLogger("SCLIENT_Queue"+userName);
		LogUtil.logDailyRollingFileSet(logQ,config,userName,userName+"Queue");
	}
	/**
	 * ��������
	 * @return
	 */
	public String getLoginPassword()
	{
		return password;
	}
	/**
	 * ��ʼ����վsim���б�
	 */
	public void initializationSIMList()
	{
		initDate = new Date(System.currentTimeMillis());
		SIMList = new ArrayList<String>();
		String jdbcuser = config.JDBCUserName;
		String jdbcpwd = config.JDBCPassword;
		String jdbcurl = config.JDBCUrl;
		String jdbcdrv = config.JDBCDriverClassName;
		Connection con = null;
	    Statement stmt = null;  
	    ResultSet rs = null;  
		try
		{
			Class.forName(jdbcdrv);
			con = DriverManager.getConnection(jdbcurl,jdbcuser,jdbcpwd);
			CallableStatement proc = null;
		    proc = con.prepareCall("{ call WYJ_TRANSMIT.P_TMON_Q(?,?,?,?,?) }");  
		    proc.setLong("pTYPE",(long)this.pType);
		    proc.setLong("pNum", (long)this.pNum);
		    proc.registerOutParameter("V_CUR",oracle.jdbc.OracleTypes.CURSOR); 
		    proc.registerOutParameter("FLAG",oracle.jdbc.OracleTypes.NUMBER);
		    proc.registerOutParameter("EXPLANATION",oracle.jdbc.OracleTypes.VARCHAR); 
		    proc.execute();  
		    rs = (ResultSet)proc.getObject("V_CUR"); 
		    
		    while(rs.next())  
		    {  
		    	SIMList.add(rs.getString(2));//����ά��sim������
		    	//System.out.println("<tr><td>" + rs.getString(1) + "</td><td>"+rs.getString(2)+"</td></tr>");  
		    }
		    isInitialSuccess = true;
		}
		catch(SQLException ex)
		{
			log.warn(this.userName+" SQL:"+ex.getMessage());
			log.warn(this.userName+" SQLState:"+ex.getSQLState());
			log.warn(this.userName+" SQLErrorCode:"+ex.getErrorCode());
			log.warn(this.userName+" CTM:"+ex.getStackTrace());
			isInitialSuccess = false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.warn(this.userName+e.getMessage());
			isInitialSuccess = false;
		}finally
		{
			try
			{
				log.fatal("DB-read: Sims  " + SIMList.size());
				if(con!=null) 
					con.close();
				log.fatal(this.userName +"SIM info Loaded~");
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
		 }
	}
	/**
	 * ���³�ʼ��SIM�б�
	 * �¼ӵ�
	 */
	public void reInitializationSIMList()
	{
		Date nowDate = new Date(System.currentTimeMillis());
		if(nowDate.getTime()- initDate.getTime()>1000*60)
		{
			initDate = new Date(System.currentTimeMillis());
			SIMList.clear();
			initializationSIMList();
		}
	}
	
	/**
	 * ������Ϣ �̷߳���
	 */
	public void run()
	{
		log.fatal(this.userName+ "Start Thread DSD Servic~");
		while(!stop)
		{
			if(csc==null)
			{
				sleepTime = 3000;
				//log.debug(this.userName + ": SocketChannel is null!");
			}
			else
			{
				if(!msgQueue.isEmpty())
				{
					if(csc.isConnected())
					{
						if(sendMsg(msgQueue.poll()))
						{
							sleepTime = ClientSendDelay;
							//log.debug(this.userName +" send OK.....");
						}
						else
						{
							log.debug(this.userName +" send Failure.....");
							sleepTime = 2000;//����ʧ�ܾ͵�2��
						}
					}
					else
					{
						log.debug("csc is not Connected!");
						sleepTime = 5000;
					}
				}
				else
				{
					//log.debug("MsgQueue is empty");
					sleepTime = 2000;
				}
			}
			try
			{
				Thread.sleep(sleepTime);//˯�� ����ϵͳ����				
			} catch (InterruptedException e)
			{
				log.debug(e.getMessage());
			}
		}
		log.fatal(this.userName+ "Stop Thread DSD Servic~!!!! --");
	}
	
	/**
	 * ������Ϣ ˽�б�֤�ⲿ��ֱ�ӷ�����Ϣ
	 * @param msg
	 * @return �Ƿ�ɹ�
	 */
	private boolean sendMsg(byte[] msg)
	{
		//if(this.userName.equals("0600"))//debugר��
		//{
		//	log.debug(this.userName +"["+new String(msg)+"]"+" sending.....");
		//}
		//��msg���ݿ���w_buff  
		//��ʱ�ٶ�ȡ����Ϣ���㷢�ͳɹ�
		//�Ժ���������쳣������ʹ��ȡ����Ϣ������ʱ���Ƴ�����ͷ
		w_buff.put(msg);
		w_buff.flip();
		int sendBytes=0;
		boolean re =false;
		while(w_buff.hasRemaining())
		{
			try
			{
				sendBytes=0;
				sendBytes = csc.write(w_buff);
				if(sendBytes>0||csc==null)
				{
					//log.debug(this.userName +" send Ok.....");
/*					if(this.userName.equals("0600"))//debugר��
					{
						log.debug(this.userName +"["+new String(msg)+"]"+" send Ok.....");
					}*/
					re= true;
				}
				else
				{
					addMsgInCacheWithoutCheck(msg);
					re= false;
					break;
				}
			}
			catch(IOException ioex)
			{
				re= false;
				log.debug(this.userName + "IOE " +ioex.getMessage());
				try
				{
					csc.close();
					csc = null;
				} catch (IOException e)
				{
					log.debug(this.userName +" IOE2 " + e.getMessage());
				}
				break;
			}
		}//while
		w_buff.clear();
		return re;
	}
	/**
	 * ������Ϣ���ͻ��ˣ����浽����
	 * @param msg
	 */
	public void sendMsgToClient(byte[] msg)
	{

		aiToDSD.setByte(msg);
		if(!SIMList.isEmpty())
			if(SIMList.contains(aiToDSD.getSIM()))
			{
				//if(this.userName == "0600"||this.userName == "0700")//debugר��
				//{
				//	log.debug(this.userName +" string : ["+new String(msg)+"]"+" send Ok.....");
				//	log.debug(this.userName +" stringHEX : ["+StringUtil.toHexString(msg)+"]"+" send Ok.....");
				//}
				addMsgInCache(msg);//�κ���Ϣ ������cache
			}
	}
	/**
	 * ������Ϣ���ͻ��� ����� �����
	 * @param msg
	 */
	public void sendMsgToClientWithoutCheck(byte[] msg)
	{
		if(SIMList.contains(aiToDSD.getSIM()))
		{
/*			if(this.userName == "0600"||this.userName == "0700")//debugר��
			{
				log.debug(this.userName +" string : ["+new String(msg)+"]"+" send Ok.....");
				log.debug(this.userName +" stringHEX : ["+StringUtil.toHexString(msg)+"]"+" send Ok.....");
			}*/
		}
		addMsgInCache(msg);//�κ���Ϣ ������cache
	}
	/**
	 * ������Ϣ���ͻ��ˣ��ȱ��浽����
	 * @param a AI
	 */
	public void sendMsgToClient(AI a)
	{	
		String sim=a.getSIM();
		if(!SIMList.isEmpty())
			if(SIMList.contains(sim))
			{					
/*				if(this.userName == "0600"||this.userName == "0700")//debugר��
				{
					log.debug(this.userName +" string : ["+new String(a.GetPackageByteArray())+"]"+" send Ok.....");
					log.debug(this.userName +" stringHEX : ["+StringUtil.toHexString(a.GetPackageByteArray())+"]"+" send Ok.....");
				}*/
				addMsgInCache(a.GetPackageByteArray());//�κ���Ϣ ������cache
			}
	}
	/**
	 * ������Ϣ������
	 * @param msg
	 */
	private void addMsgInCache(byte[] msg)
	{
		//System.out.print(this.getName() +" leg "+ " size "+msgQueue.size() );
		byte[] demission = null;
		addSUMMsg();//����
/*		if(this.userName.equals("0600"))//debugר��
		{
			log.debug(this.userName +" string : ["+new String(msg)+"]"+" in Cache");
			log.debug(this.userName +" stringHEX : ["+StringUtil.toHexString(msg)+"]"+" in Cache.....");
		}*/
		if(msgQueue.size()>=cacheCapacity)
		{
			demission = msgQueue.poll();
			try
			{
				msgQueue.put(msg);
			} catch (InterruptedException e)
			{
				log.fatal("Queue Err"+e.getMessage());
			}
			logQ.warn(this.userName +": Queue Full! Abandon|"+StringUtil.toHexString(demission));
		}
		else
		{
			try
			{
				msgQueue.put(msg);
			} catch (InterruptedException e)
			{
				log.fatal("Queue Err"+e.getMessage());
			}
		}
	}
	/***
	 * ������Ϣ�����У��������д�С
	 * @param msg
	 * @return ���д�С
	 */
	private int addMsgInCacheWithoutCheck(byte[] msg)
	{
		try
		{
			msgQueue.put(msg);
		} catch (InterruptedException e)
		{
			log.fatal("Queue Err"+e.getMessage());
		}

		return msgQueue.size();

	}
	
	/**
	 * ����һ���µĿͻ�ͨ��
	 */
	public void setClientSocketChannel(SocketChannel newcsc)
	{
		if(newcsc == null)
		{
			log.warn(this.userName +" Set socketChannel null!");
		}
		else
		{
			if(!isMySocketChannel(newcsc))
				CloseSocketChannel();
			log.warn(this.userName +" Set a new socketChannel! "+newcsc.toString());
		}
		//����csc��ʱ������SIM�б���������ֹ���߼Ӽ�վ
		reInitializationSIMList();
		this.csc = newcsc;
	}
	/**
	 * ���ر����csc
	 * @return
	 */
	/*
	public SocketChannel getClientSocketChannel()
	{
		return csc;
	}*/
	/**
	 * �����Ƿ��Ǳ����ӵ� SocketChannel
	 */
	protected boolean isMySocketChannel(SocketChannel csc)
	{
		if(this.csc==null)
			return false;
		return (this.csc ==csc);
	}
	
	/**
	 * �رտͻ�������
	 */
	protected void CloseSocketChannel()
	{
		if(this.csc!=null)
		{
			try
			{
				this.csc.close();
			} catch (IOException e)
			{
				log.debug(this.userName +" CloseSocketChannel IOE:" +e.getMessage());
			}
			this.csc = null;
		}
	}
	/**
	 * �����µ�sim
	 * @param sim
	 */
	public void addSIMToSIMList(String sim)
	{
		if(sim!=null){
			SIMList.add(sim);
			log.debug(this.userName +" Add sim " +sim);
		}
	}
	/**
	 * ���һ��sim
	 * @param sim
	 */
	public void removeFromSIMList(String sim)
	{
		if(sim!=null){
			SIMList.remove(sim);
			log.debug(this.userName +" Remove sim " +sim);
		}
	}
	/**
	 * ����һ��sim
	 * @param oldsim
	 * @param newsim
	 */
	public void updateSIMList(String oldsim,String newsim)
	{
		if(oldsim!=null&&newsim!=null)
		{
			removeFromSIMList(oldsim);
			addSIMToSIMList(newsim);
			log.debug(this.userName + " updataSIMList oldsim " +oldsim +"  newsim "+newsim);
		}
	}
	
    /**
     * ������
     */
    private void addSUMMsg()
    {
    	msgNumber =msgNumber.add(BigInteger.ONE);
    	if(msgNumber.mod(new BigInteger("2000"))==BigInteger.ZERO)
    	{
    		log.debug(this.userName +" Received:"+msgNumber.toString() +" at least");
    	}
    	if(msgNumber.mod(new BigInteger("100"))==BigInteger.ZERO)
    	{
    		log.debug(this.userName+" sate:"+SIMList.size()+" Queue:"+msgQueue.size());
    	}
    	
    }
	
	@Override
	public void stopRun()
	{
		// TODO Auto-generated method stub
		log.fatal("!!!!!!!!!Stop "+this.userName+" Client Manager!!!!!!!!!!!!");
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
		String infos = AdminProtocol.GetNameAndStat(AdminProtocol.FristStatName,this.userName);
		if(csc!=null)
			infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Connected","true"));
		else
			infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Connected","false"));
		infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Queue",String.valueOf(msgQueue.size())+"/"+this.cacheCapacity));
		infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Received",msgNumber.toString()));
		infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("SIMs",String.valueOf(SIMList.size())));
		return infos;
	}
	@Override
	public String[] getStats()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
