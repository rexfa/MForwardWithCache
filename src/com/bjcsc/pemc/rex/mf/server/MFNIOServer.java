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
	private ByteBuffer r_buff = ByteBuffer.allocate(1024);//����ֻ���������Ϣ
	private ByteBuffer w_buff = ByteBuffer.allocate(1024);//debug
	private int port = 11155;//ȱʡ�˿�
	private volatile boolean stop = false; // ֹͣ��ǣ�ͬ�����Ʊ���
	private int setSoTimeout;
	private	Logger log;
	public Hashtable<String,ClientTransmissionManager> Clients;
	public CSClient csClient;
	private AI CAI = new AI();//�Խ�����Ϣ�ĸ��Ӵ���,����פ���ڷ�������
	
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
			//����һ���źż�����
			selector = Selector.open();
			//����һ��������
			ssc=ServerSocketChannel.open();
			//����������Ϊ�첽��ʽ
			ssc.configureBlocking(false);
			//�����˷���socket�󶨵�һ���˿�
			ss=ssc.socket();
			address = new InetSocketAddress(port);
			ss.bind(address);//backlog=7 �������Ӷ�������������ʱ����java.net.ConnectException: Connection refused
			//������������ѡ���첽�ź�OP_ACCEPT
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			log.warn("registered port~~");
			
			int nKey=0;
			while(true&&!stop)
			{
				nKey = selector.select();
				
				if (nKey == 0) //û��ָ����I/O�¼�����
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
						//����һ���µ�����
						SocketChannel channel=subssc.accept();//
						if (channel != null)
                        {  
							channel.configureBlocking(false);//���ø�socket���첽�ź�	
							//OP_READ:��socket�ɶ�ʱ��
							//��������DealwithData()
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

					    	//ĳsocket�ɶ��ź�
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
	 * ���������Ϣ�¼�
	 * @param key
	 */
	public void DealwithData(SelectionKey key)// throws IOException
	{
		int count = 0;
		long readCount =0;
		//��key��ȡָ��socketchannel������
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
				sc.close(); //�������Ӻ󲻷���Ϣ��ֱ��close��� �����ﴦ��
			} catch (IOException ioe){
				log.warn("Close io without msg "+ioe.getMessage());
			}
		}
		//�ͻ��˶Ͽ����Ӵ�����
		//n �����ݵ�ʱ�򷵻ض�ȡ�����ֽ�����
		//0 û�����ݲ���û�дﵽ����ĩ��ʱ����0��
		//-1 ���ﵽ��ĩ�˵�ʱ�򷵻�-1
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
		//ȷ��r_buff�ɶ�
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
				if(ContainsSocketChannel(sc))//����Ƿ����Ѿ���¼�Ϸ������ӣ������ſ��Է�����Ϣ
				{
					csClient.SendToServerCache(read);
					//log.debug("Send To CSServer");
				}
			}
		}
	}
	/**
	 * ����ͻ��˵�¼��
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
			else//�û�������	
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
	 * �����ؿͻ���
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
	 * ���յ���Ϣ��ַ��������߳��ж� synchronized��֤CAI������cache���ᱻ����
	 * ��������ʱ��ס�˷������ᱻ���߳�ͬʱ���ã��˷�����ʱ��ͬ��
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
		 * ��ӡ�޷���������ݰ�
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
	 * �����κδ��� ͳͳ���͵�DS�ͻ�
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
	 * �����ͻ����ӣ�����Ƿ����Ѿ����ӵ���SocketChannel
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
