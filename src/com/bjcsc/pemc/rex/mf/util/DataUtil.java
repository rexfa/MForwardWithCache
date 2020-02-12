package com.bjcsc.pemc.rex.mf.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DataUtil
{
    public   static   String   getDateTime()   { 
        Date   dateTime   =   new   Date(); 
        SimpleDateFormat   format   =   new   SimpleDateFormat( "yyyy-MM-dd   HH:mm:ss "); 
        String   strTime   =   format.format(dateTime); 
        return   strTime; 
    }
    public   static   String   getDate()   { 
        Date   dateTime   =   new   Date(); 
        SimpleDateFormat   format   =   new   SimpleDateFormat( "yyyy-MM-dd"); 
        String   strTime   =   format.format(dateTime); 
        return   strTime; 
    }
    public   static   String   getDateHour()   { 
        Date   dateTime   =   new   Date(); 
        SimpleDateFormat   format   =   new   SimpleDateFormat( "yyyy-MM-dd-HH"); 
        String   strTime   =   format.format(dateTime); 
        return   strTime; 
    }
}
