class MixException extends Exception	{
	private int location;

	public MixException(String msg)	{
		super(msg);
	}

	public void attachLocation(int location)	{
		this.location = location;
	}

	public int getLocation()	{
		return location;
	}
}
