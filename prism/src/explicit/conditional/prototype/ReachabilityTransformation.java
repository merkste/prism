package explicit.conditional.prototype;

import java.util.BitSet;

import explicit.Model;
import explicit.ModelTransformation;

public interface ReachabilityTransformation<OM extends Model,TM extends Model> extends ModelTransformation<OM, TM>
{
	public BitSet getGoalStates();
}
