package script.proxies;

import java.util.LinkedList;
import java.util.List;

import geom.points.Point;
import geom.points.PointPath;
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

public class FauxTemplate_Path extends FauxTemplate_Point implements ScriptConvertible<Point>, Nodeable {
	public static final String PATHSTRING = "Path";

	public FauxTemplate_Path(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, PATHSTRING), ScriptValueType.createType(env, FauxTemplate_Point.POINTSTRING), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_Path(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
	}

	// Nodeable and ScriptConvertible interfaces
	@Override
	public Point convert(ScriptEnvironment env) {
		return this.getPoint();
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing Path Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_Path template = (FauxTemplate_Path) rawTemplate;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_Path) this.createObject(ref, template);
			}
			if (params.size() == 1) {
				((PointPath) template.getPoint()).setScenario(Conversions.getScenario(this.getEnvironment(), params.get(0)));
			}
			params.clear();
		} else if (name.equals("getTotalTime")) {
			ScriptValue returning = Conversions.wrapLong(this.getEnvironment(), ((PointPath) template.getPoint()).getTotalTime());
			assert Logs.closeNode();
			return returning;
		}
		ScriptValue returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing path faux template");
		this.addConstructor(this.getType());
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Scenario.SCENARIOSTRING)));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		this.addFauxFunction("getTotalTime", ScriptValueType.LONG, new LinkedList<ScriptValue>(), ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_Path(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Path Faux Script-Element");
		super.nodificate();
		assert Logs.closeNode();
	}
}
