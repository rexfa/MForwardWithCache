package com.bjcsc.pemc.rex.mf.client;

import java.net.*;
import java.io.*;
import java.net.InetSocketAddress;  
import java.nio.ByteBuffer;
import java.util.Set;
import java.math.BigInteger;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;  
import java.nio.channels.SocketChannel;
import java.util.Iterator; 
import java.security.DigestException;  
import java.security.MessageDigest; 

import com.bjcsc.pemc.rex.mf.protocol.AI;
import com.bjcsc.pemc.rex.mf.protocol.AdminProtocol;
import com.bjcsc.pemc.rex.mf.server.MFNIOServer;
import com.bjcsc.pemc.rex.mf.util.BaseOperation;
import com.bjcsc.pemc.rex.mf.util.StringUtil;
import com.bjcsc.pemc.rex.mf.config.ServerConfig;

import org.apache.log4j.Logger;


public class CSClient extends Thread implements BaseOperation
{
	private final String aiIP;
	private final int aiPort;
	private final String username;
	private final String password;
	private Logger log;
	private volatile boolean stop = false; // 停止标记，同步机制变量
	private volatile boolean reconnect = false;// 重新连接，同步机制变量
	//private AI aiReceive = new AI() ;
	private AI aiLogin = new AI();
	private SocketChannel sc;
	private BigInteger msgNumber = new BigInteger("0"); 
	private MFNIOServer mfserver = null;
	private SendToDSDAnalysis asThread = null;
	private SendToCSAnalysis csThread = null;
	private boolean isLogin = false;
	
    /*缓冲区大小*/  
    private static int BLOCK = 1024;  
    /*接受数据缓冲区*/  
    private static ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK); 
    /*发送数据缓冲区*/  
    private static ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK); 
	
    public CSClient(String aiIP,int aiPort, String username,String password) 
    {
        super("AIServer.Connection:" +aiIP+ ":"+aiPort+" AIServer.UserName:"+username);
         
        this.aiIP = aiIP;
        this.aiPort = aiPort;
        this.username = username;
        this.password = password;
       
        initializationLog();
    }
    private void initializationLog()
	{
		log=Logger.getLogger("CLIENT");
	}
    public void SetMFServer(MFNIOServer mfserver)
    {
    	this.mfserver = mfserver;
    	asThread = new SendToDSDAnalysis(mfserver);
    	asThread.start();
    	csThread = new SendToCSAnalysis(this);
    	csThread.start();    	
    }
    public void run() 
    {
        while(!stop)
        {
        	reconnect = false;
           	log.warn("Connect AI server...");
           	Selector selector = null;
           	SelectionKey skey =null;
           	int readCount = 0 ;
			try	{
				selector = Selector.open();
				sc = SocketChannel.open();
				sc.configureBlocking(false);
				//sc.socket().setSoTimeout(10000);
				InetSocketAddress ica = new InetSocketAddress(this.aiIP,this.aiPort);
				sc.connect(ica);
				//skey = sc.register(selector,SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
				skey = sc.register(selector,SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
			} 
			catch (IOException e)
			{
				//e.printStackTrace();
				log.fatal(e.getMessage());
				try	{
					Thread.sleep(1500);
					sc.close();
				} catch (IOException e2){
					log.fatal(e2.getMessage());
				} catch (InterruptedException ei){
					log.fatal(ei.getMessage());
				}
				log.fatal("Reinit SocketChannel;");
				continue;//socket注册失败后继续尝试
			}
			while(!stop&&!reconnect)
            {
               	try	{
					while (selector.select() > 0&&!reconnect) 
					{
						Set<SelectionKey> set = selector.selectedKeys();
						for (SelectionKey key : set) 
						{
							if(reconnect) break;
							selector.selectedKeys().remove(key);
							int ops = key.readyOps();
							if ((ops & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
								log.warn("Connceted..");
								if(sc.finishConnect()){ //完成连接， 此为堵塞方法
									this.SendLoginMsg();// 发送login命令
									skey = sc.register(selector, SelectionKey.OP_READ);									
								}else{
									log.debug("FinishConnect Failure!~~~~~~~~~~~~~~!Wait....");
									reconnect = true;
								}
							}
							if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
								readCount=sc.read(receivebuffer);
								receivebuffer.flip();
								byte[] rev = new byte[receivebuffer.limit()];
								System.arraycopy(receivebuffer.array(),0,rev,0,receivebuffer.limit());
								if(readCount==-1)	{
									log.warn("CsServer disconcet...");
									reconnect = true;
									break;
								}
								
								if(this.isLogin){
									SendToClient(rev);
								}else{
									log.debug(StringUtil.toHexString(rev)+" String "+new String(rev));
									if(AI.IsLoginSuccess(rev)){
										isLogin = true;
										log.warn("CS Login Ok;");
									}else{
										reconnect = true;
										isLogin = false;
									}
								}
								receivebuffer.clear();
							}
						}
						//if(reconnect) break;
					}//while (selector.select() > 0&&!reconnect) 
				}catch(NoConnectionPendingException npe)
				{
					log.warn("NoConnectionPendingException:"+npe.getMessage());//如果未连接此通道并且尚未发起连接操作
					reconnect = true;
					break;
				}
               	catch (ClosedChannelException e){
					log.warn("ClosedChannelE:"+e.getMessage());
					reconnect = true;
					break;
				}catch(java.net.ConnectException ce){
					log.warn("ConnectException:"+ce.getMessage());
					reconnect = true;
					break;
				}
				catch (IOException e){
					log.warn(e.getMessage());
					reconnect = true;
					break;
				}
				catch (Exception e){
					log.warn(e.getMessage());
					reconnect = true;
					break;
				}
            }//while(!stop&&!reconnect)
        	try	{
				selector.close();
			} catch (IOException e1){
				log.warn("IOE:"+e1.getMessage());
			}
   	
            try {
            	isLogin=false;
               	sc.close(); //重新连接前关闭sc          	
            } catch (IOException e){
               	log.warn("IOE:"+e.getMessage());
			} finally{
				if(!stop)
              	try{
              		log.warn("Reconncet.......wait.. . 10s");
					Thread.sleep(10000);
				} catch (InterruptedException e){}
            }
        }//while conncet
    }
    /**
     * 发送登录CS命令，其中包含对CS不响应的控制
     */
    private void SendLoginMsg()
    {
    	try {
        	  log.warn("Send login message.. isConnected :"+sc.isConnected() + " isOpen :"+sc.isOpen());
        	  Thread.sleep(1000);
        	
        } catch (InterruptedException ie) 
		{
			ie.printStackTrace();
		}
    	SendToServer(aiLogin.getLoginPackage(username, password),true);
    }
    /**
     * 发送消息到客户端
     * @param msg
     */
    private void SendToClient(byte[] msg)
    {
    	
    	if(asThread!=null)
    	{
    		asThread.SendToClinet(msg);
    	}
    	addSUMMsg();
    }
    /**
     * 发送消息到发送缓存
     * @param msg
     */
    public void SendToServerCache(byte[] msg)
    {
    	csThread.SendToCSWithCache(msg);
    }
    /**
     * 为了保证ByteBuffer线程安全性可以做同步 synchronized
     * @param msg
     */
    protected boolean SendToServer(byte[] msg,boolean isLoginPackage)
    {
    	int sended = 0;
    	if(sc!=null&&(isLogin||isLoginPackage))
    	{
    		log.debug("Send msg to csserver. HexString["+StringUtil.toHexString(msg)+"]  String:"+new String(msg));
    		sendbuffer.put(msg);
    		sendbuffer.flip();    		
    		try
			{		
    			while(sendbuffer.hasRemaining())
    			{
    				sended = sc.write(sendbuffer);
    			}
			} catch (IOException e)	{
				log.warn(e.getMessage());
				reconnect = true;
			}
			finally	{
				sendbuffer.clear();
			}
			if(sended>0){return true;}
			else{
				log.fatal("Send err :"+StringUtil.toHexString(msg));
				return false;}
    	}
    	else{return false;}
    		
    }
    /**
     * 计数用
     */
    private void addSUMMsg()
    {
    	msgNumber =msgNumber.add(BigInteger.ONE);
    	if(msgNumber.mod(new BigInteger("10000"))==BigInteger.ZERO)
    	{
    		log.warn("Received:"+msgNumber.toString() +" at least");
    	}
    }
	@Override
	public void stopRun()
	{
		// TODO Auto-generated method stub
		log.fatal("!!!!!!!!!Stop CSCliet!!!!!!!!!!!!");
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
		String infos = AdminProtocol.GetNameAndStat(AdminProtocol.FristStatName,"CSClient");
		if(sc!=null)
			if(sc.isConnected())
				infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Connected","true"));
			else
				infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Connected","false"));
		else
			infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Connected","false"));
		infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Received",msgNumber.toString()));
		infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("DSDmsgQueue",String.valueOf(asThread.GetMsgQueueSize())));
		infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("CSmsgQueue",String.valueOf(csThread.GetMsgQueueSize())));
		return infos;
	}
	@Override
	public String[] getStats()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public boolean isConncent() 
	{
		try
		{
			Thread.sleep(100);
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.sc.isConnected();
	}
	/**
	 * 判断是否登录成功
	 * @return
	 */
	public boolean IsLogin()
	{
		return this.isLogin;
	}
    
   
}
