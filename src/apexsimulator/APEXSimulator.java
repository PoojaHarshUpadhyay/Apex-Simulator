    /*
    * To change this license header, choose License Headers in Project Properties.
    * To change this template file, choose Tools | Templates
    * and open the template in the editor.
    */
    package apexsimulator;

    import java.io.BufferedReader;
    import java.io.FileNotFoundException;
    import java.io.FileReader;
    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;
    import java.util.Scanner;
    import java.util.logging.Level;
    import java.util.logging.Logger;

    /**
    *The input file is read and instructions are stored
    * in list
    * @author pooja
    */
    public class APEXSimulator {

    public static ArrayList<Instruction> _inst = new ArrayList<Instruction>();
    public static int pc_value = 0;  
    public static int busy;

    /**
    * @param args the command line arguments
    */
    public static void main(String[] args) {
     // The name of the file to open.
    int noOfCyclesToSimulate = 0;
    Stimulate obja = new Stimulate();
    Scanner scanner = new Scanner(System.in);
      try {
        //  String fileName = "/home/pooja/Desktop/cs 520/pupadhy2_cs520_pp1/APEXSimulator/src/apexsimulator/inputdata.txt";
        String fileName = args[0];
          String line = null;
           FileReader fileReader = new FileReader(fileName);

                BufferedReader bufferedReader = new BufferedReader(fileReader);
                int address = 4000;
               
                String opcode = "";
                String reg1 = "";
                String reg2 = "";
                String reg3 = "";
                int instructionnum = 0;
                 Stimulate objs = new Stimulate();
                 
        try {
            while((line = bufferedReader.readLine()) != null) {
               String instruct = "(I" + instructionnum + ")";
                Instruction obj = new Instruction(address, line, instruct);
                _inst.add(obj);
                address = address + 4;
                instructionnum = instructionnum + 1;
            }
            displayOptions();
            try {
                 while(true)
            {
                
                int option = scanner.nextInt();
                
                switch (option) {
                    case 1:
                        obja.initializeMemoryAndRegisters();
                        System.out.println("\tInitializes the PC of Fetch state to 4000 =======");
                        break;
                        
                    case 2:
                        System.out.print("\tEnter Number of cycles to simulate:: ");
                        noOfCyclesToSimulate = scanner.nextInt();
                        obja.Stimulate(noOfCyclesToSimulate);
                        break;
                        
                    case 3:
                         obja.displayMemory();
                        break;
                        
                    case 4:
			System.out.println("Exit");
			scanner.close();
			System.exit(0);
                        
                    default:
                        System.out.println("Please enter the correct option.");
                        break;
                }
            }
        } 
        catch(Exception e)
        {
            System.out.println("\n");
        }
           
        } 
        catch (IOException ex) {
            Logger.getLogger(APEXSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         } 
    catch (FileNotFoundException ex) {           
            Logger.getLogger(APEXSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }  
    catch (Exception e) {
                System.out.println("\n");
                //do nothing
        }

    }  

    private static void displayOptions() {
        try {
                System.out.println(" Simulator \n");
                System.out.println("\t\t1) Initialize ");
                System.out.println("\t\t2) simulate: ");
                System.out.println("\t\t3) Display Memory Locations");
                System.out.println("\t\t4) Exit");

        } catch (Exception e) {
        }
    }
    }

    class Forward
    {
        private String data;
        private String address;
        private boolean isForwardZero;
       private PSWFlag pswForwardValid;
        
        
        public void setdata(String data) {
            this.data = data;
         }
        public String getdata() {
            return data;
         }
        
         public void setaddress(String address) {
            this.address = address;
         }
        public String getaddress() {
            return address;
         }
        
         public void setisForwardZero(boolean isForwardZero) {
            this.isForwardZero = isForwardZero;
         }
        public boolean getisForwardZero() {
            return isForwardZero;
         }
         public void setpswForwardValid(PSWFlag pswForwardValid) {
            this.pswForwardValid = pswForwardValid;
      }
         public PSWFlag getpswForwardValid() {
            return pswForwardValid;
    }
     
    }

    
    class Latch
    {
        private String data;
        private String address;
        
        public void setdata(String data) {
            this.data = data;
         }
        public String getdata() {
            return data;
         }
        
         public void setaddress(String address) {
            this.address = address;
         }
        public String getaddress() {
            return address;
         }

    }

    
    class Instruction
    {
    private int memaddress;
    private String instruction_string;
    private int clockcycle;
    private String opcode;
    private String destination;
    private String src1;
    private String src2;
    private String src3;
    private String literal;
    private String stall;
    private int destination_value;
    private String destination_register;
    private int target_mem_address;
    private int target_mem_data; 
    private String instruction_number;
    private int pc_value;
    private int src1data;
    private int src2data;
    private int src3data;
     
   public boolean isSrc2Set;  
   public boolean isSrc3Set ;
   public boolean isSrc1Set;
        
    
       private PSWFlag pswInstruction;
        
   public void setpswInstruction(PSWFlag pswInstruction) {
            this.pswInstruction = pswInstruction;
      }
         public PSWFlag getpswInstruction() {
            return pswInstruction;
    }
    public void setsrc1data(int src1data) {
            this.src1data = src1data;
    }

    public int getsrc1data() {
            return src1data;
    }
    
    public void setsrc2data(int src2data) {
            this.src2data = src2data;
    }

    public int getsrc2data() {
            return src2data;
    }
    
    public void setsrc3data(int src3data) {
            this.src3data = src3data;
    }

    public int getsrc3data() {
            return src3data;
    }
    
    
     public String getinstruction_number() {
            return instruction_number;
    }

    public void setinstruction_number(String instruction_number) {
            this.instruction_number = instruction_number;
    }
    
    public int getmemaddress() {
            return memaddress;
    }

    public void setmemaddress(int memaddress) {
            this.memaddress = memaddress;
    }
    
    public int getpc_value() {
          return pc_value;
   }

   public void setpc_value(int pc_value) {
           this.pc_value = pc_value;
   }
    
public String getDestinationREgister() {
            return destination_register;
    }

    public void setDestinationRegister(String destination_register) {
            this.destination_register = destination_register;
    }

    public String getDestination() {
            return destination;
    }

    public void setDestination(String destination_register) {
            this.destination = destination_register;
    }
    
    public int getclockcycle() {
            return clockcycle;
    }

    public void setclockcycle(int clockcycle) {
            this.clockcycle = clockcycle;
    }

    public String getinstruction_string() {
            return instruction_string;
    }

    public void setinstruction_string(String instruction_string) {
            this.instruction_string = instruction_string;
    }

    public String getopcode() {
            return opcode;
    }

    public void setopcode(String opcode) {
            this.opcode = opcode;
    }
      public String getsrc1() {
            return src1;
    }

    public void setsrc1(String src1) {
            this.src1 = src1;
    }
      public String getsrc2() {
            return src2;
    }

    public void setsrc2(String src2) {
            this.src2 = src2;
    }


    public String getsrc3() {
            return src3;
    }

    public void setsrc3(String src3) {
            this.src3 = src3;
    }


    public String getLiteral() {
            return literal;
    }

    public void setLiteral(String literal) {
            this.literal = literal;
    }
  
       
  public String getstall() {
            return stall;
    }

    public void setStall(String stall) {
            this.stall = stall;
    }
    
   public int getdestinationValue() {
            return destination_value;
    }
   
    public void setdestinationValue(int destination_value) {
            this.destination_value = destination_value;
    }
    public void getdestinationValue(int destination_value) {
            this.destination_value = destination_value;
    }
    public int gettarget_mem_address() {
            return target_mem_address;
    }

    public void settarget_mem_address(int target_mem_address) {
            this.target_mem_address = target_mem_address;
    }

    public int gettarget_mem_data() {
            return target_mem_data;
    }

    public void settarget_mem_data(int target_mem_data) {
            this.target_mem_data = target_mem_data;
    }

    Instruction(int add, String inst, String instructnum)
    {
    memaddress = add;
    instruction_string = inst;
    instruction_number = instructnum;
    }   
    }
