package prism;

import java.util.BitSet;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

public class RewardCounterTransformationAdd extends ProbModelTransformation {
	private int bits;
	private int limit;
	private int maxRepresentable;
	private TransitionsByRewardsInfo info;
	private JDDNode statesOfInterest;
	// private PrismLog log = new PrismFileLog("stdout");
	private boolean msbFirst = true;
	
	/** Count rewards from [0,limit]. All values >= limit are encoded by limit */
	public RewardCounterTransformationAdd(ProbModel model,
	                                      TransitionsByRewardsInfo info,
	                                      int limit,
	                                      JDDNode statesOfInterest) {
		super(model);

		this.info = info;
		this.limit = limit;
		this.statesOfInterest = statesOfInterest;

		//log.println("Limit = "+limit);
		bits = (int) Math.ceil(PrismUtils.log2(limit+1));
		maxRepresentable = (1<<bits) - 1;
	}

	public void clear()
	{
		super.clear();
		info.clear();
		if (statesOfInterest != null)
			JDD.Deref(statesOfInterest);
	}

	private int bitIndex2Var(int i)
	{
		if (msbFirst) {
			return bits-i-1;
		} else {
			return i;
		}
	}

	@Override
	public int getExtraStateVariableCount() {
		return bits;
	}

	public int getLimit() {
		return limit;
	}
	
	@Override
	public JDDNode getTransformedTrans() throws PrismException {
		JDDNode newTrans = JDD.Constant(0);

		for (int rew : info.getOccuringRewards()) {
			JDDNode tr_rew = info.getTransitionsWithReward(rew);

			JDDNode tr_rew_with_counter = 
				JDD.Apply(JDD.TIMES, tr_rew,
				                     adder(extraRowVars, extraColVars, rew));

			// JDD.PrintMinterms(log, tr_rew_with_counter.copy(), "tr_rew_with_counter ("+rew+")");
			newTrans = JDD.Apply(JDD.MAX, newTrans, tr_rew_with_counter);
		}

		return newTrans;
	}

	@Override
	public JDDNode getTransformedStart() {
		JDDNode newStart = JDD.And(statesOfInterest.copy(),
		                           encodeInt(0, false));

		return newStart;
	}

	public JDDVars getExtraRowVars() {
		return extraRowVars;
	}
	
	public JDDNode saturated(boolean col) {
		int max = (1 << bits) - 1;
		//log.println("Max = "+max);
		JDDNode result = JDD.Constant(0);
		for (int i = limit; i <= max; i++) {
			JDDNode iDD = encodeInt(i, col);
			//JDD.PrintMinterms(log, iDD, "i="+i);
			result = JDD.Or(result, iDD);
		}
		return result;
	}

	public int decodeInt(BitSet bitset) {
		long[] v = bitset.toLongArray();
		if (v.length == 0) {
			return 0;
		} else if (v.length > 1  || v[0] > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Integer value out of range");
		}
		
		return (int)v[0];		
	}
	
	public JDDNode encodeInt(int value, boolean col) {
		if (value < 0)
			throw new IllegalArgumentException("Can not encode negative integer");

		JDDVars vars = col ? extraColVars : extraRowVars;
		BitSet vBits = BitSet.valueOf(new long[]{value});
		//log.println(vBits);
		
		if (value > maxRepresentable) {
			throw new IllegalArgumentException("Value "+value+" is out of range");
		}

		JDDNode result = JDD.Constant(1);
		for (int i=0; i < vars.n(); i++) {
			if (vBits.get(i))
				result = JDD.And(result, vars.getVar(bitIndex2Var(i)).copy());
			else
				result = JDD.And(result, JDD.Not(vars.getVar(bitIndex2Var(i)).copy()));
		}

		return result;
	}

	public JDDNode getStatesWithAccumulatedReward(int r) {
		if (r >= limit) {
			return encodeInt(limit, false);
		} else {
			return encodeInt(r, false);
		}
	}

	private JDDNode adder(JDDVars row, JDDVars col, int summand) throws PrismException {
		JDDNode result;

		if (summand < 0) {
			throw new IllegalArgumentException("Can not create adder for negative summand");
		}
		if (row.n() != col.n()) {
			throw new IllegalArgumentException("Can not create adder for different number of variables");
		}

		//log.println("Summand = "+summand+", bits = "+bits);
		
		if (summand >= limit) {
			// -> limit_next
			return encodeInt(limit, true);
		}

		// convert summand to BitSet
		BitSet summandBits = BitSet.valueOf(new long[]{summand});
		JDDNode nextValues = JDD.Constant(1);
		JDDNode carry = JDD.Constant(0.0);
		
		// for all the bits (0, ..., n-1)
		for (int i = 0; i < row.n(); i++) {
			// x = i-th bit in the row vector
			JDDNode x = row.getVar(bitIndex2Var(i)).copy();
			// y = i-th bit of the summand
			JDDNode y = summandBits.get(i) ? JDD.Constant(1.0) : JDD.Constant(0.0);

			JDDNode z = JDD.Xor(JDD.Xor(x.copy(), y.copy()), carry.copy());
			nextValues = JDD.And(nextValues, JDD.Equiv(col.getVar(bitIndex2Var(i)).copy(), z));
			carry = JDD.Or(JDD.Or(JDD.And(x.copy(), carry.copy()),
			                      JDD.And(y.copy(), carry)),
			                      JDD.And(x.copy(), y.copy()));
			JDD.Deref(x);
			JDD.Deref(y);
		}

		JDDNode saturated_now = saturated(false);
		//JDD.PrintMinterms(log, saturated_now.copy(), "saturated_now");
		JDDNode saturated_next = JDD.And(nextValues.copy(), saturated(true));
		//JDD.PrintMinterms(log, saturated_next.copy(), "saturated_next (1)");
		saturated_next = JDD.ThereExists(saturated_next, extraColVars);
		//JDD.PrintMinterms(log, saturated_next.copy(), "saturated_next (2)");
		saturated_next = JDD.Or(saturated_next, carry.copy());
		//JDD.PrintMinterms(log, saturated_next.copy(), "saturated_next (3)");
		JDDNode limit_next = encodeInt(limit, true);
		//JDD.PrintMinterms(log, limit_next.copy(), "limit_next");
		
		//JDD.PrintMinterms(qc.getLog(), negative_now, "negative_now");
		//JDD.PrintMinterms(qc.getLog(), negative_next, "negative_next");
		
		// result = saturated_now -> limit_next
		result = JDD.Implies(saturated_now.copy(), limit_next.copy());

		// result &= (!saturated_now & saturated_next) -> limit_next
		result = JDD.And(result,
		                 JDD.Implies(JDD.And(JDD.Not(saturated_now.copy()), saturated_next.copy()),
		                             limit_next.copy()));
		
		// result &= (!saturated_now & !satured_next) -> nextValues
		result = JDD.And(result,
                JDD.Implies(JDD.And(JDD.Not(saturated_now.copy()),
                                    JDD.Not(saturated_next.copy())),
                            nextValues.copy()));

		JDD.Deref(nextValues);
		JDD.Deref(carry);
		JDD.Deref(saturated_now);
		JDD.Deref(saturated_next);
		JDD.Deref(limit_next);
	
		//JDD.PrintMinterms(qc.getLog(), result.copy(), "adder for "+summand);
		
		//JDD.PrintMinterms(log, result, "adder("+summand+")");
		return result;
	}
}
