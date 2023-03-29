package com.computerenhance.sim86;

import java.nio.file.Files;
import java.nio.file.Path;

public class DecoderTest
{
	public static void main(String[] args) throws Exception
	{
		try (
			var channel = Files.newByteChannel(Path.of(DecoderTest.class
				.getResource("example_disassembly.bin")
				.toURI()))
		)
		{
			var decoder = new Decoder();
			System.out.println(decoder.getMnemonic(1));
			System.out.println(decoder.getRegisterName(1, 1, 1));
			decoder.setByteChannel(channel);
			Instruction instr;
			int numInstrs = 0;
			while (null != (instr = decoder.next()))
			{
				System.out.println(instr.op);
				numInstrs++;
			}
			System.out.println("Printed " + numInstrs + " instructions");
		}
	}
}
