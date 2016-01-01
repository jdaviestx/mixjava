package com.jdavies.mix;

/**
*   3         2         1
 * 10987654321098765432109876543210
 * 10000000000000000000000000000000
 */
public class Masks	{
	public static void main(String[] args)	{
		System.out.println("L\tR\tmask");
		for (int L = 1; L < 6; L++)	{
			for (int R = L; R < 6; R++)	{
				int width = (R - L + 1) * 6;
				int left_bit = (5 - L + 1) * 6;
				int right_bit = (5 - R) * 6;

				int mask = (~(0x0) << right_bit) & ~(~(0x0) << left_bit);

				System.out.print(L + "\t" + R + "\t");
				System.out.print(left_bit + "\t" + right_bit + "\t");
				for (int mmask = (0x01 << 31); mmask != 0; mmask >>>= 1)	{
					if ((mask & mmask) != 0)	{
						System.out.print("1");
					} else	{
						System.out.print("0");
					}
				}
				System.out.println();
			}
		}
	}
}
