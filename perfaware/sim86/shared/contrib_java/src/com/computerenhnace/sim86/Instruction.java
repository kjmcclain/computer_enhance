package com.computerenhance.sim86;

public class Instruction
{
	public static final int FLAG_LOCK = 1;
	public static final int FLAG_REP = 2;
	public static final int FLAG_SEGMENT = 4;
	public static final int FLAG_WIDE = 8;
	public static final int FLAG_FAR = 10;

	public int address;
	public int size;
	public OperationType op;
	public int flags;
	public InstructionOperand[] operands;
	public int segmentOverride;
}
