/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.ui.text;

import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class HTMLUtils {

	private static RGB BG_COLOR_RGB = null;
	private static RGB FG_COLOR_RGB = new RGB(0, 0, 0);

	static {
		final Display display = Display.getDefault();
		if (display != null && !display.isDisposed()) {
			try {
				display.asyncExec(new Runnable() {
					/*
					 * @see java.lang.Runnable#run()
					 */
					public void run() {
						if (isDarkTheme()) {
							Color darkBGColor = PlatformUI.getWorkbench()
									.getThemeManager().getCurrentTheme()
									.getColorRegistry()
									.get("org.eclipse.ui.workbench.DARK_BACKGROUND");
							BG_COLOR_RGB = darkBGColor != null
									? darkBGColor.getRGB()
									: new RGB(31, 31, 31);
							Color darkFGColor = PlatformUI.getWorkbench()
									.getThemeManager().getCurrentTheme()
									.getColorRegistry()
									.get("org.eclipse.ui.workbench.DARK_FOREGROUND");
							FG_COLOR_RGB = darkFGColor != null
									? darkFGColor.getRGB()
									: new RGB(204, 204, 204);
							;
						} else {
						BG_COLOR_RGB = display.getSystemColor(
								SWT.COLOR_INFO_BACKGROUND).getRGB();
						FG_COLOR_RGB = display.getSystemColor(
								SWT.COLOR_INFO_FOREGROUND).getRGB();
						}
					}
				});
			} catch (SWTError err) {
				// see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=45294
				if (err.code != SWT.ERROR_DEVICE_DISPOSED)
					throw err;
			}
		}
	}

	public static RGB getBgColor() {
		if (BG_COLOR_RGB != null) {
			return BG_COLOR_RGB;
		} else {
			// RGB value of info bg color on WindowsXP
			return new RGB(255, 255, 225);
		}
	}

	/**
	 * @since 3.0
	 */
	public static RGB getFgColor() {
		return FG_COLOR_RGB;
	}

	public static boolean isDarkTheme() {
		boolean darkTheme = false;
		BundleContext ctx = DLTKUIPlugin.getDefault().getBundle()
				.getBundleContext();
		ServiceReference<IThemeManager> serviceReference = ctx
				.getServiceReference(IThemeManager.class);
		if (serviceReference != null) {
			IThemeManager manager = ctx.getService(serviceReference);
			if (manager != null) {
				IThemeEngine engine = manager
						.getEngineForDisplay(Display.getDefault());
				if (engine != null) {
					ITheme it = engine.getActiveTheme();
					if (it != null) {
						if (it.getId().toLowerCase().contains("dark")) {
							darkTheme = true;
						}
					}
				}
			}
		}
		return darkTheme;
	}
}
