    /*
     * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apexsimulator;
import static apexsimulator.APEXSimulator._inst;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This method will flow the instruction into 
 * stages - Fetch, Decode, Execute, Memory, Writeback
 * @author pooja
 */
public class Stimulate {
    
    public static int pc =0;    // PC value to fetch new instuction
    public static int cycle = 1; // number of cycles
    public static  HashMap<Integer, Integer> _mem = new HashMap(); // Memory 
    public static  HashMap<String, Integer> registerFile = new HashMap(); // Register file
    static  Instruction Fetch, DecodeRF, INTFU, MUL1,MUL2, DIV1, DIV2, DIV3, DIV4, Memory, Writeback; // Static object of Instruction class
    ArrayList<Register> lstRegValue = new ArrayList<Register>(); // List of register to check if it is valid or not
    boolean isBusy = false; // It will check if the current stage is available to take a fetch instruction.
    boolean isHalt = false; // It will check if the instruction is HALT.
    boolean isHaltinWBStage = false; //It will be set to true only when the HALT is in WriteBack stage. 
    ArrayList<String> lstInstrWritten = new ArrayList<>(); // list of the instruction is written in register file
    boolean allInstWritten = false; // Check if all instruction is written in the WB stage.
    boolean isDecodeDone = false;
    boolean doNotFetch = false;
    Forward Forwarding = new Forward();
    Latch CacheLatch = new Latch();
    ArrayList<Forward> lstForwarding = new ArrayList<Forward>();
    PSWFlag psw = new PSWFlag();
    boolean isforwardexestalled = false;
    boolean src1set = false;
    boolean src2set = false;
    boolean src3set = false;
    boolean isforwardsrc2valid = false;
    int src1 = 0;
    int src2 = 0;
    int src3 = 0;
    ArrayList<Instruction> lstPswForwardInst = new ArrayList<Instruction>();
    ArrayList<Instruction> lstPswInst = new ArrayList<Instruction>();
    
    public void Stimulate(int clockcycle)
    {   
        for  (int i = 0; i <= 16; i++)
        {
          String RegisterName = "R"+i;
          boolean isValid = true;
          int Values = -1;
          Register regValue = new Register(RegisterName,isValid );
          lstRegValue.add(regValue);
        }
        while ( cycle <= clockcycle)
        {
        System.out.println(" ");
        System.out.println("cycle - " + cycle);
         WriteBack();
         Memory(); 
         DIV4();
         DIV3();
         DIV2();
         DIV1();      
         MUL2();
         MUL1();
         INTFU();
         DecodeRF(); 
         Fetch(); 
         cycle = cycle  + 1;
         if(isHaltinWBStage == true)
         {
             break;
         }
        }     
    }    
   
    public void Fetch()
    {
          if(pc < _inst.size() )
         {
             if (doNotFetch == true )
                {
                    doNotFetch = false;
                    Fetch = null;
                    System.out.println("Fetch : Empty");
                 } 
            else if(DecodeRF == null)
                {
                  Fetch = _inst.get(pc); 
                  Fetch.setpc_value(pc);
                  DecodeRF = Fetch;  
                  Fetch.setStall("");  
                  pc++; 
                System.out.println("Fetch : "+ Fetch.getinstruction_number() + " "+ Fetch.getinstruction_string() + " " +  Fetch.getstall());               
                }
                else
                {
                  Fetch = _inst.get(pc);
                  Fetch = Fetch;  
                  Fetch.setStall("Stalled"); 
                System.out.println("Fetch : "+ Fetch.getinstruction_number() + " "+ Fetch.getinstruction_string() + " " +  Fetch.getstall());               

                }
         }
         else if (pc == _inst.size()  && isDecodeDone == true)
          {
              Fetch = null;
               System.out.println("Fetch : Empty");
          }
          else
         {
            Fetch = null;
            System.out.println("Fetch : Empty - HALT");
         }
    }
    
     public  void DecodeRF()
     {      
          if(DecodeRF == null)
         {
            // isDecodeDone = true;
             System.out.println("DRF : Empty");
                     
         }
         else
         { 
            String[] temp_split = DecodeRF.getinstruction_string().replaceAll("#", "").trim().split(",");
           DecodeRF.setopcode((temp_split.length > 0 &&!temp_split[0].isEmpty()) ? temp_split[0] : null); 
           if(DecodeRF != null && "MOVC".equals(DecodeRF.getopcode()))
           {
               DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty())
               {
                 DecodeRF.setLiteral(temp_split[2]);
               }
               
              String s1 =  DecodeRF.getsrc1();
              int s = Integer.parseInt(s1.replace("R", ""));
              boolean isValid = lstRegValue.get(s).getisValid();
             
            if(isValid == true && INTFU == null)
             {
                    INTFU = DecodeRF; 
                    lstRegValue.get(s).setisValid(false);
                    DecodeRF.setStall("");
                    System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());              
                    DecodeRF = null;
                    isDecodeDone = true;
                   isforwardexestalled = false;
             }
             else
             {
                 DecodeRF = DecodeRF;
                 DecodeRF.setStall("stalled");
                 isforwardexestalled = true;
                  System.out.println("DRF : "+DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                isDecodeDone = false;
             }
               
           }          
           
            if(DecodeRF != null && "STORE".equals(DecodeRF.getopcode()))
           {
               DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
              DecodeRF.setsrc2(temp_split[2]);
           }
                if(temp_split.length > 3 &&!temp_split[3].isEmpty()){
              DecodeRF.setLiteral(temp_split[3]);
            }
                
                    String s1 = DecodeRF.getsrc1();
                String s2 = DecodeRF.getsrc2();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                 String ss1 = "";
                         Integer sv1 = 0 ;
                          String ss2 = "";
                         Integer sv2 = 0;
                         Integer r = 0;
                        for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc1()))
                         {
                            if( lstRegValue.get(sr1).getisValid() == true )
                            {
                          ss1 = key;
                          sv1 = value;
                         src1 = sv1;
                         DecodeRF.isSrc1Set = true;
                             }
                         }
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc2()))
                         {
                              if( lstRegValue.get(sr2).getisValid() == true )
                            {
                          ss2 = key;
                          sv2 = value;
                           src2 = sv2;
                         DecodeRF.isSrc2Set = true;
                            }
                         }
                     }
                for(Forward str : lstForwarding)
                {
                 String s = str.getaddress();
                    
                 if (s.equalsIgnoreCase(s1))
                 {
                     if(DecodeRF.isSrc1Set == false)
                     {
                         src1 = Integer.parseInt( str.getdata());
                         DecodeRF.isSrc1Set = true;  
                         
                     }  
                     
                 }
                 if (s.equalsIgnoreCase(s2))
                 {
                       if(DecodeRF.isSrc2Set == false)
                     {
                     src2 = Integer.parseInt( str.getdata());
                     DecodeRF.isSrc2Set = true;
                     }
                 }
                }                 
                
                if( INTFU == null )
                { 
                    if(DecodeRF.isSrc1Set == true && DecodeRF.isSrc2Set == true)
                    {
                        DecodeRF.setsrc1data(src1);
                         DecodeRF.setsrc2data(src2);
                        INTFU = DecodeRF; 
                        DecodeRF.setStall("");
                        System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                       
                       isforwardexestalled = false;
                       DecodeRF.isSrc1Set = false;
                       DecodeRF.isSrc2Set = false;
                       DecodeRF.isSrc3Set = false;
                        DecodeRF = null;
                        isDecodeDone = true;
                    }
                    else
                    {
                       DecodeRF = DecodeRF;
                       DecodeRF.setStall("stalled");
                       System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                       isDecodeDone = false;
                       
                   isforwardexestalled = true;
                    }
                
                }
                else
                {
                DecodeRF = DecodeRF;
                DecodeRF.setStall("stalled");
                System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                isDecodeDone = false;
                
                   isforwardexestalled = true;
                }
             
           }
            
             if(DecodeRF != null && "LOAD".equals(DecodeRF.getopcode()))
           {
            DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
              DecodeRF.setsrc2(temp_split[2]);
           }
                if(temp_split.length > 3 &&!temp_split[3].isEmpty()){
              DecodeRF.setLiteral(temp_split[3]);
            }           
                String s1 = DecodeRF.getsrc1();
                String s2 = DecodeRF.getsrc2();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", "")); 
                String ss1 = "";
                         Integer sv1 = 0 ;
                          String ss2 = "";
                         Integer sv2 = 0;
                         Integer r = 0;
                        for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc2()))
                         {
                              if( lstRegValue.get(sr2).getisValid() == true )
                            {
                          ss2 = key;
                          sv2 = value;
                           src2 = sv2;
                         DecodeRF.isSrc2Set = true;
                            }
                         }
                     }
                for(Forward str : lstForwarding)
                {
                 String s = str.getaddress();
                 if (s.equalsIgnoreCase(s2))
                 {
                       if(DecodeRF.isSrc2Set == false)
                     {
                    src2 = Integer.parseInt( str.getdata());
                     DecodeRF.isSrc2Set = true;
                     }
                 }
                }
                if(lstRegValue.get(sr1).getisValid() == true && INTFU == null)
                {    
                    if (  DecodeRF.isSrc2Set = true)
                    {
                        DecodeRF.setsrc2data(src2);
                        INTFU = DecodeRF; 
                        lstRegValue.get(sr1).setisValid(false);
                        DecodeRF.setStall("");
                        System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                        
                       isforwardexestalled = false;
                       DecodeRF.isSrc1Set = false;
                       DecodeRF.isSrc2Set = false;
                       DecodeRF.isSrc3Set = false;
                        DecodeRF = null;
                        isDecodeDone = true;
                    }
                 else
                    {
                        DecodeRF = DecodeRF;
                        DecodeRF.setStall("stalled");
                         System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                        isDecodeDone = false;
                        
                   isforwardexestalled = true;
                    }
                }
                else
                {
                 DecodeRF = DecodeRF;
                 DecodeRF.setStall("stalled");
                   System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                isDecodeDone = false;
                
                   isforwardexestalled = true;
                }
           }
           
            
             
             if(DecodeRF != null && ("ADD".equals(DecodeRF.getopcode()) 
                      ||"SUB".equals(DecodeRF.getopcode()) ) )
              {
                  DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
                  DecodeRF.setsrc2(temp_split[2]);
                }
               
              if(temp_split.length > 3 &&!temp_split[3].isEmpty()){
                 DecodeRF.setsrc3(temp_split[3]);
                 }
                
                String s1 = DecodeRF.getsrc1();
                String s2 = DecodeRF.getsrc2();
                String s3 = DecodeRF.getsrc3();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                int sr3 = Integer.parseInt(s3.replace("R", "")); 
                 String ss3 = "";
                         Integer sv3 = 0 ;
                          String ss2 = "";
                         Integer sv2 = 0;
                         Integer r = 0;
                        for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc2()))
                         {
                              if( lstRegValue.get(sr2).getisValid() == true )
                            {
                          ss2 = key;
                          sv2 = value;
                         src2 = sv2;
                         DecodeRF.isSrc2Set = true;
                         }}
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc3()))
                         { if( lstRegValue.get(sr3).getisValid() == true )
                            {
                          ss3 = key;
                          sv3 = value;
                           src3 = sv3;
                         DecodeRF.isSrc3Set = true;
                         }}
                     }
                for(Forward str : lstForwarding)
                {
                 String s = str.getaddress();
                    
                 if (s.equalsIgnoreCase(s2))
                 {
                     if(DecodeRF.isSrc2Set == false)
                     {
                         src2 = Integer.parseInt( str.getdata());
                         DecodeRF.isSrc2Set = true;  
                         
                     }              
                 }
                 if (s.equalsIgnoreCase(s3))
                 {
                       if(DecodeRF.isSrc3Set == false)
                     {
                     src3 = Integer.parseInt( str.getdata());
                     DecodeRF.isSrc3Set = true;
                     }
                 }
                }               
                // check the forwarding bus if it has destination value                        
                if(lstRegValue.get(sr1).getisValid() == true && INTFU == null)
                {
                    if(DecodeRF.isSrc3Set == true && DecodeRF.isSrc2Set == true )
                    {
                         DecodeRF.setsrc2data(src2);
                         DecodeRF.setsrc3data(src3);
                         psw.ispswValid = false;
                          INTFU = DecodeRF; 
                          lstRegValue.get(sr1).setisValid(false);
                          DecodeRF.setStall("");
                          System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                          isBusy = false;
                       isforwardexestalled = false;
                       DecodeRF.isSrc1Set = false;
                       DecodeRF.isSrc2Set = false;
                       DecodeRF.isSrc3Set = false;
                          DecodeRF = null;
                          isDecodeDone = true;
                          
                    }
                    else
                    {
                         DecodeRF = DecodeRF;
                        DecodeRF.setStall("stalled");
                        isBusy = true;
                        System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                        isDecodeDone = false;
                        
                   isforwardexestalled = true;
                    }
                    
                }
                else
                {
                DecodeRF = DecodeRF;
                DecodeRF.setStall("stalled");
                isBusy = true;
                   System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
               isDecodeDone = false;
               
                   isforwardexestalled = true;
                }
           
              }
              
             
             
              if(DecodeRF != null && ( "EXOR".equals(DecodeRF.getopcode()) ||  "OR".equals(DecodeRF.getopcode()) ||  "AND".equals(DecodeRF.getopcode())) )
              {
                  DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
                  DecodeRF.setsrc2(temp_split[2]);
                }
               
              if(temp_split.length > 3 &&!temp_split[3].isEmpty()){
                 DecodeRF.setsrc3(temp_split[3]);
                 }
                
                 String s1 = DecodeRF.getsrc1();
                String s2 = DecodeRF.getsrc2();
                String s3 = DecodeRF.getsrc3();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                int sr3 = Integer.parseInt(s3.replace("R", "")); 
                boolean f2 = false;
                boolean f3 = false;
                 String ss3 = "";
                         Integer sv3 = 0 ;
                          String ss2 = "";
                         Integer sv2 = 0;
                         Integer r = 0;
                        for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc2()))
                         {
                              if( lstRegValue.get(sr2).getisValid() == true )
                            {
                          ss2 = key;
                          sv2 = value;
                         src2 = sv2;
                         DecodeRF.isSrc2Set = true;
                         }
                         }
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc3()))
                         {
                              if( lstRegValue.get(sr3).getisValid() == true )
                            {
                          ss3 = key;
                          sv3 = value;
                           src3 = sv3;
                         DecodeRF.isSrc3Set = true;
                         }}
                     }
                for(Forward str : lstForwarding)
                {
                 String s = str.getaddress();
                    
                 if (s.equalsIgnoreCase(s2))
                 {
                     if(DecodeRF.isSrc2Set == false)
                     { 
                         src2 = Integer.parseInt( str.getdata());
                         DecodeRF.isSrc2Set = true;  
                         
                     }              
                 }
                 if (s.equalsIgnoreCase(s3))
                 {
                       if(DecodeRF.isSrc3Set == false)
                     {
                     src3 = Integer.parseInt( str.getdata());
                     DecodeRF.isSrc3Set = true;
                     }
                 }
                }                 
                // check the forwarding bus if it has destination value                        
                if(lstRegValue.get(sr1).getisValid() == true && INTFU == null)
                {
                     if(DecodeRF.isSrc3Set == true && DecodeRF.isSrc2Set == true )
                    {
                         DecodeRF.setsrc2data(src2);
                         DecodeRF.setsrc3data(src3);
                            INTFU = DecodeRF; 
                          lstRegValue.get(sr1).setisValid(false);
                          DecodeRF.setStall("");
                          System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                         
                       isforwardexestalled = false;
                       DecodeRF.isSrc1Set = false;
                       DecodeRF.isSrc2Set = false;
                       DecodeRF.isSrc3Set = false;
                          DecodeRF = null;
                          isDecodeDone = true;
                    }
                       
                    else
                    {
                          DecodeRF = DecodeRF;
                          DecodeRF.setStall("stalled");
                          System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                         isDecodeDone = false;
                         
                   isforwardexestalled = true;
                    }
                    
                }
                else
                {
                DecodeRF = DecodeRF;
                DecodeRF.setStall("stalled");
                   System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
               isDecodeDone = false;
               
                   isforwardexestalled = true;
                }
           
              }
              
               if(DecodeRF != null && ( "MUL".equals(DecodeRF.getopcode())) )
              {
                  DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
                  DecodeRF.setsrc2(temp_split[2]);
                }
               
              if(temp_split.length > 3 &&!temp_split[3].isEmpty()){
                 DecodeRF.setsrc3(temp_split[3]);
                 }
                
                String s1 = DecodeRF.getsrc1();
                String s2 = DecodeRF.getsrc2();
                String s3 = DecodeRF.getsrc3();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                int sr3 = Integer.parseInt(s3.replace("R", "")); 
                boolean f2 = false;
                boolean f3 = false;
                 String ss3 = "";
                         Integer sv3 = 0 ;
                          String ss2 = "";
                         Integer sv2 = 0;
                         Integer r = 0;
                        for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc2()))
                         {
                              if( lstRegValue.get(sr2).getisValid() == true )
                            {
                          ss2 = key;
                          sv2 = value;
                         src2 = sv2;
                         DecodeRF.isSrc2Set = true;
                         }
                         }
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc3()))
                         {
                              if( lstRegValue.get(sr3).getisValid() == true )
                            {
                          ss3 = key;
                          sv3 = value;
                           src3 = sv3;
                         DecodeRF.isSrc3Set = true;
                         }}
                     }
                for(Forward str : lstForwarding)
                {
                 String s = str.getaddress();
                    
                 if (s.equalsIgnoreCase(s2))
                 {
                     if(DecodeRF.isSrc2Set == false)
                     {
                         src2 = Integer.parseInt( str.getdata());
                         DecodeRF.isSrc2Set = true;  
                         
                     }              
                 }
                 if (s.equalsIgnoreCase(s3))
                 {
                       if(DecodeRF.isSrc3Set == false)
                     {
                     src3 = Integer.parseInt( str.getdata());
                     DecodeRF.isSrc3Set = true;
                     }
                 }
                }        
                
                if(lstRegValue.get(sr1).getisValid() == true && MUL1 == null )
                {
                     if(DecodeRF.isSrc3Set == true && DecodeRF.isSrc2Set == true )
                    {
                         DecodeRF.setsrc2data(src2);
                         DecodeRF.setsrc3data(src3);
                         psw.ispswValid = false;
                        MUL1 = DecodeRF; 
                        lstRegValue.get(sr1).setisValid(false);
                        DecodeRF.setStall("");
                        System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                        isBusy = false;
                        
                       DecodeRF.isSrc1Set = false;
                       DecodeRF.isSrc2Set = false;
                       DecodeRF.isSrc3Set = false;
                        DecodeRF = null;
                        isDecodeDone = true;
                        isforwardexestalled = false;
                    }
                    else
                    {
                        DecodeRF = DecodeRF;
                        DecodeRF.setStall("stalled");
                        isBusy = true;
                        System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                        isDecodeDone = false;
                        isforwardexestalled = true;
                    }
                }
                else
                {
                DecodeRF = DecodeRF;
                DecodeRF.setStall("stalled");
                isBusy = true;
                System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                isDecodeDone = false;
                isforwardexestalled = true;
                }
           
              }
               
               if(DecodeRF != null && ( "DIV".equals(DecodeRF.getopcode())) )
              {
                  DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
                  DecodeRF.setsrc2(temp_split[2]);
                }
               
              if(temp_split.length > 3 &&!temp_split[3].isEmpty()){
                 DecodeRF.setsrc3(temp_split[3]);
                 }
                
                 String s1 = DecodeRF.getsrc1();
                String s2 = DecodeRF.getsrc2();
                String s3 = DecodeRF.getsrc3();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                int sr3 = Integer.parseInt(s3.replace("R", "")); 
                boolean f2 = false;
                boolean f3 = false;
                 String ss3 = "";
                         Integer sv3 = 0 ;
                          String ss2 = "";
                         Integer sv2 = 0;
                         Integer r = 0;
                        for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc2()))
                         {
                              if( lstRegValue.get(sr2).getisValid() == true )
                            {
                          ss2 = key;
                          sv2 = value;
                         src2 = sv2;
                         DecodeRF.isSrc2Set = true;
                         }
                         }
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc3()))
                         {
                              if( lstRegValue.get(sr3).getisValid() == true )
                            {
                          ss3 = key;
                          sv3 = value;
                           src3 = sv3;
                         DecodeRF.isSrc3Set = true;
                         }}
                     }
                for(Forward str : lstForwarding)
                {
                 String s = str.getaddress();
                    
                 if (s.equalsIgnoreCase(s2))
                 {
                     if(DecodeRF.isSrc2Set == false)
                     {
                         src2 = Integer.parseInt( str.getdata());
                         DecodeRF.isSrc2Set = true;  
                         
                     }              
                 }
                 if (s.equalsIgnoreCase(s3))
                 {
                       if(DecodeRF.isSrc3Set == false)
                     {
                     src3 = Integer.parseInt( str.getdata());
                     DecodeRF.isSrc3Set = true;
                     }
                 }
                }        
                if(lstRegValue.get(sr1).getisValid() == true && DIV1 == null )
                {
                     
                     if(DecodeRF.isSrc3Set == true && DecodeRF.isSrc2Set == true )
                    {
                         DecodeRF.setsrc2data(src2);
                         DecodeRF.setsrc3data(src3);
                         psw.ispswValid = false;
                        DIV1 = DecodeRF; 
                        lstRegValue.get(sr1).setisValid(false);
                        DecodeRF.setStall("");
                        System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                        isBusy = false;
                        
                       DecodeRF.isSrc1Set = false;
                       DecodeRF.isSrc2Set = false;
                       DecodeRF.isSrc3Set = false;
                        DecodeRF = null;
                        isDecodeDone = true;
                        isforwardexestalled = false;
                    }
                    else
                    {
                         DecodeRF = DecodeRF;
                         DecodeRF.setStall("stalled");
                        isBusy = true;
                        System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                        isDecodeDone = false;
                        isforwardexestalled = true;
                    }
                }
                else
                {
                DecodeRF = DecodeRF;
                DecodeRF.setStall("stalled");
                isBusy = true;
                System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                isDecodeDone = false;
                
                   isforwardexestalled = true;
                }
           
              }
               
          if(DecodeRF != null &&  "BNZ".equals(DecodeRF.getopcode()))
           {   
               
              
                if( psw.ispswValid == true && INTFU == null)
                {
                     DecodeRF.setLiteral((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null);                                      
                    INTFU = DecodeRF; 
                   DecodeRF.setStall("");
                   System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                   DecodeRF = null; 
                   
                   isforwardexestalled = false;
                }
                 else
                {
                 DecodeRF = DecodeRF;
                 DecodeRF.setStall("stalled");
                 System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                 isDecodeDone = false;
                }
              
           }
          
          if(DecodeRF != null && "BZ".equals(DecodeRF.getopcode()))
           { 
               
                if(psw.ispswValid == true && INTFU == null)
                {
                   DecodeRF.setLiteral((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null);                                      
                   INTFU = DecodeRF; 
                   DecodeRF.setStall("");
                   System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                   DecodeRF = null; 
                   isforwardexestalled = false;
                }
                 else 
                {
                DecodeRF = DecodeRF;
                DecodeRF.setStall("stalled");
                System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                isDecodeDone = false;
                isforwardexestalled = true;
                
               }
          }
              
           if (DecodeRF != null && "JAL".equals(DecodeRF.getopcode()))
             {
                  DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
              DecodeRF.setsrc2(temp_split[2]);
                }
                if(temp_split.length > 3 &&!temp_split[3].isEmpty()){
              DecodeRF.setLiteral(temp_split[3]);
                    } 
             
                String s1 = DecodeRF.getsrc1();
                String s2 = DecodeRF.getsrc2();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", "")); 
                boolean f2 = false;
                String ss1 = "";
                         Integer sv1 = 0 ;
                          String ss2 = "";
                         Integer sv2 = 0;
                         Integer r = 0;
                        for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc2() ) )
                         {
                            if( lstRegValue.get(sr2).getisValid() == true )
                            {
                             ss2 = key;
                             sv2 = value;
                             src2 = sv2;
                             DecodeRF.isSrc2Set = true;
                                
                            }
                          
                         }
                     }
                for(Forward str : lstForwarding)
                {
                 String s = str.getaddress();
                 if (s.equalsIgnoreCase(s2))
                 {
                     if(DecodeRF.isSrc2Set == false)
                     {
                         src2 = Integer.parseInt( str.getdata());
                         DecodeRF.isSrc2Set = true;
                     }
                 }
                }
                
                  if(lstRegValue.get(sr1).getisValid() == true && INTFU == null )
                {
                    if( DecodeRF.isSrc2Set == true)
                    {
                        DecodeRF.setsrc2data(src2);
                     INTFU = DecodeRF; 
                     lstRegValue.get(sr1).setisValid(false);
                     DecodeRF.setStall("");
                     System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                    
                       DecodeRF.isSrc1Set = false;
                       DecodeRF.isSrc2Set = false;
                       DecodeRF.isSrc3Set = false;
                     DecodeRF = null;
                     isDecodeDone = true;
                   isforwardexestalled = false;
                    }
                    else
                    {
                     DecodeRF = DecodeRF;
                    DecodeRF.setStall("stalled");
                    System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                    isDecodeDone = false;
                   isforwardexestalled = true;
                    }
                }
                  else
                {
                    DecodeRF = DecodeRF;
                    DecodeRF.setStall("stalled");
                    System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                    isDecodeDone = false;
                    
                   isforwardexestalled = true;
                }
             }
            if(DecodeRF != null && "JUMP".equals(DecodeRF.getopcode()))
           {
               DecodeRF.setsrc1((temp_split.length > 1 &&!temp_split[1].isEmpty()) ? temp_split[1] : null); 
              
               if(temp_split.length > 2 &&!temp_split[2].isEmpty()){
                 DecodeRF.setLiteral(temp_split[2]);
                   }
                     String s1 =  DecodeRF.getsrc1();
                     int s = Integer.parseInt(s1.replace("R", ""));
                     boolean isValid = lstRegValue.get(s).getisValid();
                       String sd1 = "";
                      Integer sv1 = 0;
                       for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equalsIgnoreCase(DecodeRF.getsrc1()))
                         {
                              if( lstRegValue.get(s).getisValid() == true )
                            {
                          sd1 = key;
                          sv1 = value;
                         src1 = sv1;
                         DecodeRF.isSrc1Set = true;
                         }
                         }
                     }
                    boolean f1 = false;
                    for(Forward str : lstForwarding)
                    {
                     String ss = str.getaddress();
                     if (ss.equalsIgnoreCase(s1))
                     {
                     if(DecodeRF.isSrc1Set == false)
                     {
                         src1 = Integer.parseInt( str.getdata());
                         DecodeRF.isSrc1Set = true;
                     }
                 }
                }
                
                if(isValid == true  && INTFU == null)
               {
                  if( DecodeRF.isSrc1Set = true)
                 {
                    DecodeRF.setsrc1data(src1);
                    INTFU = DecodeRF; 
                    lstRegValue.get(s).setisValid(false);
                    DecodeRF.setStall("");
                    System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
                    DecodeRF.isSrc1Set = false;
                    DecodeRF.isSrc2Set = false;
                    DecodeRF.isSrc3Set = false;
                    DecodeRF = null;
                    isDecodeDone = true;
                   isforwardexestalled = false;
                 }
               }
             else
             {
                 DecodeRF = DecodeRF;
                 DecodeRF.setStall("stalled");
                  System.out.println("DRF : "+ DecodeRF.getinstruction_number() + " "+ DecodeRF.getinstruction_string() + " " +  DecodeRF.getstall());     
               isDecodeDone = false;
               
                   isforwardexestalled = true;
             }
               
           }
          
         if(DecodeRF != null && "HALT".equals(DecodeRF.getopcode()))
            {
               isHalt = true;
               DIV1 = DecodeRF; 
              System.out.println("DRF : HALT");
                pc = 100;
            }
     
         }
     
     }
        
     public void INTFU() { 
         
         if(INTFU == null)
         {
                System.out.println("INTFU : Empty");
         }
         else 
         { 
                  int result;
                  //ArithmeticOperation objAO = new ArithmeticOperation();  
                  if(INTFU != null && "MOVC".equals(INTFU.getopcode()))
                  {
                      if(Memory == null)
                      { 
                        result = INTFU.getsrc1data() + Integer.parseInt(INTFU.getLiteral());
                        Forward obj = new Forward();
                        INTFU.setdestinationValue(result);
                         Memory = INTFU;
                        obj.setaddress(INTFU.getsrc1());
                        obj.setdata(Integer.toString(result));
                        lstForwarding.add(obj);
                         INTFU.setStall("");
                        System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null;
                          
                      }
                      else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                           // isforwardexestalled = true;
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                      
                       
                  }
                  if(INTFU != null && "LOAD".equals(INTFU.getopcode()))
                  { 
                       if (Memory == null)
                       {
                           result = INTFU.getsrc2data() + Integer.parseInt(INTFU.getLiteral());
                          INTFU.setdestinationValue(result);
                          Memory = INTFU;
                          INTFU.setStall("");
                          System.out.println("INTFU : " +INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                          INTFU = null;
                       }  
                       else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                     
                  }
                
                  
                    if(INTFU != null && "STORE".equals(INTFU.getopcode()))
                    {
                        if (Memory == null)
                       {
                            result = INTFU.getsrc2data() + Integer.parseInt(INTFU.getLiteral());
                            INTFU.setdestinationValue(result);
                            Memory = INTFU;
                            INTFU.setStall("");
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                            INTFU = null;
                        }
                     else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                      
                     }   
                  
                    if(INTFU != null && "ADD".equals(INTFU.getopcode()))
                  {
                      
                     Forward obj = new Forward();
                         if (Memory == null)
                       {
                         result = INTFU.getsrc2data() + INTFU.getsrc3data();
                        obj.setaddress(INTFU.getsrc1());
                        obj.setdata(Integer.toString(result));
                        INTFU.setStall("");                      
                        INTFU.setdestinationValue(result);
                        Memory = INTFU;
                        isBusy = false; 
                        if (result < 0)
                        {
                            psw.setnegative(true);
                        }
                        if(result == 0)
                        { 
                            psw.setzero(true);                          
                        }
                        if(result > 0)
                        {
                            psw.setnonZero(true);
                        }
                        psw.ispswValid = true;
                       // INTFU.setpswInstruction(psw);
                        //psw.setisPSWForwarding("yes");
                       // lstPswForwardInst.add(INTFU);
                       // obj.setpswForwardValid(psw);
                        lstForwarding.add(obj);
                       System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null;
                        
                       }
                      else
                       {
                           psw.setisPSWForwarding("");
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                      
                  }
                  
                    
                      if(INTFU != null && "SUB".equals(INTFU.getopcode()))
                  {
                      
                     Forward obj = new Forward();
                         if (Memory == null)
                       {
                         result = INTFU.getsrc2data() - INTFU.getsrc3data();
                        obj.setaddress(INTFU.getsrc1());
                        obj.setdata(Integer.toString(result));
                        INTFU.setStall("");                      
                        INTFU.setdestinationValue(result);
                        Memory = INTFU;
                        isBusy = false; 
                        if (result < 0)
                        {
                            psw.setnegative(true);
                        }
                        if(result == 0)
                        { 
                            psw.setzero(true);                          
                        }
                        if(result > 0)
                        {
                            psw.setnonZero(true);
                        }
                        psw.ispswValid = true;
                       // INTFU.setpswInstruction(psw);
                        // psw.setisPSWForwarding("yes");
                        //lstPswForwardInst.add(INTFU);
                       // obj.setpswForwardValid(psw);
                        lstForwarding.add(obj);
                       System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null;
                           
                       }
                       
                      else
                       {
                            INTFU = INTFU;
                            psw.setisPSWForwarding("");
                            INTFU.setStall("Stalled");
                          //  isforwardexestalled = true;
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                      
                  }
                   
                        if(INTFU != null && "AND".equals(INTFU.getopcode()))
                  {
                      
                     Forward obj = new Forward();
                          if (Memory == null)
                       {
                         result = INTFU.getsrc2data() & INTFU.getsrc3data();
                        obj.setaddress(INTFU.getsrc1());
                        obj.setdata(Integer.toString(result));
                        INTFU.setStall("");                      
                        INTFU.setdestinationValue(result);
                        //TempInt = INTFU;;
                      Memory = INTFU;
                        lstForwarding.add(obj);
                       // isforwardexestalled = false;
                       System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null;
                           
                       }
                      else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                           // isforwardexestalled = true;
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                      
                  }
                  
                      
                        if(INTFU != null && "EXOR".equals(INTFU.getopcode()))
                  {
                      
                     Forward obj = new Forward();
                          if (Memory == null)
                       {
                         result = INTFU.getsrc2data() ^ INTFU.getsrc3data();
                        obj.setaddress(INTFU.getsrc1());
                        obj.setdata(Integer.toString(result));
                        INTFU.setStall("");                      
                        INTFU.setdestinationValue(result);
                       // TempInt = INTFU;;
                      Memory = INTFU;
                        lstForwarding.add(obj);
                      //  isforwardexestalled = false;
                       System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null;
                           
                       }
                      else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                          //  isforwardexestalled = true;
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                      
                  }
                  
                             
                        if(INTFU != null && "OR".equals(INTFU.getopcode()))
                  {
                      
                     Forward obj = new Forward();
                        if (Memory == null)
                       {
                         result = INTFU.getsrc2data() | INTFU.getsrc3data();
                        obj.setaddress(INTFU.getsrc1());
                        obj.setdata(Integer.toString(result));
                        lstForwarding.add(obj);
                        INTFU.setStall("");                      
                        INTFU.setdestinationValue(result);
                       // TempInt = INTFU;
                       Memory = INTFU;
                      // isforwardexestalled = false;
                       System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null;
                           
                       }
                      else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                         //   isforwardexestalled = true;
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                      
                  }
                  
                    
                  if (INTFU != null && "JAL".equals(INTFU.getopcode()))
                  {
                        if (Memory == null)
                       {
                        int memadd = INTFU.getsrc2data()  + Integer.parseInt(INTFU.getLiteral());
                        result = (memadd - 4000)/4;
                        Memory = INTFU;
                        if (result > 0)
                        {
                         pc = result;
                         DecodeRF = null;
                         Fetch = null;
                       }
                        doNotFetch = true;
                         INTFU.setStall("");
                        System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                                             
                      INTFU = null; 
                       }
                      else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                  }
                  
                if(INTFU != null && "BNZ".equals(INTFU.getopcode()))
                  {
                      if(Memory == null)
                      {
                      if(psw.nonZero == true )
                      {
                        int memadd = INTFU.getmemaddress();
                        int operateaddress  = memadd + Integer.parseInt(INTFU.getLiteral());
                        result = ( operateaddress - 4000 )/4;
                        Memory = INTFU;
                       if(_inst.size() > result && result >= 0)
                       {
                         pc = result;
                         DecodeRF = null;
                         Fetch = null;
                       }
                         doNotFetch = true;
                         INTFU.setStall("");
                        System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null;
                      }
                      }
                      else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                  }
                if(INTFU != null && "BZ".equals(INTFU.getopcode()))
                  {
                      if(Memory == null)
                      {
                       
                      if(psw.zero == true)
                      {
                        int memadd = INTFU.getmemaddress();
                        int operateaddress  = memadd + Integer.parseInt(INTFU.getLiteral());
                        result = (operateaddress - 4000)/4;
                         //TempInt = INTFU;;
                      Memory = INTFU;                        
                        if(_inst.size() > result && result >= 0)
                         {
                            pc = result;
                            DecodeRF = null;
                            Fetch = null;
                         }
                         INTFU.setStall("");
                          doNotFetch = true;
                        System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                        INTFU = null; 
                      }
                      }
                      else
                       {
                            INTFU = INTFU;
                            INTFU.setStall("Stalled");
                            System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                       }
                  }
                   if(INTFU != null && "JUMP".equals(INTFU.getopcode()))
                  { 
                      if(Memory == null)
                      {
                      int operateaddress  = INTFU.getsrc1data() + Integer.parseInt(INTFU.getLiteral());
                      result = ( operateaddress - 4000 ) /4;
                      Memory = INTFU;
                     if(_inst.size() > result)
                       {
                            pc = result;
                            DecodeRF = null;
                            Fetch = null;
                       }
                     doNotFetch = true;
                    INTFU.setStall("");
                       System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());     
                     INTFU = null;
                  }
                      else
                   {
                    INTFU = INTFU;
                    INTFU.setStall("Stalled");
                     System.out.println("INTFU : " + INTFU.getinstruction_number() + " "+ INTFU.getinstruction_string() + " " +  INTFU.getstall());                      
                    }
                  }
                   
         }
         
     }
     
     public void MUL1()
     {
         if (MUL1 == null)
         {
             System.out.println("MUL1 : Empty");
         }
         else
            {  
                  int result;
                  //ArithmeticOperation objAO = new ArithmeticOperation();   
                     if (MUL2 == null)
                       {
                           result = MUL1.getsrc2data() * MUL1.getsrc3data();
                           MUL1.setdestinationValue(result);
                            MUL2 = MUL1;
                            isBusy = false; 
                            MUL1.setStall("");
                            if (result < 0)
                            {
                               psw.setnegative(true);
                            }
                            if(result == 0)
                            {
                                psw.setzero(true);                          
                            }
                            if(result > 0)
                            {
                                psw.setnonZero(true);
                            }
                            
                            MUL1.setStall(""); 
                            System.out.println("MUL1 : " + MUL1.getinstruction_number() + " "+ MUL1.getinstruction_string() + " " +  MUL1.getstall());     
                            MUL1 = null;
                       }                                
                       else
                       {
                            MUL1 = MUL1;
                            MUL1.setStall("Stalled");
                            System.out.println("MUL1 : " + MUL1.getinstruction_number() + " "+ MUL1.getinstruction_string() + " " +  MUL1.getstall());                      
                       }
                      
                  }
      
     }
     public void MUL2()
     {  
         if (MUL2 == null)
         {
             System.out.println("MUL2 : Empty");
         }
         else
         {  
              if (Memory == null)
            {
            Forward obj = new Forward();
            obj.setaddress(MUL2.getsrc1());
            obj.setdata(Integer.toString(MUL2.getdestinationValue()));
            lstForwarding.add(obj);  
           // TempMUL = MUL2;
            psw.ispswValid = true;
            Memory = MUL2;
            isBusy = false;  
            MUL2.setStall("");  
         //   isforwardexestalled = false;
            System.out.println("MUL2 : "+ MUL2.getinstruction_number() + " "+ MUL2.getinstruction_string() + " " +  MUL2.getstall());     
            MUL2 = null;
            }
              else
              {
                MUL2 = MUL2;
                MUL2.setStall("Stalled");
                //isforwardexestalled = true;
                System.out.println("MUL2 : "+ MUL2.getinstruction_number() + " "+ MUL2.getinstruction_string() + " " +  MUL2.getstall());                        
              }
         }  
         
     }
      public void DIV1()
     {
          if (DIV1 == null)
         {
             System.out.println("DIV1 : Empty");
         }
          else  if(DIV1 != null && "HALT".equals(DIV1.getopcode()))
             {
                   // TempInt = INTFU;
                      DIV2 = DIV1;
                    isHalt = true;
                    System.out.println("DIV1 : HALT");  
              }
         else
            {  
                if (DIV2 == null)
                       { 
                  int result;
                  //ArithmeticOperation objAO = new ArithmeticOperation(); 
                  result = DIV1.getsrc2data() / DIV1.getsrc3data();
                  DIV1.setdestinationValue(result);
                  DIV2 = DIV1;
                  isBusy = false; 
                  if (result < 0)
                  {
                    psw.setnegative(true);
                  }
                  if(result == 0)
                  {
                    psw.setzero(true);                          
                  }
                  if(result > 0)
                  {
                    psw.setnonZero(true);
                  }
                DIV1.setStall("");
                 System.out.println("DIV1 : " + DIV1.getinstruction_number() + " "+ DIV1.getinstruction_string() + " " +  DIV1.getstall());     
                    DIV1 = null;                      
                  }
            else
              {
                DIV1 = DIV1;
                DIV1.setStall("Stalled");
                //isforwardexestalled = true;
                System.out.println("DIV1 : "+ DIV1.getinstruction_number() + " "+ DIV1.getinstruction_string() + " " +  DIV1.getstall());                        
              }
            }
         
     }
       public void DIV2()
     {
         if (DIV2 == null)
         {
             System.out.println("DIV2 : Empty");
         }
         else if(DIV2 != null && "HALT".equals(DIV2.getopcode()))
             {
                   // TempInt = INTFU;
                      DIV3 = DIV2;
                    isHalt = true;
                    System.out.println("DIV2 : HALT");  
              }
         else
         {  
              if (DIV3 == null)
           { 
            int result;
            DIV3 = DIV2;
            isBusy = false;  
            DIV2.setStall("");                       
            System.out.println("DIV2 : "+ DIV2.getinstruction_number() + " "+ DIV2.getinstruction_string() + " " +  DIV2.getstall());     
            DIV2 = null;
           }
              else
              { 
                DIV2 = DIV2;
                DIV2.setStall("Stalled");
                //isforwardexestalled = true;
                System.out.println("DIV2 : "+ DIV1.getinstruction_number() + " "+ DIV2.getinstruction_string() + " " +  DIV2.getstall());                        
             
                  
              }
         } 
          
     }
        public void DIV3()
     {
         if (DIV3 == null)
         {
             System.out.println("DIV3 : Empty");
         }
         else  if(DIV3 != null && "HALT".equals(DIV3.getopcode()))
             {
                   // TempInt = INTFU;
                      DIV4 = DIV3;
                    isHalt = true;
                    System.out.println("DIV3 : HALT");  
              }
         else
         {    
             if (DIV4 == null)
           {  
            int result;
            DIV4 = DIV3;
            isBusy = false;  
            DIV3.setStall("");                       
            System.out.println("DIV3 : "+ DIV3.getinstruction_number() + " "+ DIV3.getinstruction_string() + " " +  DIV3.getstall());     
            DIV3 = null;
         }
             else
             {
                 DIV3 = DIV3;
                DIV3.setStall("Stalled");
                //isforwardexestalled = true;
                System.out.println("DIV3 : "+ DIV1.getinstruction_number() + " "+ DIV3.getinstruction_string() + " " +  DIV3.getstall());                        
             
             }
         }
         
     }
         public void DIV4()
     {
           if (DIV4 == null)
         {
             System.out.println("DIV4 : Empty");
         }
           else  if(DIV4 != null && "HALT".equals(DIV4.getopcode()))
             {
                   // TempInt = INTFU;
                      Memory = DIV4;
                    isHalt = true;
                    System.out.println("DIV4 : HALT");  
              } 
         else
         {    if (Memory == null)
           {    
            int result;
            Forward obj = new Forward();
            obj.setaddress(DIV4.getsrc1());
            obj.setdata(Integer.toString(DIV4.getdestinationValue()));
            psw.ispswValid = true;
            lstForwarding.add(obj);  
           // TempDiv = DIV4;
            Memory = DIV4;
            DIV4.setStall("");
           // isforwardexestalled = false;
            isBusy = false;                         
            System.out.println("DIV4 : "+ DIV4.getinstruction_number() + " "+ DIV4.getinstruction_string() + " " +  DIV4.getstall());     
            DIV4 = null;
           }
         else
         {
              DIV4 = DIV4;
                DIV4.setStall("Stalled");
                //isforwardexestalled = true;
                System.out.println("DIV4 : "+ DIV1.getinstruction_number() + " "+ DIV4.getinstruction_string() + " " +  DIV4.getstall());                        
             
         }
         }
           
     }
    
    
   
     public void Memory() { 
         
        // TempMem();
         
         if(Memory == null)
         {
              System.out.println("MEM : Empty");
         }         
         else
         {
             if(Memory != null )
            {
               if( "DIV".equals(Memory.getopcode()))
            {
                MemCalculation();
            }
            else if ( "MUL".equals(Memory.getopcode()))
            {
                MemCalculation();
            }
            else if (!"DIV".equals(Memory.getopcode()) && !"MUL".equals(Memory.getopcode()))
            {
                MemCalculation();
            }
          }
         }
       
     }
	
     public void MemCalculation()
     {
                //ArithmeticOperation objAO = new ArithmeticOperation();
              if(Memory != null && "STORE".equals(Memory.getopcode()))
              {
                 int destination1 =    Memory.getdestinationValue();
                 ArrayList<Integer> res = STORECalculation(Memory);
                 Memory.settarget_mem_address(res.get(0));
                 Memory.settarget_mem_data(destination1);
                 int r = Memory.gettarget_mem_address();
                 int v1 = Memory.gettarget_mem_data();
                 _mem.put ( v1, r);                
                 Writeback = Memory;
                 System.out.println("MEM : " + Memory.getinstruction_number() + " "+ Memory.getinstruction_string() + " " +  Memory.getstall());     
                 Memory = null;
              }
                if(Memory != null && "HALT".equals(Memory.getopcode()))
             {
                   // TempInt = INTFU;
                      Writeback = Memory;
                    isHalt = true;
                    System.out.println("Memory : HALT");  
              }
                
              else if(Memory != null && "LOAD".equals(Memory.getopcode()))
              {
                int destination1 =    Memory.getdestinationValue();
               ArrayList<Integer> res =  STORECalculation(Memory);
                Memory.setdestinationValue(res.get(0));
                Writeback = Memory;
               System.out.println("MEM : " + Memory.getinstruction_number() + " "+ Memory.getinstruction_string() + " " +  Memory.getstall());     
               Memory = null;                 
              }
              
              else if(Memory != null && "HALT".equals(Memory.getopcode()))
              {
                   Writeback = Memory;
                System.out.println("MEM : HALT");
              }
              
            else  if(Memory != null && ("MOVC".equals(Memory.getopcode()) || "ADD".equals(Memory.getopcode())
                      || "SUB".equals(Memory.getopcode()) || 
                      "MUL".equals(Memory.getopcode()) || "BZ".equals(Memory.getopcode())|| 
                      "BNZ".equals(Memory.getopcode()) || "JUMP".equals(Memory.getopcode())
                      || "EXOR".equals(Memory.getopcode())|| "OR".equals(Memory.getopcode())
                       || "DIV".equals(Memory.getopcode())|| "AND".equals(Memory.getopcode()) || "JAL".equals(Memory.getopcode())))
              {
                Writeback = Memory;
                if (isforwardexestalled = true)
                {
                    for (int counter = 0; counter < lstForwarding.size(); counter++) 
                  {
                      Forward str = lstForwarding.get(counter);
                       String s = str.getaddress();
                 if (s.equalsIgnoreCase(Memory.getsrc1()))
                 {
                    lstForwarding.remove(str);
                 }
                 if (s.equalsIgnoreCase(Memory.getsrc2()))
                 {
                    lstForwarding.remove(str);
                 }
                 if (s.equalsIgnoreCase(Memory.getsrc3()))
                 {
                    lstForwarding.remove(str);
                 }  
                  
                  }
               
                }
                  
                  System.out.println("MEM : " + Memory.getinstruction_number() + " "+ Memory.getinstruction_string() + " " +  Memory.getstall());     
                Memory = null;
              }
           else  if(Memory != null && ("BZ".equals(Memory.getopcode())|| 
                      "BNZ".equals(Memory.getopcode()) ))
              {
                Writeback = Memory;
                                
                  System.out.println("MEM : " + Memory.getinstruction_number() + " "+ Memory.getinstruction_string() + " " +  Memory.getstall());     
                Memory = null;
              }
         
             
         }
     
     public void WriteBack() { 
         if(Writeback == null)
         {
              System.out.println("WB : Empty");
         }
         else
         {
              if (Writeback != null && "MOVC".equals(Writeback.getopcode()))
              {  
                int destination1 =    Writeback.getdestinationValue();
                registerFile.put( Writeback.getsrc1(), destination1);
                 String s1 = Writeback.getsrc1();
                 int sr1 = Integer.parseInt(s1.replace("R", ""));
                 lstRegValue.get(sr1).setisValid(true);
                 System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());                  
                 lstInstrWritten.add(Writeback.getinstruction_string());
                   Writeback = null;
              }
              
               if (Writeback != null && "JAL".equals(Writeback.getopcode()))
              { 
                  registerFile.put(Writeback.getsrc1(), Writeback.getmemaddress() + 4);
                  String s1 = Writeback.getsrc1();
                  int sr1 = Integer.parseInt(s1.replace("R", ""));
                  lstRegValue.get(sr1).setisValid(true);
                  System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());                  
                
              }
              
               if (Writeback != null && "BNZ".equals(Writeback.getopcode()))
              { 
                 System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());                
                  lstInstrWritten.add("BNZ");
                  Writeback = null;
              }
                 if (Writeback != null && "BZ".equals(Writeback.getopcode()))
              { 
                 System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());               
                  lstInstrWritten.add("BZ");
                  Writeback = null;
              }
              if(Writeback != null && "JUMP".equals(Writeback.getopcode()))
              {
                System.out.println( "WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());                
                  lstInstrWritten.add(Writeback.getinstruction_string());
                  Writeback = null;     
              }
               if (Writeback != null && ("ADD".equals(Writeback.getopcode())  || "SUB".equals(Writeback.getopcode()) 
                       ||"MUL".equals(Writeback.getopcode()) || "DIV".equals(Writeback.getopcode()) ))
              {  
                int destination1 =    Writeback.getdestinationValue();
                registerFile.put( Writeback.getsrc1(), destination1);
                String s1 = Writeback.getsrc1();
                String s2 = Writeback.getsrc2();
                String s3 = Writeback.getsrc2();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                int sr3 = Integer.parseInt(s3.replace("R", ""));
                lstRegValue.get(sr1).setisValid(true);
                lstPswInst.add(Writeback);
               System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());   
                lstInstrWritten.add(Writeback.getinstruction_string());
                Writeback = null;
              }
               if (Writeback != null && ("EXOR".equals(Writeback.getopcode()) 
                       ||"OR".equals(Writeback.getopcode()) 
                       ||"AND".equals(Writeback.getopcode())))
              {  
                int destination1 =    Writeback.getdestinationValue();
                registerFile.put( Writeback.getsrc1(), destination1);
                String s1 = Writeback.getsrc1();
                String s2 = Writeback.getsrc2();
                String s3 = Writeback.getsrc2();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                int sr3 = Integer.parseInt(s3.replace("R", ""));
                lstRegValue.get(sr1).setisValid(true);
               System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());   
                lstInstrWritten.add(Writeback.getinstruction_string());
                Writeback = null;
              }
            if (Writeback != null && "LOAD".equals(Writeback.getopcode()))
              {  
                int destination1 =    Writeback.getdestinationValue();
                registerFile.put( Writeback.getsrc1(), destination1);
                 String s1 = Writeback.getsrc1();
                String s2 = Writeback.getsrc2();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", ""));
                lstRegValue.get(sr1).setisValid(true);
              System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());   
                 lstInstrWritten.add(Writeback.getinstruction_string());
                Writeback = null;
              }
            
           if (Writeback != null && "STORE".equals(Writeback.getopcode())  )
              {
                String s1 = Writeback.getsrc1();
                String s2 = Writeback.getsrc2();
                int sr1 = Integer.parseInt(s1.replace("R", ""));
                int sr2 = Integer.parseInt(s2.replace("R", "")); 
                lstRegValue.get(sr1).setisValid(true);
                lstRegValue.get(sr2).setisValid(true);
               System.out.println("WB : " + Writeback.getinstruction_number() + " "+ Writeback.getinstruction_string() + " " +  Writeback.getstall());       
                 lstInstrWritten.add(Writeback.getinstruction_string());
                 Writeback = null;
              }
           
           if (Writeback != null && "HALT".equals(Writeback.getopcode())  )
              {
                  isHaltinWBStage = true;
                   System.out.println("WB : HALT");   
                  lstInstrWritten.add(Writeback.getinstruction_string());
                   Writeback = null;
              }
         
               }
         if(_inst.size() == lstInstrWritten.size())
         {
             if( lstInstrWritten.contains("BZ") || lstInstrWritten.contains("BNZ"))
             {
                 allInstWritten = false;
             }
             else
             { 
               allInstWritten = true;
             }
         }
        }              
	
     public ArrayList<Integer> STORECalculation( Instruction instList) 
        {
         ArrayList<Integer> result = new ArrayList<Integer>();
               
                switch(instList.getopcode())
                {
                    case "STORE" : 
                    {    
                         String s1 = "";
                         Integer v1 = 0 ;
                          int r = 0;
                          for (Map.Entry<String, Integer> e : registerFile.entrySet()) {
                         String key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).contains(instList.getsrc1()))
                         {
                          s1 = key;
                          v1 = value;
                         }                        
                     } 
                          r = v1;
                          result.add(r);
                      }
                        break;

                        
                   case "LOAD" : 
                    if(instList.getsrc1() != null && instList.getdestinationValue() != -1)
                    {
                         int s1 = 0;
                         Integer v1 = 0 ;
                         Integer r = 0;
                        int res = instList.getdestinationValue();
                       for (Map.Entry<Integer, Integer> e : _mem.entrySet()) {
                         Integer key = e.getKey();
                         Integer value = e.getValue();
                         if ((key).equals(res))
                         {
                          s1 = key;
                          v1 = value;
                         }                        
                     }
                       r = v1;
                       result.add(r);
                    }
                    break;
                }
                return result;
                      
        } 
     
     	public void displayMemory() {
		System.out.println("========== Register File ==============");
		Iterator reg = registerFile.entrySet().iterator();
		while (reg.hasNext()) {
			Map.Entry pair = (Map.Entry) reg.next();
			if(!(pair.getValue().toString().equals("-99999"))){
				System.out.println(pair.getKey() + " : " + pair.getValue());
				reg.remove();
			}
		}
		System.out.println("\n");
		System.out.println("============ Memory content ===============");
		Iterator mem = _mem.entrySet().iterator();
		while (mem.hasNext()) {
			Map.Entry pair = (Map.Entry) mem.next();
			if(!(pair.getValue().toString().equals("-99999"))){
				System.out.println(pair.getKey() + " : " + pair.getValue());
				mem.remove();
			}
		}

	}
        
       public void  initializeMemoryAndRegisters()
       {
           registerFile.clear();
           Fetch = null;
           DecodeRF = null;
           INTFU = null;
           MUL1 = null;
           MUL2 = null;
           Memory = null;
           Writeback = null;
           _mem.clear();
       }
     
}

class Register{
   private boolean isValid;
  private  String RegisterName;
  private boolean  isPswSetinWB;
 
    public boolean getisPswSetinWB() {
	return isPswSetinWB;
	}

  public void setisPswSetinWB(boolean isPswSetinWB) {
	this.isPswSetinWB = isPswSetinWB;
	}
        
     public boolean getisValid() {
	return isValid;
	}

  public void setisValid(boolean value) {
	this.isValid = value;
	}
  
  public String getRegisterName() {
	return RegisterName;
	}

  public void setRegisterName(String RegisterName) {
	this.RegisterName = RegisterName;
	}          
    
    Register(String RegisterName, boolean isValid)
    {
        this.RegisterName = RegisterName;
        this.isValid = isValid;
    }
}

class PSWFlag{
    boolean zero;
    boolean nonZero;
    boolean carry;
    boolean negative;
    boolean ispswValid;
    String isPSWForwarding;
    
    public boolean getispswValid() {
	return ispswValid;
	}

  public void seispswValid(boolean ispswValid) {
	this.ispswValid = ispswValid;
	}
    
     public boolean getzero() {
	return zero;
	}

  public void setzero(boolean value) {
	this.zero = value;
	}
  
  public boolean getnonZero() {
	return nonZero;
	}

  public void setnonZero(boolean nonZero) {
	this.nonZero = nonZero;
	}
  
   public boolean getnegative() {
	return zero;
	}

  public void setnegative(boolean value) {
	this.zero = value;
	}
  
  public String getisPSWForwarding() {
	return isPSWForwarding;
	}

  public void setisPSWForwarding(String isPSWForwarding) {
	this.isPSWForwarding = isPSWForwarding;
	}
}


