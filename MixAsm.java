package com.jdavies.mix;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * A standard specification for a MIX instruction.
 */
class MixInstSpec	{
	private int c;
	private int time;
	private int L;
	private int R;

	/**
	 * Record the standard settings for a MIX instruction, if the
	 * operation isn't overridden.
	 */
	public MixInstSpec(int c, int time, int L, int R)	{
		this.c = c;
		this.time = time;
		this.L = L;
		this.R = R;
	}

	public int getC() { return c; }
	public int getTime() { return time; }
	public int getL() { return L; }
	public int getR() { return R; }
}

/**
 * Assemble a plaintext file into an executable MIX program (executable by {@link MixVM}).
 */
public class MixAsm	{
	/**
	 * Static mapping of opcodes to code values; this isn't a simple list, since multiple
	 * opcodes map onto a single value (like JAP/JP, ENTA/INCA).
	 */
	private static Map<String, MixInstSpec> opcodes = new HashMap<String, MixInstSpec>();

	static	{
		// C	t	 L	R
		opcodes.put("NOP", new MixInstSpec(0, 1, 0, 0));	// 1  0  0
		opcodes.put("ADD", new MixInstSpec(1, 2, 0, 5));	// 2  0  5
		opcodes.put("SUB", new MixInstSpec(2, 2, 0, 5));	// 2  0  5
		opcodes.put("MUL", new MixInstSpec(3, 10, 0, 5));	// 10 0  5
		opcodes.put("DIV", new MixInstSpec(4, 12, 0, 5));	// 12 0  5
		opcodes.put("NUM", new MixInstSpec(5, 10, 0, 0));	// 10	0  0
		opcodes.put("CHAR", new MixInstSpec(5, 10, 0, 1));	// 10 0  1
		opcodes.put("HLT", new MixInstSpec(5, 10, 0, 2));	// 10 0  2
		opcodes.put("SLA", new MixInstSpec(6, 2, 0, 0));	// 2  0  0
		opcodes.put("SRA", new MixInstSpec(6, 2, 0, 1));	// 2  0  1
		opcodes.put("SLAX", new MixInstSpec(6, 2, 0, 2));	// 2  0  2
		opcodes.put("SRAX", new MixInstSpec(6, 2, 0, 3));	// 2  0  3
		opcodes.put("SLC", new MixInstSpec(6, 2, 0, 4));	// 2  0  4
		opcodes.put("SRC", new MixInstSpec(6, 2, 0, 5));	// 2  0  5
		opcodes.put("MOVE", new MixInstSpec(7, 1, 0, 1));	// + 2*F
		opcodes.put("LDA", new MixInstSpec(8, 2, 0, 5));	// 2	0  5
		opcodes.put("LD1", new MixInstSpec(9, 2, 0, 5));	// 2	0  5
		opcodes.put("LD2", new MixInstSpec(10, 2, 0, 5));	// 2	0  5
		opcodes.put("LD3", new MixInstSpec(11, 2, 0, 5));	// 2	0  5
		opcodes.put("LD4", new MixInstSpec(12, 2, 0, 5));	// 2	0  5
		opcodes.put("LD5", new MixInstSpec(13, 2, 0, 5));	// 2	0  5
		opcodes.put("LD6", new MixInstSpec(14, 2, 0, 5));	// 2	0  5
		opcodes.put("LDX", new MixInstSpec(15, 2, 0, 5));	// 2	0  5
		opcodes.put("LDAN", new MixInstSpec(16, 2, 0, 5));	// 2	0  5
		opcodes.put("LD1N", new MixInstSpec(17, 2, 0, 5));	// 2	0  5
		opcodes.put("LD2N", new MixInstSpec(18, 2, 0, 5));	// 2	0  5
		opcodes.put("LD3N", new MixInstSpec(19, 2, 0, 5));	// 2	0  5
		opcodes.put("LD4N", new MixInstSpec(20, 2, 0, 5));	// 2	0  5
		opcodes.put("LD5N", new MixInstSpec(21, 2, 0, 5));	// 2	0  5
		opcodes.put("LD6N", new MixInstSpec(22, 2, 0, 5));	// 2	0  5
		opcodes.put("LDXN", new MixInstSpec(23, 2, 0, 5));	// 2	0  5
		opcodes.put("STA", new MixInstSpec(24, 2, 0, 5));	// 2	0  5
		opcodes.put("ST1", new MixInstSpec(25, 2, 0, 5));	// 2	0  5
		opcodes.put("ST2", new MixInstSpec(26, 2, 0, 5));	// 2	0  5
		opcodes.put("ST3", new MixInstSpec(27, 2, 0, 5));	// 2	0  5
		opcodes.put("ST4", new MixInstSpec(28, 2, 0, 5));	// 2	0  5
		opcodes.put("ST5", new MixInstSpec(29, 2, 0, 5));	// 2	0  5
		opcodes.put("ST6", new MixInstSpec(30, 2, 0, 5));	// 2	0  5
		opcodes.put("STX", new MixInstSpec(31, 2, 0, 5));	// 2	0  5
		opcodes.put("STJ", new MixInstSpec(32, 2, 0, 5));	// 2  0  2
		opcodes.put("STZ", new MixInstSpec(33, 2, 0, 5)); // 2  0  5
		opcodes.put("JBUS", new MixInstSpec(34, 1, 0, 0));	// 
		opcodes.put("IOC", new MixInstSpec(35, 1, 0, 0));		// + T (device busy)
		opcodes.put("IN", new MixInstSpec(36, 1, 0, 0));		// + T (device busy)
		opcodes.put("OUT", new MixInstSpec(37, 1, 0, 0));	 // + T (device busy)
		opcodes.put("JRED", new MixInstSpec(38, 1, 0, 0));	// 1  0  0
		opcodes.put("JMP", new MixInstSpec(39, 1, 0, 0));		// 1  0  0
		opcodes.put("JSJ", new MixInstSpec(39, 1, 0, 1));		// 1  0  1
		opcodes.put("JOV", new MixInstSpec(39, 1, 0, 2));	  // 1  0  2
		opcodes.put("JNOV", new MixInstSpec(39, 1, 0, 3));	// 1  0  3
		opcodes.put("JL", new MixInstSpec(39, 1, 0, 4));	// 1  0  3
		opcodes.put("JE", new MixInstSpec(39, 1, 0, 5));	// 1  0  3
		opcodes.put("JG", new MixInstSpec(39, 1, 0, 6));	// 1  0  3
		opcodes.put("JGE", new MixInstSpec(39, 1, 0, 7));	// 1  0  3
		opcodes.put("JNE", new MixInstSpec(39, 1, 0, 8));	// 1  0  3
		opcodes.put("JLE", new MixInstSpec(39, 1, 0, 9));	// 1  0  3
		opcodes.put("JAN", new MixInstSpec(40, 0, 0, 0));
		opcodes.put("J1N", new MixInstSpec(41, 0, 0, 0));
		opcodes.put("J2N", new MixInstSpec(42, 0, 0, 0));
		opcodes.put("J3N", new MixInstSpec(43, 0, 0, 0));
		opcodes.put("J4N", new MixInstSpec(44, 0, 0, 0));
		opcodes.put("J5N", new MixInstSpec(45, 0, 0, 0));
		opcodes.put("J6N", new MixInstSpec(46, 0, 0, 0));
		opcodes.put("JXN", new MixInstSpec(47, 0, 0, 0));
		opcodes.put("JAZ", new MixInstSpec(40, 0, 0, 1));
		opcodes.put("J1Z", new MixInstSpec(41, 0, 0, 1));
		opcodes.put("J2Z", new MixInstSpec(42, 0, 0, 1));
		opcodes.put("J3Z", new MixInstSpec(43, 0, 0, 1));
		opcodes.put("J4Z", new MixInstSpec(44, 0, 0, 1));
		opcodes.put("J5Z", new MixInstSpec(45, 0, 0, 1));
		opcodes.put("J6Z", new MixInstSpec(46, 0, 0, 1));
		opcodes.put("JXZ", new MixInstSpec(47, 0, 0, 1));
		opcodes.put("JAP", new MixInstSpec(40, 0, 0, 2));
		opcodes.put("J1P", new MixInstSpec(41, 0, 0, 2));
		opcodes.put("J2P", new MixInstSpec(42, 0, 0, 2));
		opcodes.put("J3P", new MixInstSpec(43, 0, 0, 2));
		opcodes.put("J4P", new MixInstSpec(44, 0, 0, 2));
		opcodes.put("J5P", new MixInstSpec(45, 0, 0, 2));
		opcodes.put("J6P", new MixInstSpec(46, 0, 0, 2));
		opcodes.put("JXP", new MixInstSpec(47, 0, 0, 2));
		opcodes.put("JANN", new MixInstSpec(40, 0, 0, 3));
		opcodes.put("J1NN", new MixInstSpec(41, 0, 0, 3));
		opcodes.put("J2NN", new MixInstSpec(42, 0, 0, 3));
		opcodes.put("J3NN", new MixInstSpec(43, 0, 0, 3));
		opcodes.put("J4NN", new MixInstSpec(44, 0, 0, 3));
		opcodes.put("J5NN", new MixInstSpec(45, 0, 0, 3));
		opcodes.put("J6NN", new MixInstSpec(46, 0, 0, 3));
		opcodes.put("JXNN", new MixInstSpec(47, 0, 0, 3));
		opcodes.put("JANZ", new MixInstSpec(40, 0, 0, 4));
		opcodes.put("J1NZ", new MixInstSpec(41, 0, 0, 4));
		opcodes.put("J2NZ", new MixInstSpec(42, 0, 0, 4));
		opcodes.put("J3NZ", new MixInstSpec(43, 0, 0, 4));
		opcodes.put("J4NZ", new MixInstSpec(44, 0, 0, 4));
		opcodes.put("J5NZ", new MixInstSpec(45, 0, 0, 4));
		opcodes.put("J6NZ", new MixInstSpec(46, 0, 0, 4));
		opcodes.put("JXNZ", new MixInstSpec(47, 0, 0, 4));
		opcodes.put("JANP", new MixInstSpec(40, 0, 0, 5));
		opcodes.put("J1NP", new MixInstSpec(41, 0, 0, 5));
		opcodes.put("J2NP", new MixInstSpec(42, 0, 0, 5));
		opcodes.put("J3NP", new MixInstSpec(43, 0, 0, 5));
		opcodes.put("J4NP", new MixInstSpec(44, 0, 0, 5));
		opcodes.put("J5NP", new MixInstSpec(45, 0, 0, 5));
		opcodes.put("J6NP", new MixInstSpec(46, 0, 0, 5));
		opcodes.put("JXNP", new MixInstSpec(47, 0, 0, 5));
		opcodes.put("INCA", new MixInstSpec(48, 0, 0, 0));
		opcodes.put("INC1", new MixInstSpec(49, 0, 0, 0));
		opcodes.put("INC2", new MixInstSpec(50, 0, 0, 0));
		opcodes.put("INC3", new MixInstSpec(51, 0, 0, 0));
		opcodes.put("INC4", new MixInstSpec(52, 0, 0, 0));
		opcodes.put("INC5", new MixInstSpec(53, 0, 0, 0));
		opcodes.put("INC6", new MixInstSpec(54, 0, 0, 0));
		opcodes.put("INCX", new MixInstSpec(55, 0, 0, 0));
		opcodes.put("DECA", new MixInstSpec(48, 0, 0, 0));
		opcodes.put("DEC1", new MixInstSpec(49, 0, 0, 1));
		opcodes.put("DEC2", new MixInstSpec(50, 0, 0, 1));
		opcodes.put("DEC3", new MixInstSpec(51, 0, 0, 1));
		opcodes.put("DEC4", new MixInstSpec(52, 0, 0, 1));
		opcodes.put("DEC5", new MixInstSpec(53, 0, 0, 1));
		opcodes.put("DEC6", new MixInstSpec(54, 0, 0, 1));
		opcodes.put("DECX", new MixInstSpec(55, 0, 0, 1));
		opcodes.put("ENTA", new MixInstSpec(48, 1, 0, 2));
		opcodes.put("ENT1", new MixInstSpec(49, 1, 0, 2));
		opcodes.put("ENT2", new MixInstSpec(50, 1, 0, 2));
		opcodes.put("ENT3", new MixInstSpec(51, 1, 0, 2));
		opcodes.put("ENT4", new MixInstSpec(52, 1, 0, 2));
		opcodes.put("ENT5", new MixInstSpec(53, 1, 0, 2));
		opcodes.put("ENT6", new MixInstSpec(54, 1, 0, 2));
		opcodes.put("ENTX", new MixInstSpec(55, 1, 0, 2));
		opcodes.put("ENNA", new MixInstSpec(48, 1, 0, 2));
		opcodes.put("ENN1", new MixInstSpec(49, 1, 0, 3));
		opcodes.put("ENN2", new MixInstSpec(50, 1, 0, 3));
		opcodes.put("ENN3", new MixInstSpec(51, 1, 0, 3));
		opcodes.put("ENN4", new MixInstSpec(52, 1, 0, 3));
		opcodes.put("ENN5", new MixInstSpec(53, 1, 0, 3));
		opcodes.put("ENN6", new MixInstSpec(54, 1, 0, 3));
		opcodes.put("ENNX", new MixInstSpec(55, 1, 0, 3));
		opcodes.put("CMPA", new MixInstSpec(56, 0, 0, 0));
		opcodes.put("CMP1", new MixInstSpec(57, 0, 0, 0));
		opcodes.put("CMP2", new MixInstSpec(58, 0, 0, 0));
		opcodes.put("CMP3", new MixInstSpec(59, 0, 0, 0));
		opcodes.put("CMP4", new MixInstSpec(60, 0, 0, 0));
		opcodes.put("CMP5", new MixInstSpec(61, 0, 0, 0));
		opcodes.put("CMP6", new MixInstSpec(62, 0, 0, 0));
		opcodes.put("CMPX", new MixInstSpec(63, 0, 0, 0));
		// psuedo-operations; map these all to invalid opcode values
		opcodes.put("EQU", new MixInstSpec(64, 0, 0, 0));
		opcodes.put("CON", new MixInstSpec(65, 0, 0, 0));
		opcodes.put("ALF", new MixInstSpec(66, 0, 0, 0));
		opcodes.put("ORIG", new MixInstSpec(67, 0, 0, 0));
		opcodes.put("END", new MixInstSpec(68, 0, 0, 0));
	};

	private int pc;
	private Map<String, Integer> symbolTable = new HashMap<String, Integer>();

	// Write directly into this memory area; the output routine will compress this by
	// removing contiguous regions of zeros.
	private int mem[] = new int[4000];

	public MixAsm()	{
		this.pc = 0;	// always start assembling at 0 by default, although most programs change this.
	}

	/** 
	 * location can be:
	 * *, indicating current location counter
	 * simple arithmetic, including:
	 * +,-
	 * a numeric
	 * a symbol (defined or a placeholder)
	 * It must ultimately resolve to a numeric value, though.
	 */
	private int parseLocation(String location)	{
		int ilocation = 0;
		StringTokenizer locationParser = new StringTokenizer(location, "+-", true);

		while (locationParser.hasMoreTokens())	{
			String token = locationParser.nextToken();
			if ("*".equals(token))	{
				location += this.pc;
			} else if ("+".equals(token))	{
				// TODO
			}
		}

		if ("*".equals(location))	{
			ilocation = this.pc;
		} else	{
			try	{
				ilocation = Integer.parseInt(location);
			} catch (NumberFormatException e)	{
				if (symbolTable.get(location) != null)	{
					ilocation = symbolTable.get(location);
				} else	{
					// TODO record a forward reference
				}
			}
		}

		return ilocation;
	}

	/**
	 * A line of MIX assembler is:
	 * 1) (optional) label
	 * 2) opcode
	 * 3) (optional depending on the opcode) address
	 * 4) (optional) comments
	 * If the line starts with whitespace, it's unlabelled; the next token is an opcode.
	 * The opcode MUST be preceded by whitespace or a label, and the address part is usually
	 * required, but for instructions like HLT it isn't.  opcodes can be pseudo-ops like EQU,
	 * CON, ALF or ORIG as well.
	 */
	private void assembleLine(String line) throws SyntaxException	{
		StringTokenizer tok = new StringTokenizer(line, " \t");
		String label = null;
		String opcode;
		String address = null;
		int c;
		int f;
		int i;
		int a;

		if (line.charAt(0) != ' ' && line.charAt(0) != '\t')	{
			label = tok.nextToken();
		}
		opcode = tok.nextToken();
		if (tok.hasMoreTokens())	{
			address = tok.nextToken();
		}

System.out.println(label + ":" + opcode + ":" + address);
		if (label != null)	{
			if (symbolTable.get(label) != null)	{
				throw new SyntaxException("Duplicate symbol '" + label + "', first seen at location " +
					symbolTable.get(label));
			}
			symbolTable.put(label, pc);
		}
		if (opcodes.get(opcode) == null)	{
			throw new SyntaxException("Unrecognized opcode '" + opcode + "'");
		}
		MixInstSpec spec = opcodes.get(opcode);
		c = spec.getC();
		// The really hard part here is interpreting the address part of the instruction

		// location[,index][(L[:R])]
		// TODO W-Expressions
		// TODO regexp instead?  This is awfully complex...
		String location = null;
		String index = null;
		String L = null;
		String R = null;
		if (address != null)	{
			StringTokenizer addrParser = new StringTokenizer(address, ",():", true);

			location = addrParser.nextToken();
			if (addrParser.hasMoreTokens())	{
				String sep = addrParser.nextToken();
				if (sep.equals(","))	{
					index = addrParser.nextToken();
					if (addrParser.hasMoreTokens())	{
						sep = addrParser.nextToken();
					} else	{
						sep = null;
					}
				}
System.out.println(sep);
				if (sep != null && sep.equals("("))	{	// must be a , or a (
					L = addrParser.nextToken();
					if (!addrParser.hasMoreTokens())	{
						throw new SyntaxException("Unexpected end of expression in '" +
							address + "'");
					}
					sep = addrParser.nextToken();
					if (sep.equals(":"))	{
						R = addrParser.nextToken();
						if (!addrParser.hasMoreTokens())	{
							throw new SyntaxException("Unexpected end of expression in '" +
								address + "'");
						}
						sep = addrParser.nextToken();
					}
					if (!sep.equals(")"))	{
						throw new SyntaxException("Expected ')', got '" + sep + "'");
					}
				} else	{
					if (sep != null)	{
						throw new SyntaxException("Expected '(', got '" + sep + "'");
					}
				}
			}
		}

		int ilocation = 0;
		int iindex = 0;
		int iL = spec.getL();
		int iR = spec.getR();

		if (location != null)	{
			ilocation = parseLocation(location);
		}

		if (index != null)	{
			try	{
				iindex = Integer.parseInt(index);
			} catch (NumberFormatException e)	{
				throw new SyntaxException("Non-numeric index '" + index + "'");
			}
		}

		if (L != null)	{
			try	{
				iL = Integer.parseInt(L);
			} catch (NumberFormatException e)	{
				throw new SyntaxException("Non-numeric L '" + L + "'");
			}

			if (R == null)	{	// if there's only one value, it's R, not L
				iR = iL;
				iL = 0;
			}
		}

		if (R != null)	{
			try	{
				iR = Integer.parseInt(R);
			} catch (NumberFormatException e)	{
				throw new SyntaxException("Non-numeric R '" + R + "'");
			}
		}

		if (c > 63)	{
			// Handle psuedo-operations: e.g. assembler instructions
			if ("ORIG".equals(opcode))	{
				pc = ilocation;
			} else if ("CON".equals(opcode))	{
				mem[pc] = ilocation;
				pc++;
			} else if ("EQU".equals(opcode))	{
				symbolTable.put(label, ilocation);
			}
		} else	{
			// Actually assemble something
			// TODO correct F-spec defaults for non-load/store instructions.

			MixInst inst = new MixInst(MixOpCode.values()[c], iindex,
				iL, iR, ilocation);
System.out.println(pc + ": " + inst.toString());
			mem[pc] = inst.pack();
			pc++;
		}
	}

	public boolean assemble(BufferedReader in) throws IOException	{
		int lineCounter = 0;	// different than program counter
		String line;
		System.out.println("label\topcode\taddress");
		boolean succeeded = true;
		while ((line = in.readLine()) != null)	{
			lineCounter++;
			if ((line.charAt(0) == '*') || line.trim().length() == 0)	{
				// Skip blank or comment lines
				continue;
			}
			try	{
				assembleLine(line);
			} catch (SyntaxException e)	{
				System.out.print("At line " + lineCounter + ", input '" +  line + "': ");
				e.printStackTrace();
				succeeded = false;
			}
		}

		return succeeded;
	}

	/**
	 * Create a VM, run the assembled program and output the VM
	 * status.  For testing only.
	 */
	public void run() throws MixException	{
		MixVM vm = new MixVM(this.mem);
		vm.dumpState();
		vm.showMemory(2000,2010);
		vm.run(symbolTable.get("START"));
		vm.dumpState();
		vm.showMemory(2000,2010);
	}
	
	public static void main(String[] args) throws IOException	{
		if (args.length < 1)	{
			System.err.println("Usage: MixASM <mixal file>");
			System.exit(0);
		}

		MixAsm assembler = new MixAsm();
		if (assembler.assemble(new BufferedReader(new FileReader(args[0]))))	{
			try	{
				assembler.run();
			} catch (MixException e)	{
				e.printStackTrace();
			}
		}
	}
}
