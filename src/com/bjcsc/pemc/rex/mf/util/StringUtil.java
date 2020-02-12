package com.bjcsc.pemc.rex.mf.util;



public class StringUtil
{
	public static boolean isEmpty(String s)
	{
		if(s.length()>0)
			return false;
		else
			return true;
	}
	/**
	 * 16���Ƶ��ַ�����ʾת���ֽ�����
	 * 
	 * @param hexString
	 *            16���Ƹ�ʽ���ַ���
	 * @return ת������ֽ�����
	 **/
	public static byte[] toByteArray(String hexString) {
		if (StringUtil.isEmpty(hexString))
		{
			//throw new IllegalArgumentException("this hexString must not be empty");
			return null;
		}
 
		hexString = hexString.toLowerCase();
		final byte[] byteArray = new byte[hexString.length() / 2];
		int k = 0;
		for (int i = 0; i < byteArray.length; i++) {
                        //��Ϊ��16���ƣ����ֻ��ռ��4λ��ת�����ֽ���Ҫ����16���Ƶ��ַ�����λ����
			byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
			byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
			byteArray[i] = (byte) (high << 4 | low);
			k += 2;
		}
		return byteArray;
	}
	/**
	 * �ֽ�����ת��16���Ʊ�ʾ��ʽ���ַ���
	 * 
	 * @param byteArray
	 *            ��Ҫת�����ֽ�����
	 * @return 16���Ʊ�ʾ��ʽ���ַ���
	 **/
	public static String toHexString(byte[] byteArray) {
		if (byteArray == null || byteArray.length < 1){
			//throw new IllegalArgumentException("this byteArray must not be null or empty");
		return "Null Byte!"	;
		}
 
		final StringBuilder hexString = new StringBuilder();
		for (int i = 0; i < byteArray.length; i++) {
			if ((byteArray[i] & 0xff) < 0x10)//0~Fǰ�治��
				hexString.append("0");
			hexString.append(Integer.toHexString(0xFF & byteArray[i]));
		}
		return hexString.toString().toLowerCase();
	}
}
