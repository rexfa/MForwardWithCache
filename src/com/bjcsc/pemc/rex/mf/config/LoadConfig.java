package com.bjcsc.pemc.rex.mf.config;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
public class LoadConfig 
{
	 /**
	   * 在当前类路径或类的根路径下查找文件
	   * @param fileName
	   * @return Properties prop
	   */
	public static Properties load(String fileName) 
	{  
	  
		Properties prop = new Properties();    
	    InputStream is = LoadConfig.class.getResourceAsStream(fileName);
	     
	    try 
	    {
	    	if(is==null){//根路径下去试
	    		String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
	    		is =new FileInputStream(path+"/"+fileName);
	    		}
	    	prop.loadFromXML(is);
	    } 
	    catch (Exception e) 
	    {
	    	//throw new RuntimeException("导入属性文件出错!", e);
	    	System.out.println("导入属性文件出错!\n"+e.toString());
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
