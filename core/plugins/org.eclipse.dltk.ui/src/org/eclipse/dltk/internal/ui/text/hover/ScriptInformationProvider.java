/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.text.hover;

import org.eclipse.dltk.internal.ui.BrowserInformationControl;
import org.eclipse.dltk.internal.ui.text.HTMLTextPresenter;
import org.eclipse.dltk.internal.ui.text.ScriptWordFinder;
import org.eclipse.dltk.ui.text.hover.IScriptEditorTextHover;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

public class ScriptInformationProvider implements IInformationProvider,
		IInformationProviderExtension2 {

	protected IScriptEditorTextHover fImplementation;

	/**
	 * The presentation control creator.
	 */
	private IInformationControlCreator fPresenterControlCreator;

	public ScriptInformationProvider(IEditorPart editor) {

		fImplementation = new ScriptTypeHover();
		fImplementation.setEditor(editor);
	}

	public IRegion getSubject(ITextViewer textViewer, int offset) {

		if (textViewer != null)
			return ScriptWordFinder.findWord(textViewer.getDocument(), offset);

		return null;
	}

	public String getInformation(ITextViewer textViewer, IRegion subject) {
		if (fImplementation != null) {
			String s = fImplementation.getHoverInfo(textViewer, subject);
			if (s != null && s.trim().length() > 0) {
				return s;
			}
		}

		return null;
	}

	public IInformationControlCreator getInformationPresenterControlCreator() {
		if (fPresenterControlCreator == null) {
			fPresenterControlCreator = new AbstractReusableInformationControlCreator() {

				public IInformationControl doCreateInformationControl(
						Shell parent) {
					if (BrowserInformationControl.isAvailable(parent))
						return new BrowserInformationControl(parent,
								JFaceResources.DIALOG_FONT, true);
					else
						return new DefaultInformationControl(parent,
								new HTMLTextPresenter(false));
				}
			};
		}
		return fPresenterControlCreator;
	}
}
