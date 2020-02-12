package com.bjcsc.pemc.rex.mf.admin;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.io.IOException;

import com.bjcsc.pemc.rex.mf.MFConsoleMain;
import com.bjcsc.pemc.rex.mf.protocol.AdminProtocol;
import com.bjcsc.pemc.rex.mf.util.BaseOperation;
import com.bjcsc.pemc.rex.mf.util.StringUtil;
import com.bjcsc.pemc.rex.mf.config.AdminConfig;

import org.apache.log4j.Logger;


public final class AdminServer extends Thread implements BaseOperation
{
	private ByteBuffer r_buff = ByteBuffer.allocate(1024);//这里只处理接收信息
	private ByteBuffer w_buff = ByteBuffer.allocate(1024);//debug
	private int port = 11158;//缺省端口
	private volatile boolean stop = false; // 停止标记，同步机制变量
	private int setSoTimeout;
	private	Logger log;
	private SendInfoThread sit;
	private final AdminConfig config = new AdminConfig();
	private String AdminName,AdminPassword;
	protected int SendInterval;
	
	private MFConsoleMain MFCM = null;
	
	public AdminServer(int serverPort)
	{
		this.port = serverPort;
		log=Logger.getLogger("ADMIN");

		AdminName = config.AdminName;
		AdminPassword = config.AdminPassword;
		SendInterval = config.Interval;		
		sit = new SendInfoThread();
		sit.SetAdminServer(this);
	}
	
	public void SetMainClass(MFConsoleMain mfcm)
	{
		this.MFCM = mfcm;
	}
	
	public void run()
	{
		sit.start();
		try
		{
			//log.warn("Listen...");
			//生成一个信号监视器
			Selector selector = Selector.open();
			//生成一个侦听端
			ServerSocketChannel ssc=ServerSocketChannel.open();
			//将侦听端设为异步方式
			ssc.configureBlocking(false);
			//侦听端服务socket绑定到一个端口
			ServerSocket ss=ssc.socket();
			InetSocketAddress address = new InetSocketAddress(port);
			ss.bind(address);//backlog=7 请求连接队列数量，超出时返回java.net.ConnectException: Connection refused
			//设置侦听端所选的异步信号OP_ACCEPT
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			//log.warn("registered port~~");
			log.fatal("AdminServer Start....");
			int nKey=0;
			while(true)
			{
				nKey = selector.select();
				
				if (nKey == 0) //没有指定的I/O事件发生
				{
					continue;
				}
				Set<SelectionKey> selectionKeys=selector.selectedKeys();
				Iterator<SelectionKey> iter=selectionKeys.iterator();
				while(iter.hasNext())
				{
					SelectionKey key=iter.next();
					if((key.readyOps()&SelectionKey.OP_ACCEPT)==SelectionKey.OP_ACCEPT)
					{
						ServerSocketChannel subssc=(ServerSocketChannel)key.channel();
						//接受一个新的连接
						SocketChannel channel=subssc.accept();//
						if (channel != null)
                        {  
							channel.configureBlocking(false);//设置该socket的异步信号	
							//OP_READ:当socket可读时，
							//触发函数DealwithData()
							channel.register(selector, SelectionKey.OP_READ);
                          }
						else
						{  
                            //System.out.println("---No Connection---");  
                        } 
						iter.remove();
						//System.out.println("有新连接:"+channel);

					}
					else if((key.readyOps()&SelectionKey.OP_READ)==SelectionKey.OP_READ)
					{
					    if (key.isReadable()) 
					    {

					    	//某socket可读信号
					    	DealwithData(key);
					    	
					    }
						iter.remove();
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}
	public void DealwithData(SelectionKey key)// throws IOException
	{
		int count = 0;
		long readCount =0;
		//由key获取指定socketchannel的引用
		SocketChannel sc = (SocketChannel)key.channel();
		r_buff.clear();
		//读取数据到r_buff
		try
		{
			while((count = sc.read(r_buff))> 0)
			{
				readCount+=count;
				//log.debug("count:"+count);
			}
		} catch (IOException e)
		{
			log.warn("AdminServer sc.read--IO:"+e.getMessage()+" Close io without msg");	
			sit.delSocketChannel(sc);
			try	{
				sc.close();//存在链接后不发消息，直接close情况 ，这里处理！
			} catch (IOException ioe){
				log.warn("Close io without msg "+ioe.getMessage());
			}
			return;
		}
		//客户端断开链接处理方法
		if(count==-1)
		{
			sit.delSocketChannel(sc);
		}
		//确保r_buff可读
		r_buff.flip();
		byte[] read = new byte[r_buff.limit()];
		System.arraycopy(r_buff.array(),0,read,0,r_buff.limit());
		log.debug("count:"+count +" Admin Recv: "+StringUtil.toHexString(read));
		AdminProtocol ap = new AdminProtocol(read);
		if(ap.isLoginPackage())
		{
			log.debug("Login Package! User:" + ap.getLoginUsername());
			//log.debug("Login Package! User:" + ap.getLoginUsername()+" pass:"+ap.getLoginPassword());
			//log.debug(AdminName+"-"+AdminPassword);
			if(ap.getLoginUsername().equals(AdminName)&&ap.getLoginPassword().equals(AdminPassword))
			{
				log.warn("Get AdminPad Conncet...."+sc.toString());
				if(!sit.isLoginSocketChannel(sc))
				{
					sit.addSocketChannel(sc);
					sit.sendLoginOK(sc);
				}
			}
			else//用户名错误
			{
				try
				{
					sc.close();
				} catch (IOException e)
				{
					log.warn("sc.close()--IO:"+e.getMessage());
				}
			}

		}else if(ap.isCMDPackage())
		{
			if(sit.isLoginSocketChannel(sc))
			{
				String cmd = ap.getDataString();
				if(cmd.equals(AdminCommand.ShutdownServer)){
					MFCM.ShutdownServer();
				}else if(cmd.equals(AdminCommand.StartServer)){
					if(!MFCM.StartServer(false)){sit.sendMsg(sc, AdminCommand.ServerRunning);}						
				}else if(cmd.equals(AdminCommand.RestartServer)){
					MFCM.RestartServer();
				}else if(cmd.equals(AdminCommand.KillServer)){
					MFCM.KillServer();
				}
			}
		}

		
		r_buff.clear();
	}
	/**
	 * 获取服务器状态信息
	 * @return
	 */
	protected String GetServerInfoMsg()
	{
		String msgS="";
		ArrayList<String> list = this.MFCM.GetServerStatInfo();
		if(list!=null)
		{
			
			for(String s:list)
			{
			//log.debug("MFM info:"+s);
				msgS=AdminProtocol.GetALLInfo(msgS, s);
			}
			log.debug("MFM infos:"+msgS);
		}
		else{msgS=AdminCommand.ServerNotRun;}
		return msgS;
	}
	/**
	 * 得到管理数据的字符串
	 * @param name
	 * @param info
	 * @return
	 */
	public static String GetServerInfo(String name,String info)
	{
		return AdminProtocol.GetNameAndStat(name, info);
	}
	public static String GetInfoS(String infos,String newinfo)
	{
		return AdminProtocol.GetStatsAll(infos, newinfo);
	}
	
	@Override
	public void stopRun()
	{
		// TODO Auto-generated method stub
		log.fatal("!!!!!!!!!Stop AdminServer!!!!!!!!!!!!");
		stop = true;
		sit.sendMsg(AdminCommand.ServerClose);
		sit.stopRun();
		
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
}
