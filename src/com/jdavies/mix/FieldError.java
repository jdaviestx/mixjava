package com.jdavies.mix;

class FieldError extends MixException	{
	private String msg;

	public FieldError(String msg)	{
		super(msg);
		this.msg = msg;
	}
}
