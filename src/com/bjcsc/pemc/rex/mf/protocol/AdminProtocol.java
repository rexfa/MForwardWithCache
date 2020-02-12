package com.bjcsc.pemc.rex.mf.protocol;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public final class AdminProtocol implements Protocol
{
	private final static byte[] HEAD={(byte) 0xE2,0x5C,0x4B,(byte) 0x89};
	private final static byte[] CMDLogin={0x00,0x03};
	private final static byte[] CMDInfo={0x02,0x00};
	private final static byte[] CMDCommand={0x04,0x00};
	
	public final static String INFOSegmentation="#";//数据内容的分割符
	public final static String INFONameMsgSegmentation="_";//数据名称和消息的分割符
	public final static String INFOMsgSegmentation=";";//不同数据消息的分割符
	public final static String FristStatName = "Name";
	private byte[] array;
	private byte[] head = new byte[4];
	private byte[] msgCommand = new byte[2];
	private byte[] data;
	private String dataS=null;
	
	private byte[] username = new byte[8];
	private byte[] password = new byte[8];
	
	public AdminProtocol()
	{
		
	}
	public AdminProtocol(byte[] Array)
	{
		setByte(Array);
	}
	public void setByte(byte[] Array)
	{
		array =  new byte[Array.length];
		System.arraycopy(Array,0,array,0,Array.length);
		analysis();
	}
	@Override
	public void analysis()
	{
		// TODO Auto-generated method stub
		int offset = 0;
		if(array.length>16)
		{
			//System.arraycopy(array, offset, head, 0, head.length);
			offset+=head.length;

			System.arraycopy(array, offset, msgCommand, 0, msgCommand.length);
			offset+=msgCommand.length;
			data = new byte[array.length-offset];
			System.arraycopy(array, offset, data, 0, data.length);
			//offset+=data.length;
			if(isLoginPackage())
			{
				//登录名和密码就在data中
				System.arraycopy(array, offset, username, 0, username.length);
				offset+=username.length;
				System.arraycopy(array, offset, password, 0, password.length);
				offset+=password.length;
			}
		}
		
	}

	@Override
	public void makePackage()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Check()
	{
		// TODO Auto-generated method stub
		
	}
	/**
	 * 判断是否是登录包
	 * @return
	 */
	public boolean isLoginPackage()
	{
		if(CMDLogin[0]==msgCommand[0]&&CMDLogin[1]==msgCommand[1])
			return true;
		else
			return false;
	}
	/**
	 * 判断是否是信息包
	 * @return
	 */
	public boolean isInfoPackage()
	{
		if(CMDInfo[0]==msgCommand[0]&&CMDInfo[1]==msgCommand[1])
			return true;
		else
			return false;
	}
	/**
	 * 判断是否是命令包
	 * @return
	 */
	public boolean isCMDPackage()
	{
		if(CMDCommand[0]==msgCommand[0]&&CMDCommand[1]==msgCommand[1])
			return true;
		else
			return false;
	}
	/**
	 * 获得登录名
	 * @return
	 */
	public String getLoginUsername()
	{
		int offset=0;
		for(;offset<username.length;offset++)
		{
			if(username[offset]==0)
			{
				break;
			}
		}
		byte[] un = new byte[offset];
		System.arraycopy(username, 0, un, 0, un.length);
		return new String(un);
	}
	/**
	 * 获得登录密码
	 * @return
	 */
	public String getLoginPassword()
	{
		//String s = new String(password);
		int offset=0;
		for(;offset<password.length;offset++)
		{
			if(password[offset]==0)
			{
				break;
			}
		}
		byte[] pw = new byte[offset];
		System.arraycopy(password, 0, pw, 0, pw.length);
		return new String(pw);
	}
	/**
	 * 设置包内容
	 * @param data
	 */
	public void SetData(byte[] data)
	{
		System.arraycopy(data,0,this.data,0,data.length);
	}
	
	public void SetInfoPackage(String infoName,String infoMsg)
	{

		//byte[] in = infoName.getBytes("ISO-8859-1");
		if(dataS==null)
		{
			dataS = infoName+INFONameMsgSegmentation+infoMsg;
		}
		else
		{
			dataS = dataS + INFOSegmentation+infoName+INFONameMsgSegmentation+infoMsg;
		}

	}
	/**
	 * 把提前做好的String数据转移成Byte数据
	 * @return
	 */
	public boolean SetDataByte()
	{
		try
		{
			data = dataS.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 把提前做好的Byte数据转移成String数据
	 * @return
	 */
	public boolean SetDataString()
	{
		try
		{
			dataS = new String(data,"ISO-8859-1");
		} catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * 把提前做好的Byte数据转移成String数据
	 * @return
	 */
	public boolean SetDataString(String dataNew)
	{
		try
		{
			dataS = dataNew;
			data = dataS.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * 获得data字符串
	 * @return
	 */
	public String getDataString()
	{
		try
		{
			return new String(data,"ISO-8859-1");
		} catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 拆分信息条目
	 * @return
	 */
	public String[] GetInfoList()
	{
		return dataS.split(INFOSegmentation);
	}
	/**
	 * 拆分信息，把name和msg分开
	 * @param NameAndMsg
	 * @return String[0]:Name String[1]:Msg
	 */
	public static String[] GetInfo(String NameAndMsg)
	{
		return NameAndMsg.split(INFONameMsgSegmentation);
	}
	/**
	 * 得到管理数据的字符串
	 * @param name
	 * @param info
	 * @return
	 */
	public static String GetNameAndStat(String name,String info)
	{
		return name+AdminProtocol.INFONameMsgSegmentation+info;
	}
	/**
	 * 在老的字符串加上新的消息
	 * @param infos
	 * @param newinfo
	 * @return
	 */
	public static String GetStatsAll(String infos,String newinfo)
	{
		return infos+AdminProtocol.INFOMsgSegmentation+newinfo;
	}
	/**
	 * 组合成整个的info包
	 * @param info1
	 * @param info2
	 * @return
	 */
	public static String GetALLInfo(String info1,String info2)
	{
		return info1+AdminProtocol.INFOSegmentation+info2;
	}
	/**
	 * 拆分info包
	 * @param infos
	 * @return
	 */
	public static String[] GetInfoArray(String infos)
	{
		return infos.split(AdminProtocol.INFOSegmentation);
	}
	/**
	 * 拆分一个消息的所有状态
	 * @param Stats
	 * @return
	 */
	public static String[] GetStatArray(String Stats)
	{
		return Stats.split(AdminProtocol.INFOMsgSegmentation);
	}
	/**
	 * 拆分一个状态的名称和消息
	 * @param Stat
	 * @return
	 */
	public static String[] GetNameStatInfoArray(String Stat)
	{
		return Stat.split(AdminProtocol.INFONameMsgSegmentation);
	}
	/**
	 * 制作消息包
	 */
	public byte[] makeInfoPackage()
	{
		array = new byte[HEAD.length+CMDInfo.length+data.length];
		int offset=0;
		System.arraycopy(HEAD, 0, array, offset, HEAD.length);
		offset=offset+HEAD.length;
		System.arraycopy(CMDInfo, 0, array, offset, CMDInfo.length);
		offset=offset+CMDInfo.length;
		System.arraycopy(data, 0, array, offset, data.length);
		offset=offset+data.length;
		return array;
	}
	/**
	 * 制作命令包
	 * @return
	 */
	public byte[] makeCMDPackage()
	{
		array = new byte[HEAD.length+CMDCommand.length+data.length];
		int offset=0;
		System.arraycopy(HEAD, 0, array, offset, HEAD.length);
		offset=offset+HEAD.length;
		System.arraycopy(CMDCommand, 0, array, offset, CMDCommand.length);
		offset=offset+CMDCommand.length;
		System.arraycopy(data, 0, array, offset, data.length);
		offset=offset+data.length;
		return array;
	}
	/**
	 * 获得登录包
	 * @param name
	 * @param password
	 * @return
	 */
	public byte[] getLoginPackage(String name,String password)
	{
		array = new byte[HEAD.length+CMDLogin.length+16];
		int offset=0;
		System.arraycopy(HEAD, 0, array, offset, HEAD.length);
		offset=offset+HEAD.length;
		System.arraycopy(CMDLogin, 0, array, offset, CMDLogin.length);
		offset=offset+CMDLogin.length;
		if(name.length()>8)
		{
			name = name.substring(0, 8);
		}
		byte[] u = name.getBytes();
		System.arraycopy(u,0,array,offset,u.length);
		offset = offset+8;
		if(password.length()>8)
		{
			password = password.substring(0, 8);
		}
		byte[] p = password.getBytes();
		System.arraycopy(p,0,array,offset,p.length);
		offset = offset+8;
		return array;
	}
	/**
	 * 清空包
	 */
	public void clearAll()
	{
		array = null;
		data = null;
		dataS = null;
	}
	
}
