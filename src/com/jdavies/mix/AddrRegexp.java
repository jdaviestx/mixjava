package com.jdavies.mix;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AddrRegexp	{
	public static void main(String[] args)	{
		String inputs[] = new String[] {
			"2000",
			"2000,2",
			"2000,2(1:5)",
			"2000(3:4)"
		};
		Pattern p = Pattern.compile("(\\d*)(?,(\\d*))?(?\\((\\d*):(\\d*)\\))?");

		for (String input : inputs)	{
			Matcher m = p.matcher(input);
			for (int i = 0; i < m.groupCount(); i++)	{
				System.out.println(m.group(i));
			}
		}
	}
}
