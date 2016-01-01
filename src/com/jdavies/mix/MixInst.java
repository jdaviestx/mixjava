package com.jdavies.mix;

/**
 * A single MIX words can be compressed into 31 bits (just under a 32-bit
 * integer): six bits per word = 30 bits, plus a single bit for the sign.
 */
class MixInst	{
	private int c;
	private int i;
	private int f;
	private int a;

	/**
	 * Construct a MIX instruction from pieces (e.g. from the assembler).
	 */
	public MixInst(MixOpCode c, int i, int L, int R, int a)	{
		this.c = c.ordinal();
		this.i = i;
		this.f = (8 * L) + R;
		this.a = a;
	}

	/**
	 * Decompress an integer into c, i, f, and (signed) a.
	 * 
	 * 7: 0111
	 * 8: 1000
	 * 9: 1001
	 * A: 1010
	 * B: 1011
	 * C: 1100
	 * D: 1101
	 * E: 1110
	 * F: 1111
	 * FEDBCA9876543210FEDCBA9876543210
	 *  3         2         1       
	 * 10987654321098765432109876543210
	 * U+555555444444333333222222111111

	 * 76543210 76543210 76543210 76543210
	 * 00000000 00000000 00000000 00111111
	 * 00000000 00000000 00001111 11000000
	 * 00000000 00000011 11110000 00000000
	 * 01111111 11111100 00000000 00000000
	 * 
	 */
	public MixInst(int in) throws IllegalInstruction, FieldError	{
		c = in & 0x0000003F;
		f = (in & 0x00000FC0) >> 6;
		i = (in & 0x0003F000) >> 12;
		a = (in & 0x7FFC0000) >> 18;

		if (c > 64)	{
			throw new IllegalInstruction("Instruction " + c +
				" out of range");
		}
		
		if (i > 6)	{
			throw new IllegalInstruction("Index modifier " + i +
				" out of range");
		}
	}

	/**
	 * Compress an instruction back into its memory representation
	 * (undo what the "from int" constructor does).
	 */
	public int pack()	{
		// TODO deal with sign bit
		return (a << 18) |
			(i << 12) |
			(f << 6) |
			c;
	}

	/**
	 * Execute a single MIX instruction and update the
	 * status of the VM.  Return false if the instruction
	 * is HLT, true otherwise.
	 * 0: NOP
	 * 1: ADD
	 * 2: SUB
	 * 3: MUL
	 * 4: DIV
	 * 5: HLT
	 * 6: SLA
	 * 7: MOVE
	 * 8: LDA
	 * 9: LD1
	 * 10: LD2
	 * 11: LD3
	 * 12: LD4
	 * 13: LD5
	 * 14: LD6
	 * 15: LDX
	 * 16: LDAN
	 * 17: LD1N
	 * 18: LD2N
	 * 19: LD3N
	 * 20: LD4N
	 * 21: LD5N
	 * 22: LD6N
	 * 23: LDXN
	 * 24: STA
	 * 26: ST1
	 * 27: ST2
	 * 28: ST3
	 * 29: ST4
	 * 30: ST5
	 * 31: ST6
	 * 32: STJ
	 * 33: STZ
	 * 34: JBUS
	 * 35: IOC
	 * 36: IN
	 * 37: OUT
	 * 38: JRED
	 * 39: JMP
	 * 40: JAP
	 * 41: J1P
	 * 42: J2P
	 * 43: J3P
	 * 44: J4P
	 * 45: J5P
	 * 46: J6P
	 * 47: JXP
	 * 48: INCA
	 * 49: INC1
	 * 50: INC2
	 * 51: INC3
	 * 52: INC4
	 * 53: INC5
	 * 54: INC6
	 * 55: INCX
	 * 56: CMPA
	 * 57: CMP1
	 * 58: CMP2
	 * 59: CMP3
	 * 60: CMP4
	 * 61: CMP5
	 * 62: CMP6
	 * 63: CMPX

	 */
	public boolean execute(MixVM vm) throws MemoryLocationError,
																				  IllegalInstruction,
																					NotImplemented,
																				  FieldError	{
		int L = f / 8;
		int R = f % 8;

		if (R > 5 && c != 39)	{	// JMP allows this
			throw new FieldError("F-specification " + f + " yields invalid " +
				"R-value of " + R);
		}

		if (L > R)	{
			throw new FieldError("F-specification " + f + " yields invalid " +
				"L-value of: " + L + " (> " + R + ")");
		}

		// 1.3.1, p. 127: This indexing takes place on _every_ instruction.
		MixOpCode op = MixOpCode.values()[c];
		switch (op)	{
			case NOP:
				break;
			case ADD:
				vm.add(a, i, L, R, false);
				break;
			case SUB:
				vm.add(a, i, L, R, true);
				break;
			case MUL:
			case DIV:
				throw new NotImplemented(c);
			case HLT:
				switch (f)	{
					case 0:	// NUM
						vm.convertToNum();
						break;
					case 1:	// CHAR
						vm.convertToChar();
						break;
					case 2:	// HLT
						return false;
					default:
						throw new FieldError("Invalid F-specification " + f +
							" for instruction code " + c + "; expected 0-2");
				}
			case SLA:
				// SLA and SRA do not affect rX; the other shifts affect both A & X as though
				// they were a single 10-byte register.  With SLA, SRA, SLAX and SRAX, zeros
				// are shifted into the register on one side, and bytes disappear at the other
				// side.  The instructions SLC & SRC call for a "circulating" shift, in which
				// the bytes that leave one end enter in at the other end.
				switch (f)	{
					case 0: // SLA
					case 1: // SRA
					case 2: // SLAX
					case 3: // SRAX
					case 4: // SLC
					case 5: // SRC
						throw new NotImplemented(c);
					default:
						throw new FieldError("Invalid F-specification " + f +
							" for instruction code " + c + " (shift); expected 0-5");
				}
			case MOVE:
				vm.moveWords(a, i, f);
				break;
			case LDA:
			case LD1:
			case LD2:
			case LD3:
			case LD4:
			case LD5:
			case LD6:
			case LDX:
				vm.loadRegister(a, c - MixOpCode.LDA.ordinal(), i, L, R, false);
				break;
			case LDAN:
			case LD1N:
			case LD2N:
			case LD3N:
			case LD4N:
			case LD5N:
			case LD6N:
			case LDXN:
				vm.loadRegister(a, c - MixOpCode.LDAN.ordinal(), i, L, R, true);
				break;
			case STA:
			case ST1:
			case ST2:
			case ST3:
			case ST4:
			case ST5:
			case ST6:
			case STX:
				vm.storeRegister(a, c - MixOpCode.STA.ordinal(), i, L, R);
				break;
			case STJ:
				throw new NotImplemented(c);
			case STZ: 
			case JBUS: 
			case IOC: 
				// Tape devices:
				// If M = 0, the tape is rewound.  If M < 0, the tape is skipped backward
				// -M blocks, or to the beginning of the tape.  If M > 0, the tape is skipped
				// forward.  It is improper to skip forward over any blocks following the last
				// one on the that tape.  1 means 100 words.
				// Disk: M should be 0.  Position the device according to rX so that the next IN
				// or OUT operation will take less time.
				// Line printer: M should be 0.  Skip the printer to the top of the page
				// Paper tape: M shoudl be 0.  Rewind the tape.
			case IN: 
			case OUT: 
			case JRED: 
				throw new NotImplemented(c);
			case JMP: 
				vm.conditionalJump(f, a);
				break;
				/*
				// p. 134: When a jump takes place, the J-register is set to the
				// address of the next instruction (the address of the instruction that
				// would have been next if we hadn't jumped).
				switch (f)	{
					case 0: // JMP: unconditional
					case 1: // JSJ: jump, save J; doen't update rJ
					case 2: // JOV: if overflow toggle is on, it is turned off and a jump occurs; otherwise nothing happens
					case 3: // JNOV: if the overflow toggle is off, a JMP occurs; otherwise it is turned off
					case 4:	// JL
					case 5: // JE
					case 6: // JG
					case 7: // JGE
					case 8: // JNE
					case 9: // JLE: Jump if the comparison indicator is set.  The comparison indicator is not changed by these instructions.
						throw new NotImplemented(c);
					default:
						throw new FieldError("Invalid F-specification " + f +
							" for instruction code " + c + " (jump); expected 0-9");
				}
				*/

			case JAP: 
			case J1P:
			case J2P: 
			case J3P: 
			case J4P: 
			case J5P: 
			case J6P: 
			case JXP: 
				switch (f)	{
					case 0: // JrP	register r negative
					case 1: // JrZ  register r zero
					case 2: // JrP	register r positive  (greater than 0, not zero)
					case 3: // JrNN register r nonnegative
					case 4: // JrNZ register r nonzero
					case 5: // JrNP register r nonpositive
					 throw new NotImplemented(c);
					default:
						throw new FieldError("Invalid F-specification " + f +
							" for instruction code " + c + " (jump); expected 0-5");
				}
			case INCA: 
			case INC1: 
			case INC2: 
			case INC3: 
			case INC4: 
			case INC5: 
			case INC6:
			case INCX: 
				switch (f)	{
					case 0:
						// INCn
						// XXX can INC & DEC be indexed?
						vm.incRegister(a, c - MixOpCode.INCA.ordinal());
						break;
					case 1:
						// DECn
						vm.incRegister(a * -1, c - MixOpCode.INCA.ordinal());
						break;
					case 2:
						// ENTA instructions
						vm.setRegister(a, c - MixOpCode.INCA.ordinal(), i, false);
						break;
					case 3:
						vm.setRegister(a * -1, c - MixOpCode.INCA.ordinal(), i, true);
						break;
					default:
						throw new FieldError("Invalid F-specification " + f +
							" for instruction code " + c + "; expected 0-3");
				}
				break;
			case CMPA: 
			case CMP1: 
			case CMP2: 
			case CMP3: 
			case CMP4: 
			case CMP5: 
			case CMP6: 
			case CMPX:
				vm.compare(a, c - MixOpCode.CMPA.ordinal(), i, L, R);
				break;
			default:
				// Since MIX is self-modifying, this can happen at run time
				throw new IllegalInstruction("Illegal instruction code " + c);
		}

		return true;
	}

	public String toString()	{
		return a + ", " + i + ", " + f + ", " + c;
	}
}
