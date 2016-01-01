public class ArrayCopyTest	{
	public static void main(String[] args)	{
		int x[] = new int[] {1, 2, 3, 0, 0, 0};
		System.arraycopy(x, 1, x, 0, 3);
		for (int i = 0; i < x.length; i++)	{
			System.out.print(x[i] + ", ");
		}
		System.out.println();
	}
}
