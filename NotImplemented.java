package com.jdavies.mix;

class NotImplemented extends MixException	{
	public NotImplemented(int code)	{
		super("Valid, but unimplemented op-code " + code);
	}
}
