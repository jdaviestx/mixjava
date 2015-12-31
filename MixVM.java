package com.jdavies.mix;

 // character codes:
 // (space)ABCDEFGHI(delta)JKLMNOPQR(sigma)(pi)STUVWXYZ0123456789.,()+-*/=$<>@;:'

/**
 * <table>
 * <tr><th>Unit Number</th><th>Peripheral Device</th><th>Block Size</th></tr>
 * <tr><td>t</td><td>Tape unit number 0 &lt;= t &lt;= 7</td><th>100 words</th></tr>
 * <tr><td>d</td><td>Disk or drum unit number 8 &lt;= d &lt;= 15</td><th>100 words</th></tr>
 * <tr><td>16</td><td>Card reader</td><th>16 words</th></tr>
 * <tr><td>17</td><td>Card punch</td><th>16 words</th></tr>
 * <tr><td>18</td><td>Line printer</td><th>24 words</th></tr>
 * <tr><td>19</td><td>Typewriter terminal</td><th>14 words</th></tr>
 * <tr><td>20</td><td>Paper tape</td><th>14 words</th></tr>
 * </table>
 */
public class MixVM	{
	private static final int SIGN_POS = (0x01 << 31);
	private int clock;
	private int pc;
	private int reg[] = new int[8];	 // rA is r[0], r1-6 is r[1-6], rX is r[8]
	// 1.3.1, p. 125: The J-register always holds the address of the instruction following 
	// the most recent "jump" operation.
	private int rJ;
	private boolean ovtog;
	private int compi;

	private int mem[] = new int[4000];

	public MixVM()	{
		// TODO load a program from a file
	}

	public MixVM(int[] mem)	{
		System.arraycopy(mem, 0, this.mem, 0, mem.length);
	}

	/**
	 * For bootstrapping a program only.
	 */
	public void loadMemory(int start, int bytes[])	{
		System.arraycopy(bytes, 0, mem, start, bytes.length);
	}

	/** 
	 * Set the contents of register r to be exactly value (no memory
	 * reference).  i.e. ENTn
	 * p. 133: The action is equivalent to LDA from a memory word containing
	 * the signed value of M.
	 * ENTA sets rA to zeros, the a + sig.  ENTA 0,1  sets rA to the current
	 * contents of register 1, except that -0 is changed to +0.  ENTA -0, 1 is
	 * similar, except that +0 is changed to -0
	 */
	public void setRegister(int value, int r, int i, boolean negate)	{
		reg[r] = value + reg[i];
		
		// Negater AFTER indexing: p. 133: "ENN3 0,3 replaces rI3 by its negative"
		if (negate)	{
			reg[r] &= SIGN_POS;
		}
	}

	public void incRegister(int amount, int r)	{
		reg[r] += amount;
	}

	/**
	 * p. 133: rA/rX: Overflow is possible, and it is treated just as in ADD
	 * ri: overflow must not occur; if M + rIi doesn't fit into two bytes, the
	 * result of this instruction is undefined.
	 */
	public void incrementRegister(int value, int r, int i)	{
	}

	/**
	 * Extract a partial field specification from a value.  It will not
	 * be shifted right; the caller must take care of that.
	 */
	private int getField(int val, int L, int R)	{
		int mask = (~(0x0) << (5 - R) * 6) & ~(~(0x0) << (5 - L + 1) * 6);

		return val & mask;
	}

	/**
	 * Copy the contents of the memory cell in a (offset by optional
	 * register i) into register r.  Only store the bytes indicated
	 * by f: 8L + R.
	 * 1.3.1, p. 129: The specified field of CONTENTS(M) replaces the previous contents of
	 * register A.  In all operations where a partial field is used as an input, the sign
	 * is used if it is part of the field, otherwise the sign + is understood.  The field
	 * is shifted over to the right-hand part of the register as it is loaded.
	 * LDi: the LDi instruction is undefined if it would result in setting bytes 1, 2, or 3
	 * to anything but 0.
	 * @param negate switch the sign of the value at the location
	 *
	 * F:(0:2)
	 *  3         2         1
	 * 10987654321098765432109876543210
	 *  +555555444444333333222222111111
	 * L		bit   R	  bit
   * 0		30		0   30
	 * 1    29    1   23
	 * 2    23    2   17
	 * 3    17    3 	11
	 * 4    11    4   5
	 * 5    5     5   0
	 * (& (0xCF << (L * 6) - 1)) >> R
	 */
	public void loadRegister(int a, int r, int i, int L, int R, boolean negate)
			throws MemoryLocationError, FieldError	{
		reg[r] = 0x0;

		// Now, copy the contents of memory location a, bytes L-R, into
		// r (right shifted all the way to the edge).
		int loc = a + this.reg[i + 1];	// only registers 1-6 can be used for indexing
		// TODO have to treat 0's specially
		if (L == 0)	{
			// 0 is the sign "byte", which I represent as a single bit here.
			reg[r] |= mem[loc] & SIGN_POS;
			L++;
		}

		if (L <= R)	{		// if it was (0:0), all done.  Otherwise copy bytes.
			reg[r] |= getField(mem[loc], L, R) >> ((5 - R) * 6);
		}

System.out.println("reg " + r + " = " + reg[r] + ", m[" + loc + "] = " + mem[loc]);

		// 0 0  0
		// 1 0  1
		// 0 1  1
		// 1 1  0

		if (negate)	{
			reg[r] ^= SIGN_POS;
		}
	}

	/**
	 * p. 130: The field F has the opposite siginficance from the load operation: the
	 * number of bytes in the field is taken from the right-hand portion of the register
	 * and shifted left if necessary to be insert in the proper field of CONTENTS(M).  The
	 * sign is not altered unless it is part of the field.  The contents of the register are
	 * not affected.
	 */
	public void storeRegister(int a, int r, int i, int L, int R)
			throws MemoryLocationError, FieldError	{
		int loc = a + ((i > 0) ? this.reg[i + 1] : 0);	// only registers 1-6 can be used for indexing

System.out.println("Storing into location " + a);

		// x y +
		// 0 0 0
		// 0 1 1
		// 1 0 1
		// 1 1 0

		// set sign bit
		if (L == 0)	{
			if ((reg[r] & SIGN_POS) == 0)	{
				mem[loc] &= ~SIGN_POS;
			} else	{
				mem[loc] |= SIGN_POS;
			}
			L++;
		}

System.out.println("L = " + L + ", R = " + R);

		if (L <= R)	{
			int width = (R - L + 1) * 6;
		  int mask = ~(~0x0 << width);
			int zero_mask = ~(~(0x0) << (5 - R) * 6) & ~(~(0x0) << (5 - L + 1) * 6);
System.out.println("width = " + width);
System.out.printf("mask = %08x\n", mask);
System.out.printf("zero_mask = %08x\n", zero_mask);
System.out.println("masked = " + (reg[r] & mask));
			// zero out just the target area (e.g. L-R)
			mem[loc] &= zero_mask;
			// insert the right-most bytes in the correct position
			mem[loc] |= (reg[r] & mask) << ((5 - R) * 6);
		}
	}

	/**
	 * Extract a partial field and add (or subtract) its contents to/from rA
	 * p. 131: If the magnitude is too large for register A, the overflow toggle is
	 * set on, and the remainder of the addition appearing in rA is as though a "1"
	 * had been carried into another register to the left of rA.  (Otherwise the
	 * setting of the overflow toggle is unchanged).  If the result is zero, the sign
	 * of rA is unchanged.
	 */
	public void add(int loc, int i, int L, int R, boolean negate)	{
		loc += (i > 0) ? reg[i + 1] : 0;
		reg[0] += getField(mem[loc], L, R) >> ((5 - R) * 6);
		// TODO subtraction
		// TODO overflow handling
	}

	/** 
	 * p. 131: THe 10-byte product, V times rA, replaces registers A and X.  The
	 * signs of rA and rX are both set to the algebraic sign of the product (namely,
	 * + if the signs are the same, - if different).
	 */
	public void multiply(int loc, int i, int L, int R)	{
	}

	/**
	 * The value of rA and rX, treated as a 10-byte number rAX with the sign of rA,
	 * is divided by the value V.  If V = 0 or the quotient is more than five bytes in
	 * magnitude (|rA| >= |V|), registers A and X are filled with undefined information
	 * and the overflow toggle is set on.  Otherwise the quotient +/-floor(|rAX/V|) is
	 * placed in rA and the remainder +/-floor(|rAX| mod |V|) is placed in rX.  The sign
	 * of rA afterwards is the algebraic sign of the quotient.
	 * The sign of rX afterwards is the previous sign of rA.
	 */
	public void divide(int loc, int i, int L, int R)	{
	}

	/**
	 * Compare the contents of register r with the memory at location loc
	 * (offset by r[i]) as specified by the field specification.  Set the
	 * comparison indicator based on the comparison to -1, 0 or 1:
   * if reg[r] < mem[loc], cmpi = -1
	 * if reg[r] = mem[loc], cmpi = 0
	 * if reg[r] > mem[loc], cmpi = +1
	 * p. 134: A minus zero is equal to a plus zero.
	 * The specified field of rA is compared with _the same_ field of CONTENTS(M).
	 * If F does not include the sign position, the fields are both considered
	 * nonnegative; otherwise the sign is taken into account in the comparison.
	 * CMPi: bytes 1, 2, and 3 of the index register are treated as 0 in the comparison
	 * (thus if F=(1:2), the result can not be greater).
	 */
	public void compare(int loc, int r, int i, int L, int R)	{
		loc += (i > 0) ? reg[i + 1] : 0;
		// TODO partial field comparisons
		if (mem[loc] < reg[r])	{
			compi = -1;
		} else if (mem[loc] > reg[r])	{
			compi = 1;
		} else	{
			compi = 0;
		}
	}

	/**
	 * p. 135: The number of words specified by F is moved, starting from location M to the
	 * location specified by the contents of register 1.  The transfer occurs one
	 * word at a time, and rI1 is increased by the value of F at the end of the operation.
	 * If F = 0, nothing happens.
	 * If F = 3 and M = 100, then if rI1 = 999, we transfer 1000->999, 1001->1000, 1002->1001.
	 * If rI1 = 1001, 1000->1001, 1001->1002, 1002->1003; the same word contents(1000) into
	 * three places.
	 */
	public void moveWords(int loc, int i, int f)	{
		loc += (i > 0) ? reg[i + 1] : 0;
		// Can't use arraycopy here, since that doesn't preserve the original
		// MIX specification.  Have to copy one word at a time.
		while (f-- > 0)	{
			mem[reg[1]] = mem[loc];
			loc++;
			reg[1]++;
		}
	}

	/**
	 * p.136: When character-code input is being done, the signs of all words are set to +;
	 * on output, signs are ignored.
	 * on every in, out or ioc instruction, the 100-word block is specified by the current
	 * contents of rX.
	 * The machine will wait at this point if a preceding operation is not yet complete.
	 */
	public void input()	{
	}

	/**
	 * Registers A and X are assumed to contain a 10-byte number in character code;
	 * NUM sets the magnitude or rA equal to the numeric value of this number (decimal).
	 * The value of rX and the sign of rA are unchanged.  Byte 00, 10, 20, 30, 40 convert
	 * the the digit zero, byte 01, 11, 21, convert to one, etc.  Overflow is possible;
	 * the remainder mod 64^5 is retained.
	 */
	public void convertToNum()	{
	}

	/**
	 * The value in rA is converted to a 10-byte decimal number that is put into registers
	 * A and X in character code.  The signs of rA and rX are ignored.
	 */
	public void convertToChar()	{
	}

	/**
	 * for testing.
	 */
	public int getRegister(int r)	{
		return reg[r];
	}

	/**
	 * p. 134: When a jump takes place, the J-register is set to the
	 * address of the next instruction (the address of the instruction that
	 * would have been next if we hadn't jumped).
	 */
	public void conditionalJump(int f, int loc) throws FieldError	{
		if (f != 1)	{
			rJ = this.pc + 1;
		}
		switch (f)	{
			case 0: // JMP: unconditional
			case 1: // JSJ: jump, save J; doen't update rJ
				this.pc = loc;
				break;
			case 2: // JOV: if overflow toggle is on, it is turned off and a jump occurs; otherwise nothing happens
				if (ovtog)	{
					this.ovtog = false;
					this.pc = loc;
				}
				break;
			case 3: // JNOV: if the overflow toggle is off, a JMP occurs; otherwise it is turned off
				if (!ovtog)	{
					this.pc = loc;
				}
				this.ovtog = false;
				break;
			case 4:	// JL
				if (compi < 0)	{
					this.pc = loc;
				}
				break;
			case 5: // JE
				if (compi == 0)	{
					this.pc = loc;
				}
				break;
			case 6: // JG
				if (compi > 0)	{
					this.pc = loc;
				}
				break;
			case 7: // JGE
				if (compi >= 0)	{
					this.pc = loc;
				}
				break;
			case 8: // JNE
				if (compi != 0)	{
					this.pc = loc;
				}
				break;
			case 9: // JLE: Jump if the comparison indicator is set.  The comparison indicator is not changed by these instructions.
				if (compi <= 0)	{
					this.pc = loc;
				}
				break;
			default:
				throw new FieldError("Invalid F-specification " + f +
					" for instruction code 39 (jump); expected 0-9");
		}
	}

	/**
	 * Start running the MIX program located at {@param pc} and keep
	 * running until the HLT (0) instruction is encountered.
	 */
	public void run(int startLocation)	throws IllegalInstruction, 
																 MemoryLocationError,
																 NotImplemented,
																 FieldError	{
		pc = startLocation;
		while (true)	{
			try	{
System.out.println(pc + ": " + showWord(5, mem[pc]));
				MixInst in = new MixInst(mem[pc]);
				pc++;	// if in is a jump instruction, pc will be modified by it
				if (!in.execute(this))	{
					break;
				}
			} catch (IllegalInstruction e)	{
				e.attachLocation(pc);
				throw e;
			} catch (MemoryLocationError e)	{
				e.attachLocation(pc);
				throw e;
			}
		}
	}

	/**
	 * Convert a 32-bit integer into a 5- or 2-byte mix word for
	 * printing.
	 */
	private String showWord(int size, int val)	{
		String s = ((val & (0x01 << 31)) == 0) ? "+" : "-";

		int shift = (size - 1) * 6;

		while (shift >= 0)	{
			s += " " + ((val & (0x3F << shift)) >> shift);
			shift -= 6;
		}

		return s;
	}

	public void dumpState()	{
		System.out.println("rA: " + showWord(5, reg[0]));
		System.out.println("r1: " + showWord(2, reg[1]));
		System.out.println("r2: " + showWord(2, reg[2]));
		System.out.println("r3: " + showWord(2, reg[3]));
		System.out.println("r4: " + showWord(2, reg[4]));
		System.out.println("r5: " + showWord(2, reg[5]));
		System.out.println("r6: " + showWord(2, reg[6]));
		System.out.println("rX: " + showWord(5, reg[7]));
	}

	public void showMemory(int start, int end)	{
		while (start <= end)	{
			System.out.println("m[" + start + "] = " + showWord(5, mem[start]));
			start++;
		}
	}

	public static void main(String[] args) {
		try	{
			MixVM vm = new MixVM();
			vm.mem[2000] = (1 << 24) |
						 (2 << 18) |
						 (3 << 12) |
						 (4 << 6) |
						 5;

						 /*
			vm.mem[3000] = (31 << 24) |		// LDA 2000,0(0:5)
									(16 << 18) |
									(5 << 6) |
									8;
									*/

			vm.mem[3003] = (2 << 6) | 5;	// HLT 0,0(0:2)
			vm.run(3000);
			vm.dumpState();
		} catch (MixException e)	{
			System.out.print("At " + e.getLocation() + ": ");
			e.printStackTrace();
		}
	}
}
