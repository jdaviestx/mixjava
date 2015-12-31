class FieldError extends MixException	{
	private String msg;

	public FieldError(String msg)	{
		super(msg);
		this.msg = msg;
	}
}
