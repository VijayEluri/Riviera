package script.proxies;

import java.util.LinkedList;
import java.util.List;

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
import asset.Ace;
import asset.Asset;

public class FauxTemplate_Asset extends FauxTemplate implements ScriptConvertible<Asset>, Nodeable {
	public static final String ASSETSTRING = "Asset";
	private Asset asset;

	public FauxTemplate_Asset(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, ASSETSTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_Asset(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
		this.asset = new Asset();
	}

	// Nodeable and ScriptConvertible interfaces
	@Override
	public Asset convert(ScriptEnvironment env) {
		return this.asset;
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing Asset Faux Template Function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		try {
			FauxTemplate_Asset template = (FauxTemplate_Asset) rawTemplate;
			assert Logs.addSnapNode("Template provided", template);
			assert Logs.addSnapNode("Parameters provided", params);
			if (name == null || name.equals("")) {
				if (template == null) {
					template = (FauxTemplate_Asset) this.createObject(ref, template);
				}
				template.getAsset().setLocation(Conversions.getPoint(this.getEnvironment(), params.get(0)));
				params.clear();
			} else if (name.equals("setProperty")) {
				if (params.size() == 2) {
					template.getAsset().setProperty(Conversions.getString(this.getEnvironment(), params.get(0)), params.get(1).getValue());
					return null;
				}
			} else if (name.equals("getProperty")) {
				if (params.size() == 1) {
					Object property = template.getAsset().getProperty(Conversions.getString(this.getEnvironment(), params.get(0)));
					if (property instanceof ScriptConvertible<?>) {
						return (ScriptValue) ((ScriptConvertible<?>) property).convert(this.getEnvironment());
					}
					return (ScriptValue) property;
				}
			} else if (name.equals("addAce")) {
				template.getAsset().addAce(Conversions.getAce(this.getEnvironment(), params.get(0)));
				return null;
			} else if (name.equals("getAces")) {
				List<ScriptValue> list = new LinkedList<ScriptValue>();
				for (Ace ace : template.getAsset().getAces()) {
					list.add(Conversions.wrapAce(this.getEnvironment(), ace));
				}
				return Conversions.wrapList(this.getEnvironment(), list);
			} else if (name.equals("getLocation")) {
				assert template.getAsset().getLocation() != null : "Asset location is null!";
				return Conversions.wrapPoint(this.getEnvironment(), template.getAsset().getLocation());
			} else if (name.equals("setLocation")) {
				template.getAsset().setLocation(Conversions.getPoint(this.getEnvironment(), params.get(0)));
				return null;
			}
			return this.getExtendedFauxClass().execute(ref, name, params, template);
		} finally {
			assert Logs.closeNode();
		}
	}

	public Asset getAsset() {
		return this.asset;
	}

	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing asset faux template");
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING)));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.getObjectType(this.getEnvironment())));
		this.addFauxFunction("setProperty", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		this.addFauxFunction("getProperty", ScriptValueType.getObjectType(this.getEnvironment()), fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Ace.ACESTRING)));
		this.addFauxFunction("addAce", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		this.addFauxFunction("getAces", ScriptValueType.createType(this.getEnvironment(), FauxTemplate_List.LISTSTRING), fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		this.addFauxFunction("getLocation", ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING), fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.createType(this.getEnvironment(), FauxTemplate_Point.POINTSTRING)));
		this.addFauxFunction("setLocation", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_Asset(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Asset Faux Script-Element");
		super.nodificate();
		assert Logs.addNode(this.asset);
		assert Logs.closeNode();
	}

	public void setAsset(Asset asset) {
		this.asset = asset;
	}
}
