package common;

public class Helpers
{
	public static int getDecimalPowerForDouble2IntegerConversion(final double value)
	{
		int decimalPower = 1;
		while (!isInteger(value * decimalPower)) {
			decimalPower *= 10;
		}
		return decimalPower;
	}

	public static boolean isInteger(final double value)
	{
		return (value % 1 == 0);
	}
}