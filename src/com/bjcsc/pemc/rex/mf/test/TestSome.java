package com.bjcsc.pemc.rex.mf.test;
import com.bjcsc.pemc.rex.mf.protocol.*;
import com.bjcsc.pemc.rex.mf.util.*;

public class TestSome
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		//byte[] a1 = {0x01,0x01};
		//short i = AI.byteArrayToShort(a1);
		//int ii = (int)i;
		//System.out.println(String.valueOf(ii));
		String ai1 = "11e25c4b89007600023836313339313139343435323800effffffe0b04160e390c503f00160400010001035668fe010002035659fe01000303579dfe010004030000fe010005030000fe010006030000fe01000703000000010008030000000100090300000001000a0300000001000b030000fe01000c031388fe05ad";
		String ai2 = "e25c4b89007600023836313339313035363437333700effffffe0b04160f0d17503f00160400010001035a24fe01000203590bfe010003035a86fe010004030000fe010005030000fe010006030000fe01000703000000010008030000000100090300000001000a0300000001000b030000fe01000c031388fe04ed";
		String ai3 = "e25c4b89002400023836313335303133363639373400effffffe0b04160f0139500100020348b1fe0389"; 
		String ai4_1 = "e25c4b89002400023836313339313035333236343500effffffe";
		String ai4_2 = "0b04160e38005001000b030052fe03e2e25c4b89002400023836313335303133363639373400effffffe0b04160f0139500100020348b1fe0389";
		
		String ai5_1 = "e25c4b89001d010200000000000000000000000000003836313338303031333838383804e0";
		//                                              ^^^^
		
		byte[] aia= StringUtil.toByteArray(ai1+ai2+ai3+ai4_1);
		byte[] aia2= StringUtil.toByteArray(ai4_2);
		AI ai = new AI();
		ai.setByte(aia);
		AI[] a = ai.GetAllSinglePackageArray();
		for(AI atp:a)
		{
			System.out.println(StringUtil.toHexString(atp.GetPackageByteArray()));
		}
		
		ai.setByte(aia2);
		
		a = ai.GetAllSinglePackageArray();
		for(AI atp:a)
		{
			System.out.println(StringUtil.toHexString(atp.GetPackageByteArray()));
		}
		byte[] aiss= StringUtil.toByteArray(ai5_1);
		AI a2 = new AI();
		a2.setByte(aiss);
		a = a2.GetAllSinglePackageArray();
		for(AI atp:a)
		{
			
			System.out.println("add del update ? :"+atp.isDelSimPackage());
		}
	}

}
