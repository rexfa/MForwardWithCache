package com.bjcsc.pemc.rex.mf.protocol;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.zip.Checksum;
import java.util.zip.CRC32;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import com.bjcsc.pemc.rex.mf.util.StringUtil;

public class AI implements Protocol
{
	private final static byte[] HEAD={(byte) 0xE2,0x5C,0x4B,(byte) 0x89};
	
	private final static byte[] CMDLogin={0x00,0x03};
	
	private final static byte[] CMDAddSIM={0x01,0x01};
	private final static byte[] CMDDelSIM={0x01,0x02};
	private final static byte[] CMDUpdateSIM={0x01,0x03};
	
	private final static byte[] CMDSendToDev={0x00,0x00};
	private final static byte[] CMDSendToDevByUnicode={0x00,0x01};
	
	private final static byte[] LoginSuccess ={0x4f,0x4b};//String: OK
	
	private int lengthInt;
	private String msgSource;
	private byte[] head = new byte[4];
	private byte[] length = new byte[2];
	private byte[] msgCommand = new byte[2]; 
	private byte[] esuIP = new byte[14];
	private byte[] username = new byte[8];
	private byte[] password = new byte[8];
	private byte[] data;
	private byte[] checksum = new byte[2];
	
	private byte[] cacheFirstHalf=null;//�����ǰ���
	private byte[] cacheHalf = null;//����ĺ���
	private int cacheLength =0;
	private ArrayList<AI> ais = new ArrayList<AI>();//AIЭ�����
	private ArrayList<byte[]> Discards = new  ArrayList<byte[]>();//�������ݼ���
	public boolean isSingle = false;
	
	private byte[] array;
	public AI()
	{}
	public AI(byte[] Array)
	{
		setByte(Array);
	}
	/**
	 * ���ñ����ǵ�����Ϣ�� ������Ϣ������ �������ʧ���򷵻ط�
	 * @param Array
	 * @return
	 */
	public boolean setByteSingle(byte[] Array)
	{
		array =  new byte[Array.length];
		this.isSingle = true;
		System.arraycopy(Array,0,array,0,Array.length);
		return analysisSingle();
	}
	/**
	 * ������Ϣ������ �������ʧ���򷵻ط�
	 * @return
	 */
	public boolean analysisSingle()
	{
		
		int offset = 0;
		if(array.length>24)
		{
			//System.arraycopy(array, offset, head, 0, head.length);
			offset+=head.length;
			System.arraycopy(array, offset, length, 0, length.length);
			offset+=length.length;
			int packageLength = (int)byteArrayToShort(length);
			int packageLengthAndHL = packageLength+head.length+length.length;
			if(array.length != packageLengthAndHL){Discards.add(array);return false;}//���涪����
			System.arraycopy(array, offset, msgCommand, 0, msgCommand.length);
			offset+=msgCommand.length;
			System.arraycopy(array, offset, esuIP, 0, esuIP.length);
			offset+=esuIP.length;
			if(isLoginPackage())
			{
				System.arraycopy(array, offset, username, 0, username.length);
				offset+=username.length;
				System.arraycopy(array, offset, password, 0, password.length);
				offset+=password.length;
			}
			return true;
		}
		else
		{
			head = null;
			length=null;
			msgCommand = null;
			esuIP=null;
			return false;
		}
	}
	public void setByte(byte[] Array)
	{
		array =  new byte[Array.length];
		System.arraycopy(Array,0,array,0,Array.length);
		analysis();
	}
	@Override
	/**
	 * �������ķ��� ���Ĵ���
	 * ������ĺ���
	 */
	public void analysis()
	{
		int offset = 0;
		byte[] temp;
		int packageLength=0;
		int packageLengthAndHL=0;
		ais.clear();

		while(true){
			offset=0;
			packageLength=0;
			packageLengthAndHL=0;
			if(getHeadOffset(array)==-1){
				cacheHalf = new byte[array.length]; 
				System.arraycopy(array, 0, cacheHalf, 0,array.length);
				if(ais.size()==0&&cacheFirstHalf!=null)
				{
					DealSplitPackage();
				}
				Discards.add(array);
				return;//�жϵ�ѭ��ĩβ ����ͷ��ֱ���ӵ�,�����һ����������ͷ������ô����ǰ����û�а������cache
			}else{
				offset = getHeadOffset(array);
				temp = new byte[array.length-offset];
				System.arraycopy(array, offset, temp, 0,array.length-offset);//
				cacheHalf = new byte[offset]; //���Ǳ���ǰ�벿��ͷ�� *DealSplitPackage()ʵ�ִ�����
				System.arraycopy(array, 0, cacheHalf, 0,offset);
				array = temp;
			}
			DealSplitPackage();
			offset = 0;
			if(array.length>24){
				//System.arraycopy(array, offset, head, 0, head.length);
				offset+=head.length;
				System.arraycopy(array, offset, length, 0, length.length);
				offset+=length.length;
				packageLength = (int)byteArrayToShort(length);
				packageLengthAndHL = packageLength+head.length+length.length;
				if(packageLengthAndHL>array.length)	{//ʵ��array���Ȳ������ʶ���� �򲻼�������, ����cache
					cacheFirstHalf = new byte[array.length];
					System.arraycopy(array, 0, cacheFirstHalf, 0, array.length);
					break;
				}
				temp = new byte[packageLengthAndHL];
				System.arraycopy(array, 0, temp, 0, packageLengthAndHL);
				AI tmpAI = new AI();
				if(	tmpAI.setByteSingle(temp))
					ais.add(tmpAI);
				if(array.length>packageLengthAndHL)	{
					temp = new byte[array.length-packageLengthAndHL];
					System.arraycopy(array, packageLengthAndHL, temp, 0, temp.length);
					array = temp;
				}
				else{
					break;
				}
			}else{
				cacheFirstHalf = new byte[array.length];
				System.arraycopy(array, 0, cacheFirstHalf, 0, array.length);
				break;
			}
		}//while(true)
		//System.out.println("Analysis Finished ");
	}
	/**
	 * ����ֿ����������
	 */
	private void DealSplitPackage()
	{
		if(cacheFirstHalf!=null&&cacheHalf!=null)
		{
			byte[] temp = new byte[cacheFirstHalf.length+cacheHalf.length];
			System.arraycopy(cacheFirstHalf, 0, temp, 0, cacheFirstHalf.length);
			System.arraycopy(cacheHalf, 0, temp, cacheFirstHalf.length, cacheHalf.length);
			int headPost = getHeadOffset(temp);
			if(headPost==-1){
				Discards.add(temp);//���涪����
				cacheFirstHalf=null;
				cacheHalf=null;
				return;
			}else if(headPost>0)
			{
				byte[] temp2 = new byte[temp.length-headPost];
				System.arraycopy(temp, headPost, temp2, 0, temp2.length);
				temp = temp2;
			}
			AI ai =new AI();
			if(ai.setByteSingle(temp))
				ais.add(ai);
		}
		cacheFirstHalf=null;
		cacheHalf=null;
	}
	/**
	 * ���طǿյ�����Э�����ݰ�
	 * @return AI[]
	 */
	public  AI[] GetAllSinglePackageArray()
	{
		if(this.isSingle){ return null;}
		else{return this.ais.toArray(new AI[ais.size()]);}
	}
	/**
	 * ���طǿյ�����Э�����ݰ� 
	 * @return ArrayList<AI>
	 */
	public  ArrayList<AI> GetAllSinglePackageArrayList()
	{
		if(this.isSingle){ return null;}
		else{return ais;}
	}
	/**
	 * ����޷�����İ�����
	 * @return
	 */
	public ArrayList<byte[]> GetDiscards()
	{
		if(Discards.size()==0){
			return null;
		}else{
		return Discards;}
	}
	/**
	 * ����޷���������ݼ���
	 */
	public void ClearDiscards()
	{
		Discards.clear();
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
	 * ���ر�AI����byte����
	 * @return
	 */
	public  byte[] GetPackageByteArray()
	{
		return this.array;
	}
	/**
	 * ����AI����String��ʽ
	 * @return
	 */
	public String GetPackageToString()
	{
		return new String(this.array);
	}
	/**
	 * ��û�վsim
	 * @return
	 */
	public String getSIM()
	{
		//���ݿ�����13λ�ģ����Լ���1λ
		if(esuIP==null)
			return "";
		else
			return ((new String(esuIP)).substring(0, 13));
	}
	/**
	 * �ж��Ƿ��ǵ�¼��
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
	 * �Ƿ����·���
	 * @return
	 */
	public boolean isSendPackage()
	{

		if((CMDSendToDev[0]==msgCommand[0]&&CMDSendToDev[1]==msgCommand[1])
				||(CMDSendToDevByUnicode[0]==msgCommand[0]&&CMDSendToDevByUnicode[1]==msgCommand[1]))
			return true;
		else
			return false;
	}

	/**
	 * �ж��Ƿ��Ǽ�sim�������
	 * @return
	 */
	public boolean isAddSimPackage()
	{
		if(CMDAddSIM[0]==msgCommand[0]&&CMDAddSIM[1]==msgCommand[1])
			return true;
		else
			return false;
	}
	/**
	 * �ж��Ƿ���ɾ��sim�������
	 * @return
	 */
	public boolean isDelSimPackage()
	{
		if(CMDDelSIM[0]==msgCommand[0]&&CMDDelSIM[1]==msgCommand[1])
			return true;
		else
			return false;
	}
	/**
	 * �ж��Ƿ��Ǹ���sim�������
	 * @return
	 */
	public boolean isUpdateSimPackage()
	{
		if(CMDUpdateSIM[0]==msgCommand[0]&&CMDUpdateSIM[1]==msgCommand[1])
			return true;
		else
			return false;
	}
	/**
	 * �����Ҫ�޸ĵ�sim
	 * @return
	 */
	public String[] getChangeSIMs()
	{
		String[] sims = new String[2];
		if(this.isAddSimPackage()||this.isDelSimPackage())
		{
			byte[] sim = new byte[13];
			System.arraycopy(array,22, sim,0, sim.length);
			sims[0] = new String(sim);
		}
		else if(this.isUpdateSimPackage())
		{
			byte[] sim0 = new byte[13];
			byte[] sim1 = new byte[13];
			System.arraycopy(array,22, sim0,0, sim0.length);
			System.arraycopy(array,22+sim0.length, sim1,0, sim1.length);
			sims[0] = new String(sim0);
			sims[1] = new String(sim1);
		}
		return sims;
	}
	/**
	 * �ж��Ƿ���AI��
	 * @return
	 */
	public boolean isPackage()
	{
		if(HEAD[0]==head[0]&&HEAD[1]==head[1]&&HEAD[2]==head[2]&&HEAD[3]==head[3])
			return true;
		else
			return false;
	}
	/**
	 * ��õ�¼��
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
	 * ��õ�¼����
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
	 * �õ���¼byte��
	 * @param username
	 * @param password
	 * @return
	 */
	public byte[] getLoginPackage(String username ,String password)
	{
		int offset = 0;
		byte[] loginPackage = new byte[24+8+8];
		System.arraycopy(HEAD,0,loginPackage,offset,HEAD.length);
		offset = offset+HEAD.length;
		byte[] packageLength={0x00,(byte)(loginPackage.length-6)};
		System.arraycopy(packageLength,0,loginPackage,offset,packageLength.length);
		offset = offset+packageLength.length;
		System.arraycopy(CMDLogin,0,loginPackage,offset,CMDLogin.length);
		offset = offset+CMDLogin.length;
		
		//byte[] upb = (username+password).getBytes();
		byte[] upb = new byte[14];

		System.arraycopy(upb,0,loginPackage,offset,14);
		offset = offset+14;
		
		byte[] u = username.getBytes();
		System.arraycopy(u,0,loginPackage,offset,u.length);
		offset = offset+8;

		byte[] p = password.getBytes();
		System.arraycopy(p,0,loginPackage,offset,p.length);
		offset = offset+8;
		
		//byte[] ck = intToByteArray(4+2+2+14);
		//System.arraycopy(ck,2,loginPackage,offset,2);
	
		byte[] ck = byteSUM(loginPackage);
		System.arraycopy(ck,0,loginPackage,offset,2);
		return loginPackage;
	}
	/**
	 * ��ȡ��ͷ��λ�� 
	 * @param array
	 * @return
	 */
	private static int getHeadOffset(byte[] array)
	{
		if(array.length<4)
		{
			return -1;
		}
		int offset = 0;
		for(;offset<array.length-4;offset++)
		{
			//������λ ����λ����
			if(array[offset]==HEAD[0]&&array[offset+1]==HEAD[1]&&array[offset+2]==HEAD[2]&&array[offset+3]==HEAD[3])
			{
				return offset;
			}
		}
		return -1;
		
	}
	public static byte[] ChecksumByte(byte[] data, int start, int length)
	{
		Checksum checksum = new CRC32();
		checksum.update(data, start, length);
		long cs = checksum.getValue();
		byte[] cs0 =  java.lang.Long.toString(cs).getBytes();
        return cs0;
        
    }
	public static byte[] intToByteArray(int i) 
	{   
		byte[] result = new byte[4];   
		result[0] = (byte)((i >> 24) & 0xFF);
		result[1] = (byte)((i >> 16) & 0xFF);
		result[2] = (byte)((i >> 8) & 0xFF); 
		result[3] = (byte)(i & 0xFF);
		return result;
	}
	public static byte[] intToByteArray2(int i) throws Exception 
	{
		  ByteArrayOutputStream buf = new ByteArrayOutputStream();   
		  DataOutputStream out = new DataOutputStream(buf);   
		  out.writeInt(i);   
		  byte[] b = buf.toByteArray();
		  out.close();
		  buf.close();
		  return b;
	}
	public static byte[] shortToByteArray(short i)
	{
		byte[] result = new byte[2];   
		result[0] = (byte)((i >> 8) & 0xFF); 
		result[1] = (byte)(i & 0xFF);
		return result;
	}
	public static byte[] byteSUM(byte[] a)
	{
		short c=0;
		short sum=0;
		for(int i=0;i<a.length;i++)
		{
			c = (short)a[i];
			sum=(short) (c+sum);
		}
		return shortToByteArray(sum);
	}
    /**
     * Convert the byte array to an int.
     *
     * @param b The byte array
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }
    
    /**
     * Convert the byte array to an Short.
     *
     * @param b The byte array
     * @return The integer
     */
    public static short byteArrayToShort(byte[] b) {
        return byteArrayToShort(b, 0);
    }

    /**
     * Convert the byte array to an Short starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The Short integer
     */
    public static short byteArrayToShort(byte[] b, int offset) {
    	short value = 0;
        for (int i = 0; i < 2; i++) {
            int shift = (2 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }
    
    public static boolean IsLoginSuccess(byte[] array)
    {
    	if(array.length!=2)
    	{
    		return false;
    	}
    	if(array[0]==AI.LoginSuccess[0]&&array[1]==AI.LoginSuccess[1])
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
}
