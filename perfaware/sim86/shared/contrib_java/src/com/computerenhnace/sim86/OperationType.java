package com.computerenhance.sim86;

public enum OperationType
{
	NONE,

	MOV,

	PUSH,

	POP,

	XCHG,

	IN,

	OUT,

	XLAT, LEA, LDS, LES, LAHF, SAHF, PUSHF, POPF,

	ADD,

	ADC,

	INC,

	AAA, DAA,

	SUB,

	SBB,

	DEC,

	NEG,

	CMP,

	AAS, DAS, MUL, IMUL, AAM, DIV, IDIV, AAD, CBW, CWD,

	NOT, SHL, SHR, SAR, ROL, ROR, RCL, RCR,

	AND,

	TEST,

	OR,

	XOR,

	REP, MOVS, CMPS, SCAS, LODS, STOS,

	CALL,

	JMP,

	RET,

	RETF,

	JE, JL, JLE, JB, JBE, JP, JO, JS, JNE, JNL, JG, JNB, JA, JNP, JNO, JNS, LOOP, LOOPZ, LOOPNZ, JCXZ,

	INT, INT3,

	INTO, IRET,

	CLC, CMC, STC, CLD, STD, CLI, STI, HLT, WAIT, ESC, LOCK, SEGMENT;

	public static final OperationType[] VALUES = values();
};
