package script.proxies;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import Terrestrial;
import geom.DiscreteRegion;
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

public class FauxTemplate_Terrestrial extends FauxTemplate implements ScriptConvertible<Terrestrial>, Nodeable {
	public static final String TERRESTRIALSTRING = "Terrestrial";
	private Terrestrial terrestrial;

	public FauxTemplate_Terrestrial(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, TERRESTRIALSTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_Terrestrial(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
		this.setTerrestrial(new Terrestrial(1));
	}

	// Nodeable and ScriptConvertible implementations
	@Override
	public Terrestrial convert(ScriptEnvironment env) {
		return this.getTerrestrial();
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing terrestrial faux template function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_Terrestrial template = (FauxTemplate_Terrestrial) rawTemplate;
		ScriptValue returning;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_Terrestrial) this.createObject(ref, template);
			}
			template.setTerrestrial(new Terrestrial(Conversions.getDouble(this.getEnvironment(), params.get(0))));
			assert Logs.closeNode();
			return template;
		} else if (name.equals("add")) {
			DiscreteRegion region = Conversions.getDiscreteRegion(this.getEnvironment(), params.get(0));
			assert Logs.addSnapNode("Adding discrete region to terrestrial", region);
			template.getTerrestrial().add(region);
			assert Logs.closeNode();
			return null;
		} else if (name.equals("getPath")) {
			returning = Conversions.wrapPath(this.getEnvironment(), template.getTerrestrial().getPath(this.getEnvironment(), Conversions.getScenario(this.getEnvironment(), params.get(0)), Conversions.getTemplate(params.get(1)), Conversions.getAsset(this.getEnvironment(), params.get(2)), Conversions.getPoint(this.getEnvironment(), params.get(3)), Conversions.getPoint(this.getEnvironment(), params.get(4))));
			assert Logs.closeNode();
			return returning;
		}
		returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	public Terrestrial getTerrestrial() {
		return this.terrestrial;
	}

	// addFauxFunction(name,ScriptValueType type,List<ScriptValue_Abstract>params,ScriptKeywordType permission,boolean isAbstract)
	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing terrestrial faux template");
		this.addConstructor(this.getType());
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.DOUBLE));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		fxnParams = new ArrayList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_DiscreteRegion.DISCRETEREGIONSTRING)));
		this.addFauxFunction("add", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams.clear();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Scenario.SCENARIOSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_MovementEvaluator.MOVEMENTEVALUATORSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Asset.ASSETSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING)));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING)));
		this.addFauxFunction("getPath", ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Path.PATHSTRING), fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_Terrestrial(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Terrestrial Faux Template");
		super.nodificate();
		assert Logs.closeNode();
	}

	public void setTerrestrial(Terrestrial terrestrial) {
		this.terrestrial = terrestrial;
	}
}
