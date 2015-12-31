package com.jdavies.mix;

class MemoryLocationError extends MixException	{
	private String msg;

	public MemoryLocationError(String msg)	{
		super(msg);
		this.msg = msg;
	}
}
