# Apex Simulator

This project helps to implement a cycle-by-cycle simulator for an out-of-order APEX pipeline. 
It also has a forwarding mechanism for handling dependency. 

When we run the project, we get the below options -

Simulator

	1) Initialize   //Initializes all the Registers
	2) simulate:    // Number of cycles you want to simulate
	3) Display Memory Locations    // The Memory address values
	4) Exit        // Exit the program
 
 
PS: I have added the test input file with isnstructions.

Instruction Set Covered-

1) Register-to-register instructions: ADD, SUB, MOVC, MUL, AND, OR, EX-OR
ADD → ADD <dest> <src1> <src2>
SUB → SUB <dest> <src1> <src2>
MUL → MUL <dest> <src1> <src2>
AND → AND <dest> <src1> <src2>
MOVC → MOVC <dest> <src1> <src2>
OR → OR <dest> <src1> <src2>
EX-OR → EX-OR <dest> <src1> <src2>

2) BZ, BNZ, JUMP, BAL, HALT
BZ   → BZ #literal (checks the z(zero) flag and Branch when zero)
BNZ  → BNZ #literal(Branch when non zero )
JUMP → JUMP <src> @#literal (This will jump to the new instruction)
JAL 
HALT → Stops execution

3) LOAD, STORE
LOAD → LOAD <dest> <src1> <literal>
STORE → STORE <src1> <src2> <literal>
