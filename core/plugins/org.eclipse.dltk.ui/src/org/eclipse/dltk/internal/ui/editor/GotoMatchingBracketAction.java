/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;

public class GotoMatchingBracketAction extends Action {

	public final static String GOTO_MATCHING_BRACKET = "GotoMatchingBracket"; //$NON-NLS-1$

	private final ScriptEditor fEditor;

	public GotoMatchingBracketAction(ScriptEditor editor) {
		super(DLTKEditorMessages.GotoMatchingBracket_label);
		Assert.isNotNull(editor);
		fEditor = editor;
		setEnabled(true);
		// PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
		// IJavaHelpContextIds.GOTO_MATCHING_BRACKET_ACTION);
	}

	@Override
	public void run() {
		fEditor.gotoMatchingBracket();
	}
}
