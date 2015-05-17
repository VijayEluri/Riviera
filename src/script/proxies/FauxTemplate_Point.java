package script.proxies;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import geom.points.Point;
import geom.points.EuclideanPoint;
import inspect.Nodeable;
import logging.Logs;
import script.Conversions;
import script.ScriptConvertible;
import script.ScriptEnvironment;
import script.exceptions.ScriptException;
import script.parsing.Referenced;
import script.parsing.ScriptKeywordType;
import script.values.RiffScriptFunction;
import script.values.ScriptTemplate;
import script.values.ScriptTemplate_Abstract;
import script.values.ScriptValue;
import script.values.ScriptValueType;
import script.values.ScriptValue_Faux;

public class FauxTemplate_Point extends FauxTemplate implements ScriptConvertible<Point>, Nodeable {
	public static final String POINTSTRING = "Point";
	public Point point;

	public FauxTemplate_Point(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, POINTSTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_Point(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
		this.point = new EuclideanPoint(0, 0, 0);
	}

	public FauxTemplate_Point(ScriptEnvironment env, ScriptValueType type, ScriptValueType extended, List<ScriptValueType> implemented, boolean isAbstract) {
		super(env, type, extended, implemented, isAbstract);
	}

	// ScriptConvertible and Nodeable implementations
	@Override
	public Point convert(ScriptEnvironment env) {
		return this.point;
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing Point Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_Point template = (FauxTemplate_Point) rawTemplate;
		ScriptValue returning = null;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_Point) this.createObject(ref, template);
			}
			if (params.size() == 3) {
				template.getPoint().setX(Conversions.getDouble(this.getEnvironment(), params.get(0)).doubleValue());
				template.getPoint().setY(Conversions.getDouble(this.getEnvironment(), params.get(1)).doubleValue());
				template.getPoint().setZ(Conversions.getDouble(this.getEnvironment(), params.get(2)).doubleValue());
			}
			params.clear();
			returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		}
		if (name.equals("getX")) {
			returning = Conversions.wrapDouble(this.getEnvironment(), (template).getPoint().getX());
		} else if (name.equals("getY")) {
			returning = Conversions.wrapDouble(this.getEnvironment(), (template).getPoint().getY());
		} else if (name.equals("getZ")) {
			returning = Conversions.wrapDouble(this.getEnvironment(), (template).getPoint().getZ());
		} else if (name.equals("setX")) {
			(template).getPoint().setX(Conversions.getDouble(this.getEnvironment(), params.get(0)).doubleValue());
		} else if (name.equals("setY")) {
			(template).getPoint().setY(Conversions.getDouble(this.getEnvironment(), params.get(0)).doubleValue());
		} else if (name.equals("setZ")) {
			(template).getPoint().setZ(Conversions.getDouble(this.getEnvironment(), params.get(0)).doubleValue());
		} else {
			returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		}
		assert Logs.closeNode();
		return returning;
	}

	public Point getPoint() {
		return this.point;
	}

	// addFauxFunction(name,ScriptValueType type,List<ScriptValue_Abstract>params,ScriptKeywordType permission,boolean isAbstract)
	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing point faux template");
		this.addConstructor(this.getType());
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.DOUBLE));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.DOUBLE));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.DOUBLE));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		this.addFauxFunction("getX", ScriptValueType.DOUBLE, Collections.<ScriptValue> emptyList(), ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getY", ScriptValueType.DOUBLE, Collections.<ScriptValue> emptyList(), ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getZ", ScriptValueType.DOUBLE, Collections.<ScriptValue> emptyList(), ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.DOUBLE));
		this.addFauxFunction("setX", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("setY", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("setZ", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_Point(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Point Faux Template");
		super.nodificate();
		assert Logs.addNode(this.point);
		assert Logs.closeNode();
	}

	public void setPoint(Point point) {
		this.point = point;
	}
}
