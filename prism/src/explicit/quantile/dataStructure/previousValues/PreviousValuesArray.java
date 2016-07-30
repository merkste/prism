package explicit.quantile.dataStructure.previousValues;
/**
 * Use a 2-dimensional array in order to store the previously calculated values.
 * @author Marcus Daum (mdaum@tcs.inf.tu-dresden.de)
 */
public abstract class PreviousValuesArray implements PreviousValues
{
	protected double[][] previousValues;
}