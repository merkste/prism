package jltl2ba;

public class TLProperties
{
	private boolean syntacticSafety;
	private boolean syntacticGuarantee;
	private boolean syntacticObligation;
	private boolean syntacticRecurrence;
	private boolean syntacticPersistence;

	public TLProperties()
	{
		syntacticSafety = true;
		syntacticGuarantee = true;
		syntacticObligation = true;
		syntacticRecurrence = true;
		syntacticPersistence = true;
	}

	public TLProperties(TLProperties prop)
	{
		syntacticSafety = prop.syntacticSafety;
		syntacticGuarantee = prop.syntacticGuarantee;
		syntacticObligation = prop.syntacticObligation;
		syntacticRecurrence = prop.syntacticRecurrence;
		syntacticPersistence = prop.syntacticPersistence;
	}

	public boolean isSyntacticSafety()
	{
		return syntacticSafety;
	}

	/**
	 * @return the syntacticGuarantee
	 */
	public boolean isSyntacticGuarantee()
	{
		return syntacticGuarantee;
	}

	/**
	 * @return the syntacticObligation
	 */
	public boolean isSyntacticObligation()
	{
		return syntacticObligation;
	}

	/**
	 * @return the syntacticRecurrence
	 */
	public boolean isSyntacticRecurrence()
	{
		return syntacticRecurrence;
	}

	/**
	 * @return the syntacticPersistence
	 */
	public boolean isSyntacticPersistence()
	{
		return syntacticPersistence;
	}

	public String toString()
	{
		String s = "[";
		boolean first = true;

		if (isSyntacticSafety()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic safety";
		}

		if (isSyntacticGuarantee()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic guarantee";
		}

		if (isSyntacticObligation()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic obligation";
		}

		if (isSyntacticRecurrence()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic recurrence";
		}

		if (isSyntacticPersistence()) {
			if (!first) s+=",";
			first = false;
			s+="syntactic persistence";
		}

		return s+"]";
	}
	
	public TLProperties and(TLProperties other)
	{
		TLProperties result = new TLProperties(this);

		result.syntacticSafety &= other.syntacticSafety;
		result.syntacticGuarantee &= other.syntacticGuarantee;
		result.syntacticObligation &= other.syntacticObligation;
		result.syntacticRecurrence &= other.syntacticRecurrence;
		result.syntacticPersistence &= other.syntacticPersistence;

		return result;
	}

	public TLProperties or(TLProperties other)
	{
		TLProperties result = new TLProperties(this);

		result.syntacticSafety &= other.syntacticSafety;
		result.syntacticGuarantee &= other.syntacticGuarantee;
		result.syntacticObligation &= other.syntacticObligation;
		result.syntacticRecurrence &= other.syntacticRecurrence;
		result.syntacticPersistence &= other.syntacticPersistence;

		return result;
	}

	public TLProperties not()
	{
		TLProperties result = new TLProperties();

		// take opposites
		result.syntacticSafety = this.syntacticGuarantee;
		result.syntacticGuarantee = this.syntacticSafety;

		// obligation stays the same
		result.syntacticObligation = this.syntacticObligation;

		// take opposites
		result.syntacticRecurrence = this.syntacticPersistence;
		result.syntacticPersistence = this.syntacticRecurrence;

		return result;
	}

	public TLProperties nextstep()
	{
		return new TLProperties(this);  // just a copy
	}

	public TLProperties eventually()
	{
		TLProperties result = new TLProperties(this);  // copy

		result.syntacticSafety = false;
		// syntacticGuarantee inherited
		result.syntacticObligation = syntacticGuarantee;
		result.syntacticRecurrence = syntacticGuarantee;
		// syntacticPersistence inherited

		return result;
	}

	public TLProperties always()
	{
		TLProperties result = new TLProperties(this);  // copy

		// syntacticSafety inherited
		result.syntacticGuarantee = false;
		result.syntacticObligation = syntacticSafety;
		// syntacticRecurrence inherited
		result.syntacticPersistence = syntacticSafety;

		return result;
	}
	
	public TLProperties until(TLProperties other)
	{
		TLProperties result = new TLProperties();
		
		result.syntacticSafety = false;
		result.syntacticGuarantee = this.syntacticGuarantee & other.syntacticGuarantee;
		result.syntacticObligation = this.syntacticObligation & other.syntacticGuarantee;
		result.syntacticRecurrence = this.syntacticRecurrence & other.syntacticGuarantee;
		result.syntacticPersistence = this.syntacticPersistence & other.syntacticPersistence;

		return result;
	}

	public TLProperties release(TLProperties other)
	{
		TLProperties result = new TLProperties();

		result.syntacticSafety = this.syntacticSafety & other.syntacticSafety;
		result.syntacticGuarantee = false;
		result.syntacticObligation = this.syntacticObligation & other.syntacticSafety;
		result.syntacticRecurrence = this.syntacticRecurrence & other.syntacticRecurrence;
		result.syntacticPersistence = this.syntacticPersistence & other.syntacticSafety;

		return result;
	}

	public TLProperties equiv(TLProperties other)
	{
		TLProperties left = and(other);
		TLProperties right = not().and(other.not());

		return left.or(right);
	}

	public TLProperties implies(TLProperties other)
	{
		TLProperties result = not().or(other);
		return result;
	}

	public static TLProperties analyse(SimpleLTL root)
	{
		switch (root.kind) {
		case TRUE:
		case FALSE:
			return new TLProperties(); // all true
		case AP:
			return new TLProperties(); // all true
		case NOT:
			return analyse(root.left).not();

		case AND:
			return analyse(root.left).and(analyse(root.right));
		case OR:
			return analyse(root.left).or(analyse(root.right));
			
		case FINALLY:
			return analyse(root.left).eventually();
		case GLOBALLY:
			return analyse(root.left).always();
		case NEXT:
			return analyse(root.left).nextstep();
		case UNTIL:
			return analyse(root.left).until(analyse(root.right));
		case RELEASE:
			return analyse(root.left).release(analyse(root.right));

		case EQUIV:
			return analyse(root.left).equiv(analyse(root.right));
		case IMPLIES:
			return analyse(root.left).implies(analyse(root.right));
		}
		throw new UnsupportedOperationException();
	}
}
