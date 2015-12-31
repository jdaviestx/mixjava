import java.io.BufferedReader;
import java.io.StringReader;

public class MixAsmTest	{
	private static void assertAssembly(String in, MixInst inst) 
			throws IOException	{
		MixAsm asm = new MixAsm();
		asm.assemble(new BufferedReader(new StringReader(in)));
		// TODO assert that I actually got back the right instruction
	}

	/**
	 * Examples from TAOCP, 1.3.1
	 */
	public static void testAssembly() throws IOException	{
		assertAssembly("	LDA 2000,2(0:3)", 
			new MixInst(MixOpCode.LDA, 2, 0, 3, 2000)); // +2000, 2, 3, 8);
		assertAssembly("	LDA 2000,2(1:3)",
			new MixInst(MixOpCode.LDA, 2, 8, 3, 2000)); // +2000, 2, 11, 8);
		assertAssembly("	LDA 2000(1:3)",
			new MixInst(MixOpCode.LDA, 0, 8, 3, 2000)); // +2000, 0, 3, 8);
		assertAssembly("	LDA 2000",
			new MixInst(MixOpCode.LDA, 0, 0, 5, 2000)); // +2000, 0, 5, 8);
		assertAssembly("	LDA -2000,4",
			new MixInst(MixOpCode.LDA, 4, 0, 5, -2000)); // -2000, 4, 5, 8);
	}
}
