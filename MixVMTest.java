public class MixVMTest	{
	public static boolean assertEquals(int a, int b)	{
		if (a != b)	{
			System.err.println("ERROR: Expected " + a + " but got " + b);
			return false;
		}

		return true;
	}

	public static void testLoading() throws Exception	{
		MixVM vm = new MixVM();
		int[] expectedValues = new int[] {
			0,																					// 0:0
			1,																					// 0:1
			1 << 6 | 2,																	// 0:2
			1 << 12 | 2 << 6 | 3,												// 0:3
			1 << 18 | 2 << 12 | 3 << 6 | 4,							// 0:4
			1 << 24 | 2 << 18 | 3 << 12 | 4 << 6 | 5,		// 0:5
			1,																					// 1:1
			1 << 6 | 2,																	// 1:2
			1 << 12 | 2 << 6 | 3,												// 1:3
			1 << 18 | 2 << 12 | 3 << 6 | 4,							// 1:4
			1 << 24 | 2 << 18 | 3 << 12 | 4 << 6 | 5,		// 1:5
			2,																					// 2:2
			2 << 6 | 3,																	// 2:3
			2 << 12 | 3 << 6 | 4,												// 2:4
			2 << 18 | 3 << 12 | 4 << 6 | 5,		 					// 2:5
			3,																					// 3:3
			3 << 6 | 4,																	// 3:4
			3 << 12 | 4 << 6 | 5,												// 3:5
			4,																					// 4:4
			4 << 6 | 5,																	// 4:5
			5 																					// 5:5
		};
		MixOpCode opcodes[] = new MixOpCode[] {
				MixOpCode.LDA,
				MixOpCode.LD1,
				MixOpCode.LD2,
				MixOpCode.LD3,
				MixOpCode.LD4,
				MixOpCode.LD5,
				MixOpCode.LD6,
				MixOpCode.LDX
		};
		// Load memory address 2000 with 12345
		int testVector = 1 << 24 | 2 << 18 | 3 << 12 | 4 << 6 | 5;
		vm.loadMemory(2000, new int[] {testVector});

		int counter = 0;
		// Load each register
		for (int r = 0; r < 8; r++	)	{
			// Cycle through every valid combination of L & R
			counter = 0;
			for (int L = 0; L <= 5; L++)	{
				for (int R = L; R <= 5; R++)	{
					vm.loadMemory(3000, new int[] {
						new MixInst(opcodes[r], 0, L, R, 2000).pack(),
						new MixInst(MixOpCode.HLT, 0, 0, 2, 0).pack()});
					vm.run(3000);
					if (!assertEquals(expectedValues[counter++], vm.getRegister(r)))	{
						System.out.println("r = " + r + ", L = " + L + ", R = " + R);
						System.exit(0);
					}
				}
			}
		}

		// Some specific examples from TAOCP 1.3.1
		/*
		vm.loadMemory(2000, new int[] {-80 << 24 | 3 << 12 | 5 << 6 | 4});
		assertValue(new MixInst(MixOpCode.LDA, 0, 0, 5, 2000), {-80 << 24 | 3 << 12 | 5 << 6 | 4});
		assertValue(new MixInst(MixOpCode.LDA, 0, 1, 5, 2000), {80 << 24 | 3 << 12 | 5 << 6 | 4});
		assertValue(new MixInst(MixOpCode.LDA, 0, 3, 5, 2000), {3 << 12 | 5 << 6 | 4});
		assertValue(new MixInst(MixOpCode.LDA, 0, 0, 3, 2000), {-80 << 12 | 3});
		assertValue(new MixInst(MixOpCode.LDA, 0, 4, 4, 2000), {5});
		assertValue(new MixInst(MixOpCode.LDA, 0, 0, 0, 2000), {-0});
		assertValue(new MixInst(MixOpCode.LDA, 0, 1, 1, 2000), {1});	// "unknown", but I know
		*/
	}

	public static void testStoring() throws Exception	{
		MixVM vm = new MixVM();
		vm.loadMemory(3000, new int[] {
				new MixInst(MixOpCode.INCA, 0, 0, 2, 1000).pack(), // ENTA
				new MixInst(MixOpCode.STA, 0, 0, 5, 2000).pack(),
				new MixInst(MixOpCode.HLT, 0, 0, 2, 0).pack()});
		vm.run(3000);
		vm.dumpState();
		vm.showMemory(2000,2000);

		// From TAOCP, p. 130
		/*
		vm.loadMemory(2000, new int[] {-1 << 24 | 2 << 18 | 3 << 12 | 4 << 6 | 5});
		vm.loadRegister(0, new int[] {6 << 24 | 7 << 18 | 8 << 12 | 9 << 6 | 0});
		assertMemory(new MixInst(MixOpCode.STA, 0, 0, 5, 2000), {6 << 24 | 7 << 18 | 8 << 12 | 9 << 6 | 0});
		assertMemory(new MixInst(MixOpCode.STA, 0, 1, 5, 2000), {-6 << 24 | 7 << 18 | 8 << 12 | 9 << 6 | 0});
		assertMemory(new MixInst(MixOpCode.STA, 0, 5, 5, 2000), {-1 << 24 | 2 << 18 | 3 << 12 | 4 << 6 | 0});
		assertMemory(new MixInst(MixOpCode.STA, 0, 2, 2, 2000), {-1 << 24 | 0 << 18 | 3 << 12 | 4 << 6 | 5});
		assertMemory(new MixInst(MixOpCode.STA, 0, 2, 3, 2000), {-1 << 24 | 9 << 18 | 0 << 12 | 4 << 6 | 5});
		assertMemory(new MixInst(MixOpCode.STA, 0, 0, 1, 2000), {0 << 24 | 2 << 18 | 3 << 12 | 4 << 6 | 5});
		*/
	}

	public static void testAdd()	{
		// p. 131
		// Compute the sum of the five bytes of register A
		// STA 2000
		// LDA 2000(5:5)
		// ADD 2000(4:4)
		// ADD 2000(3:3)
		// ADD 2000(2:2)
		// ADD 2000(1:1)
	}

	public static void testArithmetic()	{
		// p. 132
		// reg		0		1		2		3		4		5
		// rA     + 1234      1    150
		// 1000   +  100      5     50
		// ADD 1000
		// rA     + 1334      6    200

		// rA     - 1234      0   0   9
		// 1000   - 2000       150    0
		// SUB 1000
		// rA     +  766       149    ?

		// rA     +   1   1   1   1   1
		// 1000   +   1   1   1   1   1
		// MUL 1000
		// rA     +   0   1   2   3   4
		// rX     +   5   4   3   2   1

		// rA	    -               112
		// 1000   ?   2   ?   ?   ?   ?
		// MUL 1000(1:1)
		// rA     -                   0
		// rX     -                 224

		// rA     -  50   0    112    4
		// 1000   -   2   0   0   0   0
		// MUL 1000
		// rA     +   100     0    224
		// rX     +   8   0   0   0   0

		// rA     +                   0
		// rX     ?                  17
		// 1000   +                   3
		// DIV 1000
		// rA     +                   5
		// rX     +                   2

		// rA     -                   0
		// rX     +  1235     0   3   1
		// 1000   -   0   0   0   0   0
		// DIV 1000
		// rA     +   0    617    ?   ?
		// rX     -   0   0   0   ?   1
	}

	public static void testShifting()	{
		//           rA						 rX
		// 		       + 1 2 3 4 5    - 6 7 8 9 10
		// SRAX 1    + 0 1 2 3 4    - 5 6 7 8 9
		// SLA 2     + 2 3 4 0 0    - 5 6 7 8 9
		// SRC 4     + 6 7 8 9 2    - 3 4 0 0 5
		// SRA 2     + 0 0 6 7 8    - 3 4 0 0 5
		// SLC 501   + 0 6 7 8 3    - 4 0 0 5 0
	}

	public static void testConversions()	{
		//			   rA             	rX
		//         - 00 00 31 32 39 + 37 57 47 30 30
		// NUM 0   -       12977700 + 37 57 47 30 30
		// INCA 1  -       12977699 + 37 57 47 30 30
		// CHAR 0  - 30 30 31 32 39 + 37 37 36 39 39
	}

	public static void main(String[] args) throws Exception	{
		testLoading();
		testStoring();
	}
}
