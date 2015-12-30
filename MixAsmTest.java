public class MixAsmTest	{
	/**
	 * Examples from TAOCP, 1.3.1
	 */
	public static void testAssembly()	{
		assertEquals(assemble("	LDA 2000,2(0:3)")); // +2000, 2, 3, 8);
		assertEquals(assemble("	LDA 2000,2(1:3)")); // +2000, 2, 11, 8);
		assertEquals(assemble("	LDA 2000(1:3)")); // +2000, 0, 3, 8);
		assertEquals(assemble("	LDA 2000")); // +2000, 0, 5, 8);
		assertEquals(assemble("	LDA -2000,4")); // -2000, 4, 5, 8);
	}
}
