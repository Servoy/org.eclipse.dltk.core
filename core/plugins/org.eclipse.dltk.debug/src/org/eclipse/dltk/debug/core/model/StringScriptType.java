package org.eclipse.dltk.debug.core.model;

public class StringScriptType extends AtomicScriptType {
	public StringScriptType(String name) {
		super(name);
	}

	public boolean isString() {
		return true;
	}

	public String formatValue(IScriptValue value) {
		String string = value.getRawValue();

		if (string == null) {
			return null;
		}
		return '"' + string + '"';
	}
}
