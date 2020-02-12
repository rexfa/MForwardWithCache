package com.bjcsc.pemc.rex.mf.server;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.io.IOException;

import com.bjcsc.pemc.rex.mf.client.CSClient;
//import com.bjcsc.pemc.rex.mf.config.ServerConfig;
import com.bjcsc.pemc.rex.mf.protocol.AI;
import com.bjcsc.pemc.rex.mf.protocol.AdminProtocol;
import com.bjcsc.pemc.rex.mf.util.BaseOperation;
import com.bjcsc.pemc.rex.mf.util.StringUtil;

import org.apache.log4j.Logger;

public class MFNIOServer extends Thread implements BaseOperation
{	
	private ByteBuffer r_buff = ByteBuffer.allocate(1024);//这里只处理接收信息
	private ByteBuffer w_buff = ByteBuffer.allocate(1024);//debug
	private int port = 11155;//缺省端口
	private volatile boolean stop = false; // 停止标记，同步机制变量
	private int setSoTimeout;
	private	Logger log;
	public Hashtable<String,ClientTransmissionManager> Clients;
	public CSClient csClient;
	private AI CAI = new AI();//对接受消息的复杂处理,长期驻留在服务器端
	
	private Selector selector=null;
	private ServerSocketChannel ssc = null;
	private ServerSocket ss =null;
	private InetSocketAddress address=null;

	public MFNIOServer(int port ,int setSoTimeout)
	{
		this.port = port;
		initializationLog();
		this.setSoTimeout= setSoTimeout;
	}
	private void initializationLog()
	{
		log=Logger.getLogger("SERVER");
		//LogUtil.logFlieNameSet(log,config);
	}
	public void run()
	{

		try
		{
			log.warn("Listen...");
			//生成一个信号监视器
			selector = Selector.open();
			//生成一个侦听端
			ssc=ServerSocketChannel.open();
			//将侦听端设为异步方式
			ssc.configureBlocking(false);
			//侦听端服务socket绑定到一个端口
			ss=ssc.socket();
			address = new InetSocketAddress(port);
			ss.bind(address);//backlog=7 请求连接队列数量，超出时返回java.net.ConnectException: Connection refused
			//设置侦听端所选的异步信号OP_ACCEPT
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			log.warn("registered port~~");
			
			int nKey=0;
			while(true&&!stop)
			{
				nKey = selector.select();
				
				if (nKey == 0) //没有指定的I/O事件发生
				{
					continue;
				}
				Set<SelectionKey> selectionKeys=selector.selectedKeys();
				Iterator<SelectionKey> iter=selectionKeys.iterator();
				while(iter.hasNext()&&!stop)
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
							log.debug("---No Connection---");
                        } 
						iter.remove();
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
			log.fatal("Server Err!!! :"+e.getMessage());
		}
		finally
		{
			ColseServerSocket();
		}
	}
	private void ColseServerSocket()
	{
		log.warn("Server Socket Close....");
		try
		{
			selector.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try
		{
			ssc.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try
		{
			ss.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 处理接收消息事件
	 * @param key
	 */
	public void DealwithData(SelectionKey key)// throws IOException
	{
		int count = 0;
		long readCount =0;
		//由key获取指定socketchannel的引用
		SocketChannel sc = (SocketChannel)key.channel();
		ClientTransmissionManager ctmtmp = null;
		ClientTransmissionManager ctm = null;
		Enumeration<ClientTransmissionManager> css = Clients.elements();
		while(css.hasMoreElements()){
			ctmtmp = css.nextElement();
			if(ctmtmp.isMySocketChannel(sc))	{
				ctm = ctmtmp;
				break;
			}
		}
		r_buff.clear();
		try	{
			while((count = sc.read(r_buff))> 0)	{
				readCount+=count;
			}
		} catch (IOException e)	{
			log.warn("sc.read--IO:"+e.getMessage()+" Close io without msg");	
			try{
				sc.close(); //存在链接后不发消息，直接close情况 ，这里处理！
			} catch (IOException ioe){
				log.warn("Close io without msg "+ioe.getMessage());
			}
		}
		//客户端断开链接处理方法
		//n 有数据的时候返回读取到的字节数。
		//0 没有数据并且没有达到流的末端时返回0。
		//-1 当达到流末端的时候返回-1
		if(count==-1){
			try{
				if(ctm!=null){
					ctm.setClientSocketChannel(null);
				}
				//log.debug("ClientSocketChannel close by count="+count);
				sc.close();
			} catch (IOException e)
			{
				log.warn("sc.close()--IO:"+e.getMessage());
			}
		}
		//确保r_buff可读
		r_buff.flip();
		byte[] read = new byte[r_buff.limit()];
		System.arraycopy(r_buff.array(),0,read,0,r_buff.limit());
		r_buff.clear();
		//log.debug(StringUtil.toHexString(read));
		AI ai = new AI();
		try{
			ai.setByte(read);
		}catch(ArrayIndexOutOfBoundsException ae){
			log.fatal(ae.getMessage());
			log.fatal("Err Read Clinet:"+StringUtil.toHexString(read));
		}
		ArrayList<AI> al = ai.GetAllSinglePackageArrayList();
		log.debug("Ai list "+ al.size());
		log.debug("HexString  "+StringUtil.toHexString(read)+"  String "+new String(read));
		for(AI a: al)
		{
			if(!a.isSendPackage()){
				if(a.isLoginPackage()){	
					ClientLogin(a,sc);
				}
				else if(a.isAddSimPackage()){
					//log.debug("add sim");
					ctm.addSIMToSIMList(a.getChangeSIMs()[0]);
				}
				else if(a.isDelSimPackage()){
					//log.debug("del sim");
					ctm.removeFromSIMList(a.getChangeSIMs()[0]);
				}
				else if(a.isUpdateSimPackage()){
					//log.debug("updata sim");
					ctm.updateSIMList(a.getChangeSIMs()[0], a.getChangeSIMs()[1]);
				}
			}else{
				if(ContainsSocketChannel(sc))//检查是否是已经登录合法的连接，这样才可以发送消息
				{
					csClient.SendToServerCache(read);
					//log.debug("Send To CSServer");
				}
			}
		}
	}
	/**
	 * 处理客户端登录包
	 * @param a
	 * @param sc
	 */
	private void ClientLogin(AI a,SocketChannel sc)
	{
		log.debug("Login Package! User:" + a.getLoginUsername() + "Pass : "+a.getLoginPassword().length());
		ClientTransmissionManager c = Clients.get(a.getLoginUsername());
		if(c!=null)
		{
			if(c.getLoginPassword().equals(a.getLoginPassword())||c.getLoginPassword().length()==0)
			{
				log.debug("get Client! Clinets ");
				c.CloseSocketChannel();
				c.setClientSocketChannel(sc);
			}
			else//用户名错误	
			{
				try	{
					log.warn("Password Err");
					sc.close();
				} catch (IOException e)	{
					log.warn("sc.close()--IO:"+e.getMessage());
				}
			}	
		}
		else
		{
			log.debug("Can't got Client!");
			Enumeration<String> keys = Clients.keys();
			while(keys.hasMoreElements())
			{
				log.debug(keys.nextElement());
			}
			try
			{
				log.debug("ClientSocketChannel close");
				sc.close();
			} catch (IOException e)
			{
				log.warn("sc.close()--IO:"+e.getMessage());
			}
		}
	}
	/**
	 * 反馈回客户端
	 * @param sc
	 * @throws IOException
	 */
	public void EchoToClient(SocketChannel sc) throws IOException{
		while(w_buff.hasRemaining())
		{
			sc.write(w_buff);
		}
	}
	/**
	 * 接收到消息后分发到各个线程判断 synchronized保证CAI这个类的cache不会被打乱
	 * 本程序暂时保住此方法不会被多线程同时调用，此方法暂时不同步
	 * @param msg
	 */
	public void sendMsgToAllClient(byte[] msg)
	{
		try{
			CAI.setByte(msg);
		}catch(ArrayIndexOutOfBoundsException ae){
			log.fatal(ae.getMessage());
			log.fatal("Err Msg:"+StringUtil.toHexString(msg));
		}
		ArrayList<AI> ais = CAI.GetAllSinglePackageArrayList();
		if(ais.size()>0)
		{
			for(AI a : ais)
			{
				Enumeration<ClientTransmissionManager> ctms =this.Clients.elements();
				ClientTransmissionManager ctm;
				while(ctms.hasMoreElements())
				{
					ctm = ctms.nextElement();
					ctm.sendMsgToClient(a);
				}
			}
		}
		/**
		 * 打印无法处理的数据包
		 */
		ArrayList<byte[]> ds = CAI.GetDiscards();
		if(ds!=null){
			for(byte[] d:ds)
			{
				log.warn("Process failure :"+StringUtil.toHexString(d));
			}
			CAI.ClearDiscards();
		}
	}
	/**
	 * 不做任何处理 统统发送到DS客户
	 * @param msg
	 */
	public void sendMsgToAllClientTransparent(byte[] msg)
	{
				Enumeration<ClientTransmissionManager> ctms =this.Clients.elements();
				ClientTransmissionManager ctm;
				while(ctms.hasMoreElements())
				{
					ctm = ctms.nextElement();
					ctm.sendMsgToClientWithoutCheck(msg);
				}

		
	}
	/**
	 * 遍历客户连接，检查是否是已经连接到的SocketChannel
	 * @param sc
	 * @return
	 */
	private boolean ContainsSocketChannel(SocketChannel sc)
	{
		if(sc!=null)
		{
			Enumeration<ClientTransmissionManager> ctms =this.Clients.elements();
			while(ctms.hasMoreElements())
			{
				if(ctms.nextElement().isMySocketChannel(sc))
				{
					return true;
				}
			}
		}
		return false;
	}
	@Override
	public void stopRun()
	{
		// TODO Auto-generated method stub
		ColseServerSocket();
		log.fatal("!!!!!!!!!Stop Server!!!!!!!!!!!!");
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
		String infos = AdminProtocol.GetNameAndStat(AdminProtocol.FristStatName,"Server");
		infos = AdminProtocol.GetStatsAll(infos, AdminProtocol.GetNameAndStat("Port",String.valueOf(port)));
		return infos;
	}
	@Override
	public String[] getStats()
	{
		
		String[] stats = new String[Clients.size()];
		Enumeration<ClientTransmissionManager> ctms =this.Clients.elements();
		int i = 0;
		while(ctms.hasMoreElements())
		{
			stats[i]=ctms.nextElement().getStatString();
			i++;
		}
		return stats;
	}
}
