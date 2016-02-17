package common;

import prism.PrismException;

public class SafeCast
{
	public static int toInt(double d) throws PrismException
	{
		if (Double.isInfinite(d) || Double.isNaN(d)) {
			throw new PrismException("Value "+d+" is not an integer");
		}

		int i = (int) d;
		if ((double)i != d) {
			throw new PrismException("Can not safely convert value "+d+" to an integer");
		}
		return i;
	}
}