package com.computerenhance.sim86;

public enum InstructionBitsUsage
{
	End,

  Literal,

  D,
  S,
  W,
  V,
  Z,
  MOD,
  REG,
  RM,
  SR,
  Disp,
  Data,

	DispAlwaysW,
  WMakesDataW,
  RMRegAlwaysW,
  RelJMPDisp,
  Far;

	public static final InstructionBitsUsage[] VALUES = values();
}
