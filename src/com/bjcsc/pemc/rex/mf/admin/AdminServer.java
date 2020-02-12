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
	private ByteBuffer r_buff = ByteBuffer.allocate(1024);//����ֻ���������Ϣ
	private ByteBuffer w_buff = ByteBuffer.allocate(1024);//debug
	private int port = 11158;//ȱʡ�˿�
	private volatile boolean stop = false; // ֹͣ��ǣ�ͬ�����Ʊ���
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
			//����һ���źż�����
			Selector selector = Selector.open();
			//����һ��������
			ServerSocketChannel ssc=ServerSocketChannel.open();
			//����������Ϊ�첽��ʽ
			ssc.configureBlocking(false);
			//�����˷���socket�󶨵�һ���˿�
			ServerSocket ss=ssc.socket();
			InetSocketAddress address = new InetSocketAddress(port);
			ss.bind(address);//backlog=7 �������Ӷ�������������ʱ����java.net.ConnectException: Connection refused
			//������������ѡ���첽�ź�OP_ACCEPT
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			//log.warn("registered port~~");
			log.fatal("AdminServer Start....");
			int nKey=0;
			while(true)
			{
				nKey = selector.select();
				
				if (nKey == 0) //û��ָ����I/O�¼�����
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
                            //System.out.println("---No Connection---");  
                        } 
						iter.remove();
						//System.out.println("��������:"+channel);

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
			log.error(e.getMessage());
		}
	}
	public void DealwithData(SelectionKey key)// throws IOException
	{
		int count = 0;
		long readCount =0;
		//��key��ȡָ��socketchannel������
		SocketChannel sc = (SocketChannel)key.channel();
		r_buff.clear();
		//��ȡ���ݵ�r_buff
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
				sc.close();//�������Ӻ󲻷���Ϣ��ֱ��close��� �����ﴦ��
			} catch (IOException ioe){
				log.warn("Close io without msg "+ioe.getMessage());
			}
			return;
		}
		//�ͻ��˶Ͽ����Ӵ�����
		if(count==-1)
		{
			sit.delSocketChannel(sc);
		}
		//ȷ��r_buff�ɶ�
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
			else//�û�������
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
	 * ��ȡ������״̬��Ϣ
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
	 * �õ��������ݵ��ַ���
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
