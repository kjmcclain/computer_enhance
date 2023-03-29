package com.computerenhance.sim86;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Decoder
{
	private final ByteBuffer out;
	private final byte[] outBuffer;
	private final ByteBuffer in;
	private ByteChannel channel;
	private boolean eof;

	static
	{
		var libFile = System.getProperty("sim86java");
		if (libFile == null)
		{
			System.loadLibrary("sim86java");
		}
		else
		{
			System.load(Path.of(libFile).toAbsolutePath().toString());
		}
		var version = version();
		if (version != 3)
		{
			System.err.println("Saw sim86_shared library version " + version + " but expected version 3");
		}
	}

	public Decoder()
	{
		in = ByteBuffer.allocate(1024);
		in.flip();
		out = ByteBuffer.allocate(1024);
		out.order(ByteOrder.nativeOrder());
		outBuffer = out.array();
	}

	public void setByteChannel(ByteChannel channel)
	{
		this.channel = channel;
		eof = false;
		in.clear();
		in.flip();
	}

	public Instruction next() throws IOException
	{
		if (!eof && in.remaining() < 16)
		{
			in.compact();
			eof = -1 == channel.read(in);
			in.flip();
		}
		if (!in.hasRemaining()) return null;
		decode();
		out.clear();

		// Let's just make a big assumption that we know exactly how all structs are laid out.
		// Pretty sure there aren't so strong a guarantee by the C standard...
		// Alternately, we could have the C code give us a big list of offsets to all these fields.

		var instr = new Instruction();
		instr.address = out.getInt();
		instr.size = out.getInt();
		instr.op = OperationType.VALUES[out.getInt()];
		instr.flags = out.getInt();
		instr.operands = new InstructionOperand[]{
			readInstructionOperand(), readInstructionOperand()
		};
		instr.segmentOverride = out.getInt();

		return instr;
	}

	private InstructionOperand readInstructionOperand()
	{
		var o = new InstructionOperand();
		o.type = OperandType.VALUES[out.getInt()];
		var p = out.position();
		switch (o.type)
		{
			case NONE -> o.immediate = null;
			case IMMEDIATE -> o.immediate = readImmediate();
			case MEMORY -> o.address = readEffectiveAddressExpression();
			case REGISTER -> o.register = readRegisterAccess();
		}
		out.position(p + 44);
		return o;
	}

	private Immediate readImmediate()
	{
		var immediate = new Immediate();
		immediate.value = out.getInt();
		immediate.flags = out.getInt();
		return immediate;
	}

	private EffectiveAddressExpression readEffectiveAddressExpression()
	{
		var address = new EffectiveAddressExpression();
		address.terms = new EffectiveAddressTerm[]{
			readEffectiveAddressTerm(), readEffectiveAddressTerm()
		};
		address.explicitSegment = out.getInt();
		address.displacement = out.getInt();
		address.flags = out.getInt();
		return address;
	}

	private RegisterAccess readRegisterAccess()
	{
		var ra = new RegisterAccess();
		ra.index = out.get();
		ra.offset = out.get();
		ra.count = out.get();
		return ra;
	}

	private EffectiveAddressTerm readEffectiveAddressTerm()
	{
		var eat = new EffectiveAddressTerm();
		eat.register = readRegisterAccess();
		eat.scale = out.getInt();
		return eat;
	}




	public String getMnemonic(int operationType)
	{
		var l = mnemonic(operationType);
		return new String(outBuffer, 0, l, StandardCharsets.US_ASCII);
	}

	public String getRegisterName(int index, int offset, int count)
	{
		var l = registerName(index, offset, count);
		return new String(outBuffer, 0, l, StandardCharsets.US_ASCII);
	}

	private static native int version();

	private native void decode();

	private native int registerName(int index, int offset, int count);

	private native int mnemonic(int operationType);
}
