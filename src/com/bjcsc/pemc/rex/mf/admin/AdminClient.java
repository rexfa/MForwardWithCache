package com.bjcsc.pemc.rex.mf.admin;

import java.awt.Insets;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.bjcsc.pemc.rex.mf.protocol.AdminProtocol;
import com.bjcsc.pemc.rex.mf.util.StringUtil;
import com.bjcsc.pemc.rex.mf.util.JTreeUtil;


public class AdminClient extends Thread
{
	private final String serverHost;
	private final int port;
	private final String adminName,adminPassword;
	private volatile boolean stop = false; // 停止标记，同步机制变量
	private Selector selector = null;
   	private SelectionKey skey =null;
	private SocketChannel sc=null;
	//private AdminConsolePad Pad=null;
	private AdminProtocol ap = new AdminProtocol();
	private AdminProtocol apCmd = new AdminProtocol();
	private JTree jTreeStats =null;
	private JCheckBox jCBUpdataEnble = null;
	private JScrollPane jSPTreeScroll =null;
	private JTextArea logTextArea = null;
	private JButton JBtnConncet =null;
	public static DefaultMutableTreeNode TreeRoot = new DefaultMutableTreeNode("MFServer--DSDs");;
	public static DefaultTreeModel TreeModel = new   DefaultTreeModel(TreeRoot);
    /*缓冲区大小*/  
    private static int BLOCK = 1024;  
    /*接受数据缓冲区*/  
    private static ByteBuffer sendbuffer = ByteBuffer.allocate(BLOCK); 
    /*发送数据缓冲区*/  
    private static ByteBuffer receivebuffer = ByteBuffer.allocate(BLOCK); 
    
    private static int testNum = 0;
	public AdminClient(String serverHost,String port,String adminName,String adminPassword)
	{
		this.serverHost = serverHost;
		this.port = Integer.valueOf(port);
		this.adminName = adminName;
		this.adminPassword = adminPassword;
	}
	/**
	 * 设置停止标记
	 * @param stop
	 */
	public void SetStop(boolean stop)
	{
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
			sc.close();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.stop = stop;
	}
	/**
	 * 设置界面
	 * @param Pad
	 */
	public void SetStatsTree(JTree jTreeStats,JCheckBox jCBUpdataEnble,JScrollPane jSPTreeScroll)
	{
		this.jTreeStats = jTreeStats;
		this.jCBUpdataEnble =jCBUpdataEnble;
		this.jSPTreeScroll = jSPTreeScroll;
	}
	public void SetLogTextArea(JTextArea logTextArea)
	{
		this.logTextArea= logTextArea;
	}
	public void SetBtnConncet(JButton JBtnConncet )
	{
		this.JBtnConncet = JBtnConncet;
	}
    public void run() 
    {
        while(!stop)
        {

           	int readCount = 0 ;
			try
			{
				selector = Selector.open();
				sc = SocketChannel.open();
				sc.configureBlocking(false);
				InetSocketAddress ica = new InetSocketAddress(serverHost,port);
				sc.connect(ica);
				skey = sc.register(selector,SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
			while(!stop)
            {
               	try
				{
					while (selector.select() > 0&&!stop) 
					{
						Set<SelectionKey> set = selector.selectedKeys();
						for (SelectionKey key : set) 
						{
							selector.selectedKeys().remove(key);
							int ops = key.readyOps();
							if ((ops & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
								if(sc.finishConnect())
								{
									skey = sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE );
									// 发送login命令
									sendLoginMsg();
								}
								else
								{
									stop= true;
								}
							}
							if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
								
								setLogin(true);
								readCount = sc.read(receivebuffer);
								if(readCount==-1)
								{
									stop = true;
									sc.close();									
									break;
								}
								receivebuffer.flip();
								byte[] rev = new byte[receivebuffer.limit()];
								System.arraycopy(receivebuffer.array(),0,rev,0,receivebuffer.limit());
								if(receivebuffer.limit()>30)
								{
									showInfo(rev);
								}
								else
								{
									showShotMsg(rev);
								}
								receivebuffer.clear();
							}
							if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
								skey = sc.register(selector, SelectionKey.OP_READ );
							}
						}
					}
				} catch (ClosedChannelException e)
				{
					e.printStackTrace();
					stop = true;
					break;
				} catch (IOException e)
				{
					e.printStackTrace();
					stop = true;
					break;
				}
            }//while(!stop&&!reconnect)
        }
        setLogin(false);
    }
    /**
     * 设置是否登录完毕
     * @param isLogin
     */
    private void setLogin(boolean isLogin)
    {
    	if(JBtnConncet!=null)
    	{
    		JBtnConncet.setEnabled(!isLogin);
    	}
    	showShotMsg(String.valueOf(isLogin));
    }
    /**
     * 发送登录消息
     */
    private void sendLoginMsg()
    {
     	sendbuffer.put(ap.getLoginPackage(adminName, adminPassword));
    	sendbuffer.flip();		
    	try	{
    		while(sendbuffer.hasRemaining()){
				sc.write(sendbuffer);
			} 
		}catch (IOException e){
				e.printStackTrace();
		}
		finally
		{
			sendbuffer.clear();
			ap.clearAll();
		}
    }
    /**
     * 显示消息
     * @param rev
     */
    private void showInfo(byte[] rev)
    {

    	ap.setByte(rev);
    	//System.out.println(StringUtil.toHexString(rev));
    	//System.out.println(ap.getDataString());
    	showInfoByTree(AdminProtocol.GetInfoArray(ap.getDataString()));
    	

    }
    /**
     * 用树显示
     * @param infos
     */
    private void showInfoByTree(String[] infos)
    {
    	if(jTreeStats==null||jSPTreeScroll==null)
    	{
    		return;
    	}
    	if(!jCBUpdataEnble.isSelected())
    	{
    		return;
    	}
    	jSPTreeScroll.setEnabled(false);
    	TreePath[] paths = jTreeStats.getSelectionPaths();
    	
    	DefaultMutableTreeNode RootNode =(DefaultMutableTreeNode)jTreeStats.getModel().getRoot();
    	if(RootNode.getChildCount()==0)
    	{
	    	for(String Stats:infos)
	    	{
	    		if(Stats.length()<4)
	    		{
	    			continue;
	    		}
	    		String[] stats = AdminProtocol.GetStatArray(Stats);
	    		DefaultMutableTreeNode statNode =null;
	    		for(String stat :stats)
	    		{
	    			if(stat.contains(AdminProtocol.FristStatName))
	    			{
	    				String[] NameStat = AdminProtocol.GetNameStatInfoArray(stat);
	    				statNode = new DefaultMutableTreeNode(NameStat[1]);
	    			}
	    			if(statNode!=null)
	    			{
	    				DefaultMutableTreeNode node = new DefaultMutableTreeNode(stat);
	    				statNode.add(node);
	    			}
	    		}
	    		TreeModel.insertNodeInto(statNode, RootNode,  RootNode.getChildCount());   
	    		
	    	}
    	}
    	else
    	{
     		Enumeration<DefaultMutableTreeNode> Childrens = RootNode.children();
    		while(Childrens.hasMoreElements())
    		{
    			DefaultMutableTreeNode node = Childrens.nextElement();
    	    	for(String Stats:infos)
    	    	{
    	    		String[] stats = AdminProtocol.GetStatArray(Stats);
    	    		for(String stat :stats)
    	    		{
    	    			
    	    			if(stat.contains(AdminProtocol.FristStatName))
    	    			{
    	    				String[] NameStat = AdminProtocol.GetNameStatInfoArray(stat);
    	    				if(JTreeUtil.getNodeText(node).equals(NameStat[1]))
    	    				{
    	    					Enumeration<DefaultMutableTreeNode> infoNodes = node.children();
    	    					//node.removeAllChildren();
    	    	   	    		for(String s :stats)
    	        	    		{	
    	    	    				//DefaultMutableTreeNode n = new DefaultMutableTreeNode(s);
    	    	    				//node.add(n);
    	    	    				while(infoNodes.hasMoreElements())
    	    	    				{
    	    	    					DefaultMutableTreeNode sn=infoNodes.nextElement();
    	    	    					//System.out.println(AdminProtocol.GetInfo(s)[0]+"===="+AdminProtocol.GetInfo((String)sn.getUserObject())[0]);
    	    	    					if(AdminProtocol.GetInfo(s)[0].equals(AdminProtocol.GetInfo((String)sn.getUserObject())[0]))
    	    	    					{
    	    	    						sn.setUserObject(s);
    	    	    						break;
    	    	    					}
    	    	    				}    	    	    				
    	        	    		}
    	    					//System.out.println(NameStat[1]);
    	    					break;
    	    				}
    	    			}
    	    		}
    	    	}
    		}
    	}
    	jTreeStats.setModel(TreeModel);
    	jTreeStats.updateUI();
    	JTreeUtil.expandAll(jTreeStats,new TreePath(RootNode),true);
    	if(paths!=null)
    	{
    		jTreeStats.setSelectionPath(paths[0]);
    	}
    	jSPTreeScroll.setEnabled(true);
    }
    
    public void testTree()
    {
		String info1 = AdminProtocol.GetNameAndStat(AdminProtocol.FristStatName,"test");
		info1 = AdminProtocol.GetStatsAll(info1, AdminProtocol.GetNameAndStat("Connected","false"+this.testNum));
		info1 = AdminProtocol.GetStatsAll(info1, AdminProtocol.GetNameAndStat("Queue","1000/2000"));
		info1 = AdminProtocol.GetStatsAll(info1, AdminProtocol.GetNameAndStat("Received","11111111"+this.testNum));
		info1 = AdminProtocol.GetStatsAll(info1, AdminProtocol.GetNameAndStat("SIMs","600"));
		
		String info2 = AdminProtocol.GetNameAndStat(AdminProtocol.FristStatName,"test1");
		info2 = AdminProtocol.GetStatsAll(info2, AdminProtocol.GetNameAndStat("Connected","false"+this.testNum));
		info2 = AdminProtocol.GetStatsAll(info2, AdminProtocol.GetNameAndStat("Queue","1000/2000"));
		info2 = AdminProtocol.GetStatsAll(info2, AdminProtocol.GetNameAndStat("Received","11111111"+this.testNum));
		info2 = AdminProtocol.GetStatsAll(info2, AdminProtocol.GetNameAndStat("SIMs","600"));
		
		String infos = AdminProtocol.GetALLInfo(info1, info2);
		//String [] t1 = infos.split("|");
		String[] t = AdminProtocol.GetInfoArray(infos);
		showInfoByTree(AdminProtocol.GetInfoArray(infos));
		this.testNum++;
    }
    /**
     * 发送命令到服务器 同步发送
     * @param cmd
     * @throws IOException 
     */
    public synchronized boolean sendCommand(String cmd)
    {
    	boolean re=false;
    	if(!apCmd.SetDataString(cmd)) return re;
       	sendbuffer.put(apCmd.makeCMDPackage());
    	sendbuffer.flip();		
    	try	{
    		while(sendbuffer.hasRemaining()){
				sc.write(sendbuffer);
				re = true;
			} 
		}catch (IOException e){
				e.printStackTrace();
				try
				{
					sc.close();
				} catch (IOException e1)
				{
					e1.printStackTrace();
				}
		}
		finally
		{
			sendbuffer.clear();
			apCmd.clearAll();
		}
		return re;
    }
    
    private void showShotMsg(byte[] msg)
    {
    	ap.setByte(msg);
    	//ap.getDataString();
    	if(logTextArea!=null)
    		logTextArea.setText(logTextArea.getText()+"\n\r"+ap.getDataString());
    }
    private void showShotMsg(String msg)
    {
    	if(logTextArea!=null)
    		logTextArea.setText(logTextArea.getText()+"\n\r"+msg);
    }
    
    
}
