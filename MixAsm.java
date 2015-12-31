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
 * Assemble a plaintext file into an executable MIX program (executable by {@link MixVM}).
 */
public class MixAsm	{
	/**
	 * Static mapping of opcodes to code values; this isn't a simple list, since multiple
	 * opcodes map onto a single value (like JAP/JP, ENTA/INCA).
	 */
	private static Map<String, Integer> opcodes = new HashMap<String, Integer>();

	static	{
		// C	t	 L	R
		opcodes.put("NOP", 0);	// 1  0  0
		opcodes.put("ADD", 1);	// 2  0  5
		opcodes.put("SUB", 2);	// 2  0  5
		opcodes.put("MUL", 3);	// 10 0  5
		opcodes.put("DIV", 4);	// 12 0  5
		opcodes.put("NUM", 5);	// 10	0  0
		opcodes.put("CHAR", 5);	// 10 0  1
		opcodes.put("HLT", 5);	// 10 0  2
		opcodes.put("SLA", 6);	// 2  0  0
		opcodes.put("SRA", 6);	// 2  0  1
		opcodes.put("SLAX", 6);	// 2  0  2
		opcodes.put("SRAX", 6);	// 2  0  3
		opcodes.put("SLC", 6);	// 2  0  4
		opcodes.put("SRC", 6);	// 2  0  5
		opcodes.put("MOVE", 7);	// 1 + 2F	0 1
		opcodes.put("LDA", 8);	// 2	0  5
		opcodes.put("LD1", 9);	// 2	0  5
		opcodes.put("LD2", 10);	// 2	0  5
		opcodes.put("LD3", 11);	// 2	0  5
		opcodes.put("LD4", 12);	// 2	0  5
		opcodes.put("LD5", 13);	// 2	0  5
		opcodes.put("LD6", 14);	// 2	0  5
		opcodes.put("LDX", 15);	// 2	0  5
		opcodes.put("LDAN", 16);	// 2	0  5
		opcodes.put("LD1N", 17);	// 2	0  5
		opcodes.put("LD2N", 18);	// 2	0  5
		opcodes.put("LD3N", 19);	// 2	0  5
		opcodes.put("LD4N", 20);	// 2	0  5
		opcodes.put("LD5N", 21);	// 2	0  5
		opcodes.put("LD6N", 22);	// 2	0  5
		opcodes.put("LDXN", 23);	// 2	0  5
		opcodes.put("STA", 24);	// 2	0  5
		opcodes.put("ST1", 25);	// 2	0  5
		opcodes.put("ST2", 26);	// 2	0  5
		opcodes.put("ST3", 27);	// 2	0  5
		opcodes.put("ST4", 28);	// 2	0  5
		opcodes.put("ST5", 29);	// 2	0  5
		opcodes.put("ST6", 30);	// 2	0  5
		opcodes.put("STX", 31);	// 2	0  5
		opcodes.put("STJ", 32);	// 2  0  2
		opcodes.put("STZ", 33); // 2  0  5
		opcodes.put("JBUS", 34);	// 1	0  0
		opcodes.put("IOC", 35);		// 1+T  0  0
		opcodes.put("IN", 36);		// 1+T  0  0
		opcodes.put("OUT", 37);	 // 1+T  0  0
		opcodes.put("JRED", 38);	// 1  0  0
		opcodes.put("JMP", 39);		// 1  0  0
		opcodes.put("JSJ", 39);		// 1  0  1
		opcodes.put("JOV", 39);	  // 1  0  2
		opcodes.put("JNOV", 39);	// 1  0  3
		opcodes.put("JAP", 40);
		opcodes.put("J1P", 41);
		opcodes.put("J2P", 42);
		opcodes.put("J3P", 43);
		opcodes.put("J4P", 44);
		opcodes.put("J5P", 45);
		opcodes.put("J6P", 46);
		opcodes.put("JXP", 47);
		opcodes.put("INCA", 48);
		opcodes.put("INC1", 49);
		opcodes.put("INC2", 50);
		opcodes.put("INC3", 51);
		opcodes.put("INC4", 52);
		opcodes.put("INC5", 53);
		opcodes.put("INC6", 54);
		opcodes.put("INCX", 55);
		opcodes.put("ENTA", 48);
		opcodes.put("ENT1", 49);
		opcodes.put("ENT2", 50);
		opcodes.put("ENT3", 51);
		opcodes.put("ENT4", 52);
		opcodes.put("ENT5", 53);
		opcodes.put("ENT6", 54);
		opcodes.put("ENTX", 55);
		opcodes.put("CMPA", 56);
		opcodes.put("CMP1", 57);
		opcodes.put("CMP2", 58);
		opcodes.put("CMP3", 59);
		opcodes.put("CMP4", 60);
		opcodes.put("CMP5", 61);
		opcodes.put("CMP6", 62);
		opcodes.put("CMPX", 63);
		// psuedo-operations; map these all to invalid opcode values
		opcodes.put("EQU", 64);
		opcodes.put("CON", 65);
		opcodes.put("ALF", 66);
		opcodes.put("ORIG", 67);
		opcodes.put("END", 68);
	};

	private int pc;
	private Map<String, Integer> symbolTable = new HashMap<String, Integer>();

	// Write directly into this memory area; the output routine will compress this by
	// removing contiguous regions of zeros.
	private int memory[] = new int[4000];

	public MixAsm()	{
		this.pc = 0;	// always start assembling at 0 by default, although most programs change this.
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
		c = opcodes.get(opcode);
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
				if (sep.equals(','))	{
					index = addrParser.nextToken();
					if (addrParser.hasMoreTokens())	{
						sep = addrParser.nextToken();
					} else	{
						sep = null;
					}
				}
				if (sep != null && sep.equals('('))	{	// must be a , or a (
					L = addrParser.nextToken();
					if (!addrParser.hasMoreTokens())	{
						throw new SyntaxException("Unexpected end of expression in '" +
							address + "'");
					}
					sep = addrParser.nextToken();
					if (sep.equals(':'))	{
						R = addrParser.nextToken();
						if (!addrParser.hasMoreTokens())	{
							throw new SyntaxException("Unexpected end of expression in '" +
								address + "'");
						}
						sep = addrParser.nextToken();
					}
					if (!sep.equals(')'))	{
						throw new SyntaxException("Expected ')', got '" + sep + "'");
					}
				} else	{
					throw new SyntaxException("Expected '(', got '" + sep + "')");
				}
			}
		}

		int ilocation = 0;
		int iindex = 0;
		int iL = 0;
		int iR = 5;

		try	{
			ilocation = Integer.parseInt(location);
		} catch (NumberFormatException e)	{
			if (symbolTable.get(location) != null)	{
				ilocation = symbolTable.get(location);
			} else	{
				// TODO record a forward reference
			}
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
			}
		} else	{
			// Actually assemble something
			// TODO correct F-spec defaults for non-load/store instructions.

			MixInst inst = new MixInst(MixOpCode.values()[c], ilocation, iindex,
				iL, iR);
System.out.println(pc + ": " + inst.toString());
			memory[pc] = inst.pack();
			pc++;
		}
	}

	public void assemble(BufferedReader in) throws IOException	{
		int lineCounter = 0;	// different than program counter
		String line;
		System.out.println("label\topcode\taddress");
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
			}
		}
	}
	
	public static void main(String[] args) throws IOException	{
		if (args.length < 1)	{
			System.err.println("Usage: MixASM <mixal file>");
			System.exit(0);
		}

		MixAsm assembler = new MixAsm();
		assembler.assemble(new BufferedReader(new FileReader(args[0])));
	}
}
