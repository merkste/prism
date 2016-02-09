package quantile;

import java.util.BitSet;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.NondetModel;
import prism.NondetModelTransformation;
import prism.PrismException;
import prism.PrismUtils;

public class RewardCounterTransformationSubtract extends NondetModelTransformation {
	private int bits;
	private int limit;
	private QuantileCalculatorContext qc;
	private boolean msbFirst = true;
	private JDDNode statesOfInterest;
	
	public RewardCounterTransformationSubtract(NondetModel model,
	                                           QuantileCalculatorContext qc,
	                                           int limit,
	                                           JDDNode statesOfInterest) {
		super(model);

		this.qc = qc;
		this.limit = limit;
		this.statesOfInterest = statesOfInterest;

		// + one bit for negative
		bits = (int) Math.ceil(PrismUtils.log2(limit+1)) + 1;
	}
	
	@Override
	public void clear()
	{
		super.clear();
		if (statesOfInterest != null) {
			JDD.Deref(statesOfInterest);
		}
	}

	@Override
	public int getExtraStateVariableCount() {
		return bits;
	}

	@Override
	public int getExtraActionVariableCount() {
		return 0;
	}

	public int bitIndex2Var(int i)
	{
		if (msbFirst) {
			return bits-i-1;
		} else {
			return i;
		}
	}
	
	@Override
	public JDDNode getTransformedTrans() throws PrismException {
		JDDNode newTrans = JDD.Constant(0);

		for (int rew : qc.getOccuringRewards()) {
			JDDNode tr_rew = qc.getTransitionsWithReward(rew);

			// TODO: Directly go to negative if rew > limit
			
			JDDNode tr_rew_with_counter = 
				JDD.Apply(JDD.TIMES, tr_rew,
				                     subtractor(extraRowVars, extraColVars, rew));

			newTrans = JDD.Apply(JDD.MAX, newTrans, tr_rew_with_counter);
		}

		return newTrans;
	}

	@Override
	public JDDNode getTransformedStart() {
		JDDNode newStart = JDD.And(statesOfInterest.copy(),
		                           JDD.Not(negative_bit(false)));
		
		return newStart;
	}

	public JDDVars getExtraRowVars() {
		return extraRowVars;
	}
	
	public JDDNode negative_bit(boolean col)
	{
		JDDVars vars = col ? extraColVars : extraRowVars;

		JDDNode negative = vars.getVar(bitIndex2Var(vars.n()-1)).copy();
		return negative;
	}
	
	public JDDNode negative(boolean col) {
		JDDVars vars = col ? extraColVars : extraRowVars;

		JDDNode negative = negative_bit(col);
		for (int i=0; i < vars.n()-1; i++) {
			negative = JDD.And(negative, JDD.Not(vars.getVar(bitIndex2Var(i)).copy()));
		}
		return negative;
	}

	public JDDNode zeroOrNegative(boolean col) {
		JDDVars vars = col ? extraColVars : extraRowVars;

		JDDNode result = JDD.Not(negative_bit(col));
		for (int i=0; i < vars.n()-1; i++) {
			result = JDD.And(result, JDD.Not(vars.getVar(bitIndex2Var(i)).copy()));
		}
		result = JDD.Or(result, negative(col));
		return result;
	}
	
	public Integer getSmallestCounter(JDDNode values) throws PrismException
	{
		if (values.equals(JDD.ZERO)) {
			return null;
		}

//		JDD.PrintMinterms(qc.getLog(), values, extraRowVars, "values");

		
		JDDVars vars = extraRowVars;
		JDDNode cur = values;

		BitSet result = new BitSet();

		for (int i = vars.n()-2; i>=0; i--) {
			JDDNode zero = JDD.And(JDD.Not(vars.getVar(bitIndex2Var(i)).copy()), cur.copy());
			if (!zero.equals(JDD.ZERO)) {
				// there is a counter where v_i = 0
				result.set(i, false);
				JDD.Deref(cur);
				cur = zero;
			} else {
				// there is only a counter where v_i = 1
				JDD.Deref(zero);
				result.set(i, true);
				cur = JDD.And(vars.getVar(bitIndex2Var(i)).copy(), cur);
			}
		}
		
		JDD.Deref(cur);

		return decodeInt(result);
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

		if (value > limit) {
			throw new IllegalArgumentException("Value "+value+" is out of range");
		}

		JDDNode result = JDD.Constant(1);
		for (int i=0; i < vars.n()-1; i++) {
			if (vBits.get(i))
				result = JDD.And(result, vars.getVar(bitIndex2Var(i)).copy());
			else
				result = JDD.And(result, JDD.Not(vars.getVar(bitIndex2Var(i)).copy()));
		}

		// result &= !negative
		result = JDD.And(result, JDD.Not(negative_bit(false)));

		return result;
	}

	
	private JDDNode subtractor(JDDVars row, JDDVars col, int subtrahend) throws PrismException {
		JDDNode result;

		if (subtrahend < 0) {
			throw new IllegalArgumentException("Can not create subtractor for negative subtrahend");
		}
		if (row.n() != col.n()) {
			throw new IllegalArgumentException("Can not create subtractor for different number of variables");
		}

		if (subtrahend > limit) {
			// -> negative'
			return negative(true);
		}
		
		// convert subtrahend to BitSet
		BitSet subtrahendBits = BitSet.valueOf(new long[]{subtrahend});
		JDDNode nextValues = JDD.Constant(1);
		JDDNode borrow = JDD.Constant(0.0);
		
		// for all the regular bits (0, ..., n-2)
		for (int i = 0; i < row.n()-1; i++) {
			// x = i-th bit in the row vector
			JDDNode x = row.getVar(bitIndex2Var(i)).copy();
			// y = i-th bit of the subtrahend
			JDDNode y = subtrahendBits.get(i) ? JDD.Constant(1.0) : JDD.Constant(0.0);

			JDDNode z = JDD.Xor(JDD.Xor(x.copy(), y.copy()), borrow.copy());
			nextValues = JDD.And(nextValues, JDD.Equiv(col.getVar(bitIndex2Var(i)).copy(), z));
			borrow = JDD.Or(JDD.And(JDD.Not(x.copy()),
			                        JDD.Or(y.copy(), borrow.copy())),
			                JDD.And(x,
			                        JDD.And(y, borrow)
			                       )
			               );
		}

		// JDD.PrintMinterms(qc.getLog(),  nextValues.copy(), "next values for subtrahend "+subtrahend);
		
		JDDNode negative_now = negative(false);
		JDDNode negative_next = negative(true);
		JDDNode negative_now_bit = negative_bit(false);
		JDDNode negative_next_bit = negative_bit(true);

		// JDD.PrintMinterms(qc.getLog(), negative_now.copy(), "negative_now");
		// JDD.PrintMinterms(qc.getLog(), negative_next.copy(), "negative_next");
		
		// result = negative_bit -> negative'
		result = JDD.Implies(negative_now_bit.copy(), negative_next.copy());

		// result &= (!negative_bit & borrow) -> negative'
		result = JDD.And(result,
		                 JDD.Implies(JDD.And(JDD.Not(negative_now_bit.copy()), borrow.copy()),
		                             negative_next.copy()));

		// result &= (!negative & !borrow) -> nextValues & !negative_bit'
		result = JDD.And(result,
                JDD.Implies(JDD.And(JDD.Not(negative_now_bit.copy()), JDD.Not(borrow.copy())),
                            JDD.And(nextValues.copy(), JDD.Not(negative_next_bit.copy()))));

		JDD.Deref(nextValues);
		JDD.Deref(borrow);
		JDD.Deref(negative_now);
		JDD.Deref(negative_next);
		JDD.Deref(negative_now_bit);
		JDD.Deref(negative_next_bit);
	
		// JDD.PrintMinterms(qc.getLog(), result.copy(), "subtractor for "+subtrahend);
		
		return result;
	}
}
