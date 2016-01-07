package explicit.conditional;

import java.util.BitSet;

import explicit.Model;
import explicit.ModelTransformation;

//FIXME ALG: add comment
public interface ReachabilityTransformation<OM extends Model,TM extends Model> extends ModelTransformation<OM, TM>
{
	public BitSet getGoalStates();
}
