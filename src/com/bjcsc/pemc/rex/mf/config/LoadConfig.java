package com.bjcsc.pemc.rex.mf.config;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
public class LoadConfig 
{
	 /**
	   * �ڵ�ǰ��·������ĸ�·���²����ļ�
	   * @param fileName
	   * @return Properties prop
	   */
	public static Properties load(String fileName) 
	{  
	  
		Properties prop = new Properties();    
	    InputStream is = LoadConfig.class.getResourceAsStream(fileName);
	     
	    try 
	    {
	    	if(is==null){//��·����ȥ��
	    		String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
	    		is =new FileInputStream(path+"/"+fileName);
	    		}
	    	prop.loadFromXML(is);
	    } 
	    catch (Exception e) 
	    {
	    	//throw new RuntimeException("���������ļ�����!", e);
	    	System.out.println("���������ļ�����!\n"+e.toString());
	    }
	    finally 
	    {
	    	if (is != null)
	    	{
	    		try 
	    		{
	    			is.close();
	    		}
	    		catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
	    }
	    return prop;
	}
}
