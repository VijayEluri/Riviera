package script.proxies;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import gui.style.Stylesheets;
import inspect.Nodeable;
import logging.Logs;
import script.Conversions;
import script.ScriptConvertible;
import script.ScriptEnvironment;
import script.exceptions.InvalidColorRangeException;
import script.exceptions.ScriptException;
import script.parsing.Referenced;
import script.parsing.ScriptKeywordType;
import script.values.RiffScriptFunction;
import script.values.ScriptTemplate;
import script.values.ScriptTemplate_Abstract;
import script.values.ScriptValue;
import script.values.ScriptValueType;
import script.values.ScriptValue_Faux;

public class FauxTemplate_Color extends FauxTemplate implements ScriptConvertible<Color>, Nodeable {
	public static final String COLORSTRING = "Color";
	private Color color;

	public FauxTemplate_Color(ScriptEnvironment env) {
		super(env, ScriptValueType.createType(env, COLORSTRING), ScriptValueType.getObjectType(env), new LinkedList<ScriptValueType>(), false);
	}

	public FauxTemplate_Color(ScriptEnvironment env, ScriptValueType type) {
		super(env, type);
		this.color = Color.BLACK;
	}

	// Nodeable and ScriptConvertible interfaces
	@Override
	public Color convert(ScriptEnvironment env) {
		return this.color;
	}

	// Function bodies are contained via a series of if statements in execute
	// Template will be null if the object is exactly of this type and is constructing, and thus must be created then
	@Override
	public ScriptValue execute(Referenced ref, String name, List<ScriptValue> params, ScriptTemplate_Abstract rawTemplate) throws ScriptException {
		assert Logs.openNode("Faux Template Executions", "Executing color faux template function (" + RiffScriptFunction.getDisplayableFunctionName(name) + ")");
		FauxTemplate_Color template = (FauxTemplate_Color) rawTemplate;
		ScriptValue returning;
		assert Logs.addSnapNode("Template provided", template);
		assert Logs.addSnapNode("Parameters provided", params);
		if (name == null || name.equals("")) {
			if (template == null) {
				template = (FauxTemplate_Color) this.createObject(ref, template);
			}
			if (params.size() == 1) {
				template.setColor(Stylesheets.getColor(Conversions.getString(this.getEnvironment(), params.get(0))));
			} else if (params.size() == 3) {
				if (params.get(0).isConvertibleTo(ScriptValueType.INT)) {
					int r, g, b;
					r = Conversions.getInteger(this.getEnvironment(), params.get(0));
					g = Conversions.getInteger(this.getEnvironment(), params.get(1));
					b = Conversions.getInteger(this.getEnvironment(), params.get(2));
					if (r < 0 || r > 255) {
						throw new InvalidColorRangeException(this, new Integer(r));
					}
					if (g < 0 || g > 255) {
						throw new InvalidColorRangeException(this, new Integer(g));
					}
					if (b < 0 || b > 255) {
						throw new InvalidColorRangeException(this, new Integer(b));
					}
					template.setColor(new java.awt.Color(r, g, b));
				} else {
					;
					float r, g, b;
					r = Conversions.getFloat(this.getEnvironment(), params.get(0));
					g = Conversions.getFloat(this.getEnvironment(), params.get(1));
					b = Conversions.getFloat(this.getEnvironment(), params.get(2));
					if (r < 0.0d || r > 1.0d) {
						throw new InvalidColorRangeException(this, new Float(r));
					}
					if (g < 0.0d || g > 1.0d) {
						throw new InvalidColorRangeException(this, new Float(g));
					}
					if (b < 0.0d || b > 1.0d) {
						throw new InvalidColorRangeException(this, new Float(b));
					}
					template.setColor(new java.awt.Color(r, g, b));
				}
			}
			params.clear();
		} else if (name.equals("getRed")) {
			returning = Conversions.wrapInt(this.getEnvironment(), template.getColor().getRed());
			assert Logs.closeNode();
			return returning;
		} else if (name.equals("getGreen")) {
			returning = Conversions.wrapInt(this.getEnvironment(), template.getColor().getGreen());
			assert Logs.closeNode();
			return returning;
		} else if (name.equals("getBlue")) {
			returning = Conversions.wrapInt(this.getEnvironment(), template.getColor().getBlue());
			assert Logs.closeNode();
			return returning;
		} else if (name.equals("setRed")) {
			int value = 0;
			if (params.get(0).getType().equals(ScriptValueType.FLOAT)) {
				value = (int) (Conversions.getFloat(this.getEnvironment(), params.get(0)) * 255.0d);
			} else {
				value = Conversions.getInteger(this.getEnvironment(), params.get(0));
			}
			template.setColor(new java.awt.Color(value, template.getColor().getGreen(), template.getColor().getBlue()));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("setGreen")) {
			int value = 0;
			if (params.get(0).getType().equals(ScriptValueType.FLOAT)) {
				value = (int) (Conversions.getFloat(this.getEnvironment(), params.get(0)) * 255.0d);
			} else {
				value = Conversions.getInteger(this.getEnvironment(), params.get(0));
			}
			template.setColor(new java.awt.Color(template.getColor().getRed(), value, template.getColor().getBlue()));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("setBlue")) {
			int value = 0;
			if (params.get(0).getType().equals(ScriptValueType.FLOAT)) {
				value = (int) (Conversions.getFloat(this.getEnvironment(), params.get(0)) * 255.0d);
			} else {
				value = Conversions.getInteger(this.getEnvironment(), params.get(0));
			}
			template.setColor(new java.awt.Color(template.getColor().getRed(), template.getColor().getGreen(), value));
			assert Logs.closeNode();
			return null;
		} else if (name.equals("setColor")) {
			template.setColor(Stylesheets.getColor(Conversions.getString(this.getEnvironment(), params.get(0))));
			assert Logs.closeNode();
			return null;
		}
		returning = this.getExtendedFauxClass().execute(ref, name, params, template);
		assert Logs.closeNode();
		return returning;
	}

	public java.awt.Color getColor() {
		return this.color;
	}

	// All functions must be defined here. All function bodies are defined in 'execute'.
	@Override
	public void initialize() throws ScriptException {
		assert Logs.openNode("Faux Template Initializations", "Initializing color faux template");
		this.addConstructor(this.getType());
		List<ScriptValue> fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		this.addConstructor(this.getType(), fxnParams);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.INT));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.INT));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.INT));
		this.addConstructor(this.getType(), fxnParams);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.FLOAT));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.FLOAT));
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.FLOAT));
		this.addConstructor(this.getType(), fxnParams);
		this.disableFullCreation();
		this.getExtendedClass().initialize();
		fxnParams = new LinkedList<ScriptValue>();
		this.addFauxFunction("getRed", ScriptValueType.INT, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getGreen", ScriptValueType.INT, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getBlue", ScriptValueType.INT, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getRedOpacity", ScriptValueType.FLOAT, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getGreenOpacity", ScriptValueType.FLOAT, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("getBlueOpacity", ScriptValueType.FLOAT, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.INT));
		this.addFauxFunction("setRed", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("setGreen", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("setBlue", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.FLOAT));
		this.addFauxFunction("setRed", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("setGreen", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		this.addFauxFunction("setBlue", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		fxnParams = new LinkedList<ScriptValue>();
		fxnParams.add(new ScriptValue_Faux(this.getEnvironment(), ScriptValueType.STRING));
		this.addFauxFunction("setColor", ScriptValueType.VOID, fxnParams, ScriptKeywordType.PUBLIC, false, false);
		assert Logs.closeNode();
	}

	// Define default constructor here
	@Override
	public ScriptTemplate instantiateTemplate() {
		return new FauxTemplate_Color(this.getEnvironment(), this.getType());
	}

	@Override
	public void nodificate() {
		assert Logs.openNode("Color Faux Template");
		super.nodificate();
		if (this.color == null) {
			assert Logs.addNode("Color: null");
		} else {
			assert Logs.addNode("Color: " + Stylesheets.getColorName(this.color));
		}
		assert Logs.closeNode();
	}

	public void setColor(java.awt.Color color) {
		this.color = color;
	}
}
