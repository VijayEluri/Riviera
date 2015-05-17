package script.proxies;

import java.util.LinkedList;
import java.util.List;

import ArchetypeMapNode;
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

public class FauxTemplate_DiscreteRegion extends FauxTemplate_GraphicalElement implements ScriptConvertible<DiscreteRegion>, Nodeable {
	public static final String DISCRETEREGIONSTRING = "DiscreteRegion";
	private DiscreteRegion region;

	public FauxTemplate_DiscreteRegion(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, DISCRETEREGIONSTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), false);
		assert env != null;
	}

	public FauxTemplate_DiscreteRegion(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
	}

	// Nodeable and ScriptConvertible interfaces
	@Override
	public DiscreteRegion convert(ScriptEnvironment env) {
		return this.region;
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing Discrete Region Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_DiscreteRegion template = (FauxTemplate_DiscreteRegion) rawTemplate;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_DiscreteRegion) this.createObject(ref, template);
			}
			template.setRegion(this.region = new DiscreteRegion(this.getEnvironment()));
			params.clear();
		} else if (name.equals("add")) {
			//region.addPoint(Parser.getPoint(aaron is a sand jewparams.get(0)));
			if (params.size() == 1) {
				template.getRegion().addPoint(Conversions.getPoint(this.getEnvironment(), params.get(0)));
				assert Logs.closeNode();
				return null;
			}
		} else if (name.equals("addAsset")) {
			if (template.getRegion().getProperty("Archetypes") == null) {
				template.getRegion().setProperty("Archetypes", ArchetypeMapNode.createTree(Conversions.getAsset(this.getEnvironment(), params.get(0))));
			} else {
				((ArchetypeMapNode) template.getRegion().getProperty("Archetypes")).addAsset(Conversions.getAsset(this.getEnvironment(), params.get(0)));
			}
			assert Logs.closeNode();
			return null;
		} else if (name.equals("setProperty")) {
			if (params.size() == 2) {
				template.getRegion().setProperty(Conversions.getString(this.getEnvironment(), params.get(0)), Conversions.getObject(this.getEnvironment(), params.get(1)));
				assert Logs.closeNode();
				return null;
			}
		} else if (name.equals("getProperty")) {
			if (params.size() == 1) {
				ScriptValue returning = (ScriptValue) Conversions.convert(this.getEnvironment(), template.getRegion().getProperty(Conversions.getString(this.getEnvironment(), params.get(0))));
				assert Logs.addSnapNode("Retrieved property", returning);
				assert Logs.closeNode();
				return returning;
			}
		} else if (name.equals("getCenter")) {
			ScriptValue returning = Conversions.wrapPoint(this.getEnvironment(), template.getRegion().getCenter());
			assert Logs.closeNode();
			return returning;
		}
		ScriptValue returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	public DiscreteRegion getRegion() {
		return this.region;
	}

	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing discrete region faux template");
		this.addConstructor(this.getType());
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), this.getType()));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING)));
		this.addFauxFunction("add", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Asset.ASSETSTRING)));
		this.addFauxFunction("addAsset", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.getObjectType(this.getEnvironment())));
		this.addFauxFunction("setProperty", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		this.addFauxFunction("getProperty", ScriptValueType.getObjectType(this.getEnvironment()), fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		this.addFauxFunction("getCenter", ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING), fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_DiscreteRegion(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Discrete Region Faux Script-Element");
		super.nodificate();
		assert Logs.addNode(this.region);
		assert Logs.closeNode();
	}

	public void setRegion(DiscreteRegion region) {
		this.region = region;
	}
}
