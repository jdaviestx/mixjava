package com.jdavies.mix;

class IllegalInstruction extends MixException	{
	private String msg;

	public IllegalInstruction(String msg)	{
		super(msg);
		this.msg = msg;
	}
}
