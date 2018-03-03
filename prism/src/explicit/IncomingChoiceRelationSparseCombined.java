package explicit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;

import common.IterableBitSet;
import common.StopWatch;
import common.functions.ObjIntFunction;
import common.functions.ObjIntIntFunction;
import common.functions.primitive.IntIntConsumer;
import common.functions.primitive.IntTriOperator;
import common.iterable.ArrayIterator;
import common.iterable.EmptyIterator;
import common.iterable.FunctionalIterator;
import common.iterable.FunctionalPrimitiveIterator;
import common.iterable.Interval;
import common.iterable.IterableArray;
import common.iterable.FunctionalPrimitiveIterable.IterableInt;
import common.iterable.FunctionalPrimitiveIterator.OfInt;
import explicit.IncomingChoiceRelation.Choice;
import prism.PrismComponent;

/**
 * A class for storing and accessing the incoming choices of an explicit NondetModel.
 * This class can be seen as providing more detailed information than PredecessorRelation,
 * as that class only stores information about the states and not the choices linking them. 
 * <p>
 * As NondetModel only provide easy access to successors of states,
 * the predecessor relation is computed and stored for subsequent efficient access.
 * <p>
 * Note: Naturally, if the NondetModel changes, the predecessor relation
 * has to be recomputed to remain accurate.
 */
public class IncomingChoiceRelationSparseCombined extends PredecessorRelationSparse
{
	protected BitSet   choices;
	protected int      maxNumChoices;
//	protected Interval choices;
//	protected BitSet[] choiceSets;

	// FIXME ALG: Build during model build

	/**
	 * Constructor. Computes the predecessor relation for the given model
	 * by considering the successors of each state.
	 *
	 * @param model the Model
	 */
	@SuppressWarnings("unchecked")
	public IncomingChoiceRelationSparseCombined(NondetModel model)
	{
		int numStates         = model.getNumStates();
		int countPredecessors = 0;
//		int maxNumChoices     = model.getMaxNumChoices();
		maxNumChoices         = model.getMaxNumChoices();

		ArrayList<Choice>[] pre = new ArrayList[numStates];
		for (int state = 0; state < numStates; state++) {
			int numChoices = model.getNumChoices(state);
			for (int choice = 0; choice < numChoices; choice++) {
				Choice newChoice        = new Choice(state, choice);

				for (Iterator<Integer> it = model.getSuccessorsIterator(state, choice); it.hasNext();) {
					int successor = it.next();

					// Add the current choice (s,c) to pre[successor].
					ArrayList<Choice> choices = pre[successor];
					if (choices == null) {
						pre[successor] = choices = new ArrayList<Choice>(5);
						countPredecessors++;
					} else if (choices.get(choices.size() - 1).state != state) {
						countPredecessors++;
					}
					choices.add(newChoice);
				}
			}
		}

		// copy predecessors to sparse array
		offsets      = new int[numStates + 1];
		predecessors = new int[countPredecessors];
		choices      = new BitSet(countPredecessors * maxNumChoices);
		for (int state = 0, offset = 0; state < numStates; state++) {
			offsets[state] = offset;
			if (pre[state] == null) {
				continue;
			}

			int index = offset-1; // current write index
			for (Choice choice : pre[state]) {
				// dedupe and store predecessors
				int p = choice.getState();
				if (index < offset || predecessors[index] != p) {
					// new predecessor of current state
					index++;
					predecessors[index] = p;
				}
				// store choice
				int c = choice.getChoice();
				choices.set(index * maxNumChoices + c);
			}
			offset     = index + 1;
			pre[state] = null;
		}
		offsets[numStates] = countPredecessors;

//		// copy predecessors to sparse array
//		offsets      = new int[numStates + 1];
//		predecessors = new int[countPredecessors];
//		choices      = new Interval(maxNumChoices);
//		int numBits  = countPredecessors; // compiler does not recognize countPredecessors as effectively final 
//		choiceSets   = choices.map((int c) -> new BitSet(numBits)).collect(new BitSet[maxNumChoices]);
//		for (int state = 0, offset = 0; state < numStates; state++) {
//			offsets[state] = offset;
//			if (pre[state] == null) {
//				continue;
//			}
//			int index = offset-1; // current write index
//			for (Choice choice : pre[state]) {
//				// dedupe and store predecessors
//				int p = choice.getState();
//				if (index < offset || predecessors[index] != p) {
//					// new predecessor of current state
//					index++;
//					predecessors[index] = p;
//				}
//				// store choice
//				int c = choice.getChoice();
//				choiceSets[c].set(index);
//			}
//			offset     = index + 1;
//			pre[state] = null;
//		}
//		offsets[numStates] = countPredecessors;
	}

	/**
	 * Get an Iterable over the incoming choices of state {@code s}.
	 */
	public Iterable<Choice> getIncomingChoices(int s)
	{
		return () -> getIncomingChoicesIterator(s);
	}

	public ChoiceIterator getIncomingChoicesIterator(int s)
	{
		// FIXME ALG: try internal vs external iteration (needs IntIntPredicate, IntIntConsumer)
		// FIMXE ALG: try using SetBitsIterator
		// FIXME ALG: try building explicit list/array/stuff
//		Interval indices = new Interval(offsets[s], offsets[s+1]);
//		return preIndices.flatMap((int i) -> {int p = predecessors[i]; return choices.filter((int c) -> choiceSets[c].get(i)).map((int c) -> new Choice(p, c));});
//		int[]  pre = predecessors;
//		int    max = maxNumChoices;
//		BitSet cho = choices;
//		return preIndices.flatMap((int i) -> {int p = pre[i]; int low = i * max; int high = low + max - 1; return new IterableBitSet(cho, low, high).map((int c) -> new Choice(p, c - low));});
		return new ChoiceIterator(offsets[s], offsets[s+1], predecessors, choices, maxNumChoices);
	}


	public static class ChoiceIteratorMultipleIterators implements FunctionalIterator<Choice>
	{
		final FunctionalPrimitiveIterator.OfInt indices;
		final int[] sparseStates;
		final BitSet sparseChoices;
		final int window;

		FunctionalPrimitiveIterator.OfInt choices;
		int offset;
		int state;

		public ChoiceIteratorMultipleIterators(int fromIndex, int toIndex, int[] sparseStates, BitSet sparseChoices, int window)
		{
			this.sparseStates  = sparseStates;
			this.sparseChoices = sparseChoices;
			this.window        = window;
			this.indices       = new Interval.IntervalIterator(fromIndex, toIndex, 1);
			this.choices       = indices.hasNext() ? nextState(indices.nextInt()) : EmptyIterator.OfInt();
		}

		@Override
		public boolean hasNext()
		{
			if (indices.hasNext() || choices.hasNext()) {
				return true;
			}
			release();
			return false;
		}

		@Override
		public Choice next() {
			if (choices.hasNext()) {
				return new Choice(state, choices.nextInt() - offset);
			}
			requireNext();
			choices = nextState(indices.nextInt());
			return new Choice(state, choices.next() - offset);
		}

		public void forEachRemaining(IntIntConsumer action)
		{
			Objects.requireNonNull(action);
			// FIXME ALG: try (but mind mutable state issues)
			IntConsumer wrapped = (int c) -> action.accept(state, c - offset);
			choices.forEachRemaining(wrapped);
//			indices.forEachRemaining((int i) -> nextState(i).forEachRemaining(wrapped));
//--------------------------------------------------------------------------------
			while (indices.hasNext()) {
				choices = nextState(indices.nextInt());
				choices.forEachRemaining(wrapped);
			}
//--------------------------------------------------------------------------------
//			while (choices.hasNext()) {
//				action.accept(state, choices.nextInt() - offset);
//			}
//			while (indices.hasNext()) {
//				choices = nextState(indices.nextInt());
//				while (choices.hasNext()) {
//					action.accept(state, choices.nextInt() - offset);
//				}
//			}
		}

		public <T> T reduce(T identity, ObjIntIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			T result = identity;
			// FIXME ALG: try (but mind mutable state issues)
			ObjIntFunction<T, T> wrapped = (T r, int c) -> accumulator.apply(r, state, c - offset);
			result = choices.reduce(result, wrapped);
//			result = indices.reduce(result, (T r, int i) -> nextState(i).reduce(r, wrapped));
//--------------------------------------------------------------------------------
			while (indices.hasNext()) {
				choices = nextState(indices.nextInt());
				result  = choices.reduce(result, wrapped);
			}
//--------------------------------------------------------------------------------
//			while (choices.hasNext()) {
//				result = accumulator.apply(result, state, choices.nextInt() - offset);
//			}
//			while (indices.hasNext()) {
//				choices = nextState(indices.nextInt());
//				while (choices.hasNext()) {
//					result = accumulator.apply(result, state, choices.nextInt() - offset);
//				}
//			}
			release();
			return result;
		}

		public int reduce(int identity, IntTriOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			int result = identity;
			// FIXME ALG: try (but mind mutable state issues)
			IntBinaryOperator wrapped = (int r, int c) -> accumulator.applyAsInt(r, state, c - offset);
//			result = indices.reduce(result, (int r, int i) -> nextState(i).reduce(r, wrapped));
//--------------------------------------------------------------------------------
			result = choices.reduce(result, wrapped);
			while (indices.hasNext()) {
				choices = nextState(indices.nextInt());
				result  = choices.reduce(result, wrapped);
			}
//--------------------------------------------------------------------------------
//			while (choices.hasNext()) {
//				result = accumulator.applyAsInt(result, state, choices.nextInt() - offset);
//			}
//			while (indices.hasNext()) {
//				choices = nextState(indices.nextInt());
//				while (choices.hasNext()) {
//					result = accumulator.applyAsInt(result, state, choices.nextInt() - offset);
//				}
//			}
			release();
			return result;
		}

		protected FunctionalPrimitiveIterator.OfInt nextState(int i)
		{
			state   = sparseStates[i];
			offset  = i * window;
			return new IterableBitSet(sparseChoices, offset, offset + window - 1).iterator();
		}

		protected void release()
		{
			choices = EmptyIterator.OfInt();
		}

		protected void requireNext()
		{
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
		}
	}



	public static class ChoiceIterator implements FunctionalIterator<Choice>
	{
		final FunctionalPrimitiveIterator.OfInt indices;
		final FunctionalPrimitiveIterator.OfInt choices;
		final int[] sparseStates;
		final BitSet sparseChoices;
		final int window;

		int offset;
		int state;
		int choice;

		public ChoiceIterator(int fromIndex, int toIndex, int[] sparseStates, BitSet sparseChoices, int window)
		{
			this.sparseStates  = sparseStates;
			this.sparseChoices = sparseChoices;
			this.window        = window;
			this.indices       = new Interval.IntervalIterator(fromIndex, toIndex, 1);
			if (indices.hasNext()) {
				state   = nextState();
				choices = new IterableBitSet(sparseChoices, offset, toIndex * window - 1).iterator();
				choice  = choices.nextInt() - offset;
				assert 0 <= choice && choice < window;
			} else {
				choices = EmptyIterator.OfInt();
				state   = -1;
				choice  = -1;
			}
		}

		@Override
		public boolean hasNext()
		{
			if (choice >= 0) {
				// in choice window
				return true;
			}
			release();
			return false;
		}

		@Override
		public Choice next() {
			requireNext();
			if (choice >= window) {
				// advance state
				state   = nextState();
				// shift choice
				choice -= window;
			}
			assert 0 <= choice && choice < window : "valid choice index";
			Choice next = new Choice(state, choice);
			// advance choice only
			choice      = choices.hasNext() ? choices.nextInt() - offset : -1;
			return next;
		}

		public void forEachRemaining(IntIntConsumer action)
		{
			Objects.requireNonNull(action);
			if (!hasNext()) {
				return;
			}
			if (choice >= window) {
				// advance state
				state   = nextState();
				// shift choice
				choice -= window;
			}
			assert 0 <= choice && choice < window : "valid choice index";
			// accept current choice
			action.accept(state, choice);
			// accept remaining choice
			// FIXME ALG: variant that updates variable state
//			IntConsumer wrapped = (int c) -> {c -= offset; if (c >= window) {state = nextState(); c -= window;}; action.accept(state, c);};
//			choices.forEachRemaining(wrapped);
			// FIXME ALG: variant that does not update state variable
			IntBinaryOperator wrapped = (int s, int c) -> {c -= offset; if (c >= window) {s = nextState(); c -= window;}; action.accept(s, c); return s;};
			choices.reduce(state, wrapped);
//--------------------------------------------------------------------------------
//			while (hasNext()) {
//				Choice c = next();
//				action.accept(c.state, c.choice);
//			}
			release();
		}

		public <T> T reduce(T identity, ObjIntIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext()) {
				return identity;
			}
			if (choice >= window) {
				// advance state
				state   = nextState();
				// shift choice
				choice -= window;
			}
			assert 0 <= choice && choice < window : "valid choice index";
			// accept current choice
			T result = accumulator.apply(identity, state, choice);
			// accept remaining choice
			ObjIntFunction<T, T> wrapped = (T r, int c) -> {c -= offset; if (c >= window) {state = nextState(); c -= window;}; return accumulator.apply(r, state, c);};
			result = choices.reduce(result, wrapped);
//--------------------------------------------------------------------------------
//			while (hasNext()) {
//				Choice c = next();
//				result = accumulator.apply(result, c.state, c.choice);
//			}
			release();
			return result;
		}

		public int reduce(int identity, IntTriOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			if (!hasNext()) {
				return identity;
			}
			if (choice >= window) {
				// advance state
				state   = nextState();
				// shift choice
				choice -= window;
			}
			assert 0 <= choice && choice < window : "valid choice index";
			// accept current choice
			int result = accumulator.applyAsInt(identity, state, choice);
			// accept remaining choice
			IntBinaryOperator wrapped = (int r, int c) -> {c -= offset; if (c >= window) {state = nextState(); c -= window;}; return accumulator.applyAsInt(r, state, c);};
			result = choices.reduce(result, wrapped);
//--------------------------------------------------------------------------------
//			while (hasNext()) {
//				Choice c = next();
//				result = accumulator.applyAsInt(result, c.state, c.choice);
//			}
			release();
			return result;
		}

		protected int nextState()
		{
			int i  = indices.nextInt();
			offset = i * window;
			return sparseStates[i];
		}

		protected void release()
		{
			state   = -1;
			choice  = -1;
		}

		protected void requireNext()
		{
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
		}
	}



	/**
	 * Factory method to compute the incoming choices information for the given model.
	 * Logs diagnostic information to the log of the given PrismComponent.
	 *
	 * @param parent a PrismComponent (for obtaining the log and settings)
	 * @param model the non-deterministic model for which the predecessor relation should be computed
	 * @returns the incoming choices information
	 **/
	public static IncomingChoiceRelationSparseCombined forModel(PrismComponent parent, NondetModel model)
	{
		parent.getLog().print("Calculating incoming choice relation (SPARSE, COMBINED, SINGLE BITSET) for "+model.getModelType().fullName()+"...  ");
		parent.getLog().flush();

		StopWatch watch = new StopWatch().start();
		IncomingChoiceRelationSparseCombined pre = new IncomingChoiceRelationSparseCombined(model);

		parent.getLog().println("done (" + watch.elapsedSeconds() + " seconds)");

		return pre;
	}

	/**
	 * Get an Iterable over the predecessor states of {@code s}.
	 */
	@Override
	public IterableInt getPre(int s)
	{
		return new IterableArray.OfInt(predecessors, offsets[s], offsets[s+1]);
	}

	/**
	 * Get an Iterator over the predecessor states of {@code s}.
	 */
	@Override
	public OfInt getPreIterator(int s)
	{
		return new ArrayIterator.OfInt(predecessors, offsets[s], offsets[s+1]);
	}

	/**
	 * Computes the set Pre*(target) via a DFS, i.e., all states that
	 * are in {@code target} or can reach {@code target} via one or more transitions
	 * from states contained in {@code remain} and via the enabled choices in {@code enabledChoices}.
	 * <br/>
	 * If the parameter {@code remain} is {@code null}, then
	 * {@code remain} is considered to include all states in the model.
	 * <br/>
	 * If the parameter {@code enabledChoices} is {@code null}, then
	 * {@code enabledChoices} is considered to include all choices in the model.
	 * <br/>
	 * If the parameter {@code absorbing} is not {@code null},
	 * then the states in {@code absorbing} are considered to be absorbing,
	 * i.e., to have a single self-loop, disregarding other outgoing edges.

	 * @param remain restriction on the states that may occur
	 *               on the path to target, {@code null} = all states
	 * @param target The set of target states
	 * @param absorbing (optional) set of states that should be considered to be absorbing,
	 *               i.e., their outgoing edges are ignored, {@code null} = no states
	 * @param enabledChoices a mask providing information which choices are considered to be enabled
	 * @return the set of states Pre*(target)
	 */
//	public BitSet calculatePreStar(BitSet remain, BitSet target, BitSet absorbing, ChoicesMask enabledChoices)
//	{
//		if (enabledChoices == null) {
//			return calculatePreStar(remain, target, absorbing);
//		}
//
//		int cardinality = target.cardinality();
//		// all target states are in Pre*
//		BitSet result = (BitSet) target.clone();
//		// the stack of states whose predecessors have to be considered
//		Deque<Integer> todo = new ArrayDeque<Integer>(cardinality);
//		new IterableBitSet(target).collect(todo);
//		BitSet todo = (BitSet) target.clone();
//		// the set of states that are finished
//		BitSet done = new BitSet(cardinality);
//
//		while (!todo.isEmpty()) {
//			int s = todo.pop()
//			// already considered?
//			if (done.get(s)) {
//				continue;
//			}
//			done.set(s);
//
//			// for each predecessor in the graph
//			for (Choice choice : getIncomingChoices(s)) {
//				// check that choice is actually enabled
//				if (!enabledChoices.isEnabled(choice.getState(), choice.getChoice())) {
//					continue;
//				}
//
//				int p = choice.getState();
//				if (absorbing != null && absorbing.get(p)) {
//					// predecessor is absorbing, thus the edge is considered to not exist
//					continue;
//				}
//
//				if (remain == null || remain.get(p)) {
//					// can reach result (and is in remain)
//					result.set(p);
//					if (!done.get(p)) {
//						// add to stack
//						todo.push(p);
//					}
//				}
//			}
//		}
//		return result;
//	}

	public BitSet calculatePreStar(BitSet remain, BitSet target, BitSet absorbing, ChoicesMask enabledChoices)
	{
		if (enabledChoices == null) {
			return calculatePreStar(remain, target, absorbing);
		}

		int cardinality = target.cardinality();
		// all target states are in Pre*
		BitSet preStar = (BitSet) target.clone();
		// STACK of states whose predecessors have to be considered
		Deque<Integer> todo = new ArrayDeque<Integer>(cardinality);
		new IterableBitSet(target).collect(todo);
		// the set of states that are finished
		BitSet done = new BitSet(cardinality);

		// FIXME ALG: test use filter for enabled choices, absorbing
		IntIntConsumer addToPreStar = new IntIntConsumer()
		{
			@Override
			public void accept(int p, int c)
			{
				// check that choice is actually enabled
				if (!enabledChoices.isEnabled(p, c)) {
					return;
				}
				if (absorbing != null && absorbing.get(p)) {
					// predecessor is absorbing, thus the edge is considered to not exist
					return;
				}
				if (remain == null || remain.get(p)) {
					// can reach result (and is in remain)
					preStar.set(p);
					if (!done.get(p)) {
						// add to stack
						todo.push(p);
					}
				}
			}
		};

		while (!todo.isEmpty()) {
			int s = todo.pop();
			// already considered?
			if (done.get(s)) {
				continue;
			}
			done.set(s);

			getIncomingChoicesIterator(s).forEachRemaining(addToPreStar);
		}
		return preStar;
	}
}
