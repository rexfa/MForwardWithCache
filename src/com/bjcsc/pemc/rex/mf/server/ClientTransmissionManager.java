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
	private volatile boolean stop = false; // 停止标记，同步机制变量
	//private volatile boolean reConncet = false;//标记是否重新进行连接了
	private ArrayBlockingQueue<byte[]> msgQueue;
	private int cacheCapacity;
	private String userName="",password="";
	private int pType,pNum;//基站类型和数量代码
	private Logger log;
	private Logger logQ;
	private final ServerConfig config;
	private ArrayList<String> SIMList = null;
	private SocketChannel csc = null;
	private ByteBuffer w_buff = ByteBuffer.allocate(1024);//写出的消息缓存
	//private AI aiFromDSD = new AI();
	private AI aiToDSD = new AI();
	private int sleepTime =1000;
	private int ClientSendDelay=10;
	private BigInteger msgNumber = new BigInteger("0"); 
	
	private java.util.Date initDate;// = new Date(); 	
	
	
	private boolean isInitialSuccess = false;
	/**
	 * 是否初始化成功
	 * @return
	 */
	public boolean IsInitialSuccess()
	{
		return isInitialSuccess ;
	}
	/**
	 * 设置每个包之间延迟发送的时间ms
	 * @param ClientSendDelay
	 */
	public void SetSendDelay(int ClientSendDelay)
	{
		this.ClientSendDelay=ClientSendDelay;
	}
	
	public ClientTransmissionManager(String userName,String password,ServerConfig cfg)
	{
		this.cacheCapacity = cfg.ClientCache;
		msgQueue= new ArrayBlockingQueue<byte[]>(cacheCapacity);//队列容量大，保证消息尽量不丢失
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
	 * 返回密码
	 * @return
	 */
	public String getLoginPassword()
	{
		return password;
	}
	/**
	 * 初始化基站sim卡列表
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
		    	SIMList.add(rs.getString(2));//加入维护sim卡集合
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
	 * 重新初始化SIM列表
	 * 新加的
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
	 * 发送消息 线程方法
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
							sleepTime = 2000;//发送失败就等2秒
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
				Thread.sleep(sleepTime);//睡眠 降低系统开销				
			} catch (InterruptedException e)
			{
				log.debug(e.getMessage());
			}
		}
		log.fatal(this.userName+ "Stop Thread DSD Servic~!!!! --");
	}
	
	/**
	 * 发送消息 私有保证外部不直接发送消息
	 * @param msg
	 * @return 是否成功
	 */
	private boolean sendMsg(byte[] msg)
	{
		//if(this.userName.equals("0600"))//debug专用
		//{
		//	log.debug(this.userName +"["+new String(msg)+"]"+" sending.....");
		//}
		//将msg内容拷入w_buff  
		//暂时假定取出消息就算发送成功
		//以后如果发现异常，可以使用取出消息，而暂时不移除队列头
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
/*					if(this.userName.equals("0600"))//debug专用
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
	 * 发现消息到客户端，保存到队列
	 * @param msg
	 */
	public void sendMsgToClient(byte[] msg)
	{

		aiToDSD.setByte(msg);
		if(!SIMList.isEmpty())
			if(SIMList.contains(aiToDSD.getSIM()))
			{
				//if(this.userName == "0600"||this.userName == "0700")//debug专用
				//{
				//	log.debug(this.userName +" string : ["+new String(msg)+"]"+" send Ok.....");
				//	log.debug(this.userName +" stringHEX : ["+StringUtil.toHexString(msg)+"]"+" send Ok.....");
				//}
				addMsgInCache(msg);//任何消息 都先如cache
			}
	}
	/**
	 * 发现消息到客户端 入队列 不检查
	 * @param msg
	 */
	public void sendMsgToClientWithoutCheck(byte[] msg)
	{
		if(SIMList.contains(aiToDSD.getSIM()))
		{
/*			if(this.userName == "0600"||this.userName == "0700")//debug专用
			{
				log.debug(this.userName +" string : ["+new String(msg)+"]"+" send Ok.....");
				log.debug(this.userName +" stringHEX : ["+StringUtil.toHexString(msg)+"]"+" send Ok.....");
			}*/
		}
		addMsgInCache(msg);//任何消息 都先如cache
	}
	/**
	 * 发现消息到客户端，先保存到队列
	 * @param a AI
	 */
	public void sendMsgToClient(AI a)
	{	
		String sim=a.getSIM();
		if(!SIMList.isEmpty())
			if(SIMList.contains(sim))
			{					
/*				if(this.userName == "0600"||this.userName == "0700")//debug专用
				{
					log.debug(this.userName +" string : ["+new String(a.GetPackageByteArray())+"]"+" send Ok.....");
					log.debug(this.userName +" stringHEX : ["+StringUtil.toHexString(a.GetPackageByteArray())+"]"+" send Ok.....");
				}*/
				addMsgInCache(a.GetPackageByteArray());//任何消息 都先如cache
			}
	}
	/**
	 * 加入消息到队列
	 * @param msg
	 */
	private void addMsgInCache(byte[] msg)
	{
		//System.out.print(this.getName() +" leg "+ " size "+msgQueue.size() );
		byte[] demission = null;
		addSUMMsg();//计数
/*		if(this.userName.equals("0600"))//debug专用
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
	 * 加入消息到队列，不检查队列大小
	 * @param msg
	 * @return 队列大小
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
	 * 设置一个新的客户通道
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
		//更新csc的时候重置SIM列表，这样，防止离线加减站
		reInitializationSIMList();
		this.csc = newcsc;
	}
	/**
	 * 返回本类的csc
	 * @return
	 */
	/*
	public SocketChannel getClientSocketChannel()
	{
		return csc;
	}*/
	/**
	 * 返回是否是本连接的 SocketChannel
	 */
	protected boolean isMySocketChannel(SocketChannel csc)
	{
		if(this.csc==null)
			return false;
		return (this.csc ==csc);
	}
	
	/**
	 * 关闭客户端连接
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
	 * 加入新的sim
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
	 * 清除一个sim
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
	 * 更新一个sim
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
     * 计数用
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
