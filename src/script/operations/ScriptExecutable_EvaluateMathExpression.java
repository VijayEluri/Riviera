package script.operations;

import inspect.Nodeable;
import logging.Logs;
import script.exceptions.InternalException;
import script.exceptions.DivisionByZeroScriptException;
import script.exceptions.ScriptException;
import script.parsing.Referenced;
import script.parsing.ScriptElement;
import script.parsing.ScriptOperatorType;
import script.values.ScriptValue;
import script.values.ScriptValueType;
import script.values.ScriptValue_Numeric;

public class ScriptExecutable_EvaluateMathExpression extends ScriptElement implements ScriptValue, ScriptExecutable, Nodeable {
	private ScriptValue lhs, rhs;
	private final ScriptOperatorType operator;
	private ScriptValueType type;

	public ScriptExecutable_EvaluateMathExpression(Referenced ref, ScriptValue lhs, ScriptValue rhs, ScriptOperatorType expressionType) {
		super(ref);
		this.type = ScriptValueType.createType(lhs);
		this.lhs = lhs;
		this.rhs = rhs;
		this.operator = expressionType;
		switch (this.operator) {
		case PLUS:
		case MINUS:
		case MODULUS:
		case MULTIPLY:
		case DIVIDE:
			break;
		default:
			throw new IllegalArgumentException("Operator must be a arithmetic operator");
		}
	}

	@Override
	public ScriptValue castToType(Referenced ref, ScriptValueType type) throws ScriptException {
		return this.getValue().castToType(ref, type);
	}

	// ScriptExecutable implementation
	@Override
	public ScriptValue execute() throws ScriptException {
		return this.getValue();
	}

	// ScriptValue_Abstract implementation
	@Override
	public ScriptValueType getType() {
		return this.type;
	}

	@Override
	public ScriptValue getValue() throws ScriptException {
		assert Logs.openNode("Mathematic Expressions", "Executing Mathematic Expression");
		assert Logs.addNode(this);
		ScriptValue_Numeric left = (ScriptValue_Numeric) this.lhs.getValue();
		ScriptValue_Numeric right = (ScriptValue_Numeric) this.rhs.getValue();
		if ((this.operator == ScriptOperatorType.DIVIDE || this.operator == ScriptOperatorType.MODULUS) && right.getNumericValue().doubleValue() == 0.0d) {
			throw new DivisionByZeroScriptException(this);
		}
		ScriptValue returning = null;
		switch (this.operator) {
		case PLUS:
			returning = new ScriptValue_Numeric(this.getEnvironment(), left.increment(this, right));
			break;
		case MINUS:
			returning = new ScriptValue_Numeric(this.getEnvironment(), left.decrement(this, right));
			break;
		case MODULUS:
			returning = new ScriptValue_Numeric(this.getEnvironment(), left.modulus(this, right));
			break;
		case MULTIPLY:
			returning = new ScriptValue_Numeric(this.getEnvironment(), left.multiply(this, right));
			break;
		case DIVIDE:
			returning = new ScriptValue_Numeric(this.getEnvironment(), left.divide(this, right));
			break;
		default:
			throw new AssertionError("Unexpected default");
		}
		assert Logs.closeNode();
		return returning;
	}

	@Override
	public boolean isConvertibleTo(ScriptValueType type) {
		return this.lhs.isConvertibleTo(type);
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Mathematical Expression Evaluator (" + this.operator + ")");
		assert Logs.addNode("Left", this.lhs);
		assert Logs.addNode("Right", this.rhs);
		assert Logs.closeNode();
	}

	@Override
	public ScriptValue setValue(Referenced ref, ScriptValue value) throws ScriptException {
		throw new InternalException(this, "Unexecuted Variable");
	}

	@Override
	public int valuesCompare(Referenced ref, ScriptValue rhs) throws ScriptException {
		return this.getValue().valuesCompare(ref, rhs.castToType(ref, this.getType()));
	}

	@Override
	public boolean valuesEqual(Referenced ref, ScriptValue rhs) throws ScriptException {
		return this.getValue().valuesEqual(ref, rhs.castToType(ref, this.getType()));
	}
}
