/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.wizards;

import static org.eclipse.dltk.core.IScriptProjectFilenames.BUILDPATH_FILENAME;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelStatus;
import org.eclipse.dltk.core.IScriptLanguageProvider;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.internal.core.BuildpathEntry;
import org.eclipse.dltk.internal.corext.util.Messages;
import org.eclipse.dltk.internal.ui.dialogs.StatusUtil;
import org.eclipse.dltk.internal.ui.util.CoreUtility;
import org.eclipse.dltk.internal.ui.wizards.NewWizardMessages;
import org.eclipse.dltk.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.dltk.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.dltk.internal.ui.wizards.buildpath.BPListElement;
import org.eclipse.dltk.internal.ui.wizards.buildpath.BPListLabelProvider;
import org.eclipse.dltk.internal.ui.wizards.buildpath.BuildPathBasePage;
import org.eclipse.dltk.internal.ui.wizards.buildpath.BuildpathOrderingWorkbookPage;
import org.eclipse.dltk.internal.ui.wizards.buildpath.FolderSelectionDialog;
import org.eclipse.dltk.internal.ui.wizards.buildpath.LibrariesWorkbookPage;
import org.eclipse.dltk.internal.ui.wizards.buildpath.ProjectsWorkbookPage;
import org.eclipse.dltk.internal.ui.wizards.buildpath.SourceContainerWorkbookPage;
import org.eclipse.dltk.internal.ui.wizards.buildpath.newsourcepage.NewSourceContainerWorkbookPage;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.DLTKUILanguageManager;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.IDLTKUILanguageToolkit;
import org.eclipse.dltk.ui.PreferenceConstants;
import org.eclipse.dltk.ui.dialogs.StatusInfo;
import org.eclipse.dltk.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.dltk.ui.util.IStatusChangeListener;
import org.eclipse.dltk.ui.viewsupport.ImageDisposer;
import org.eclipse.dltk.utils.ResourceUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class BuildpathsBlock {
	public static interface IRemoveOldBinariesQuery {
		/**
		 * Do the callback. Returns <code>true</code> if .class files should be
		 * removed from the old output location.
		 * 
		 * @param oldOutputLocation
		 *            The old output location
		 * @return Returns true if .class files should be removed.
		 * @throws OperationCanceledException
		 */
		boolean doQuery(IPath oldOutputLocation)
				throws OperationCanceledException;
	}

	protected IWorkspaceRoot fWorkspaceRoot;
	protected CheckedListDialogField<BPListElement> fBuildPathList;
	protected StringButtonDialogField fBuildPathDialogField;
	protected StatusInfo fPathStatus;
	protected StatusInfo fBuildPathStatus;
	protected IScriptProject fCurrScriptProject;
	protected final IStatusChangeListener fContext;
	protected Control fSWTWidget;
	protected TabFolder fTabFolder;
	protected int fPageIndex;
	protected BuildPathBasePage fSourceContainerPage;
	protected ProjectsWorkbookPage fProjectsPage;
	protected LibrariesWorkbookPage fLibrariesPage;
	protected BuildPathBasePage fCurrPage;
	protected String fUserSettingsTimeStamp;
	protected long fFileTimeStamp;
	protected IRunnableContext fRunnableContext;
	protected boolean fUseNewPage;

	// null when invoked from a non-property page context
	protected final IWorkbenchPreferenceContainer fPageContainer;

	/**
	 * @since 2.0
	 */
	public BuildpathsBlock(IStatusChangeListener context, int pageToShow,
			boolean useNewPage, IWorkbenchPreferenceContainer pageContainer) {
		this(new BusyIndicatorRunnableContext(), context, pageToShow,
				useNewPage, pageContainer);
	}

	public BuildpathsBlock(IRunnableContext runnableContext,
			IStatusChangeListener context, int pageToShow, boolean useNewPage,
			IWorkbenchPreferenceContainer pageContainer) {
		fPageContainer = pageContainer;
		fWorkspaceRoot = DLTKUIPlugin.getWorkspace().getRoot();
		fContext = context;
		if (!(fContext instanceof IScriptLanguageProvider)) {
			DLTKUIPlugin.log(new Exception("context should implement " //$NON-NLS-1$
					+ IScriptLanguageProvider.class.getSimpleName()));
		}
		fUseNewPage = useNewPage;
		fPageIndex = pageToShow;
		fSourceContainerPage = null;
		fLibrariesPage = null;
		fProjectsPage = null;
		fCurrPage = null;
		fRunnableContext = runnableContext;
		BuildPathAdapter adapter = new BuildPathAdapter();
		String[] buttonLabels = new String[] {
				NewWizardMessages.BuildPathsBlock_buildpath_up_button,
				NewWizardMessages.BuildPathsBlock_buildpath_down_button,
				/* 2 */null,
				NewWizardMessages.BuildPathsBlock_buildpath_checkall_button,
				NewWizardMessages.BuildPathsBlock_buildpath_uncheckall_button };
		fBuildPathList = new CheckedListDialogField<BPListElement>(null,
				buttonLabels, new BPListLabelProvider());
		fBuildPathList.setDialogFieldListener(adapter);
		fBuildPathList
				.setLabelText(NewWizardMessages.BuildPathsBlock_buildpath_label);
		fBuildPathList.setUpButtonIndex(0);
		fBuildPathList.setDownButtonIndex(1);
		fBuildPathList.setCheckAllButtonIndex(3);
		fBuildPathList.setUncheckAllButtonIndex(4);
		fBuildPathDialogField = new StringButtonDialogField(adapter);
		fBuildPathDialogField
				.setButtonLabel(NewWizardMessages.BuildPathsBlock_buildpath_button);
		fBuildPathDialogField.setDialogFieldListener(adapter);
		fBuildPathDialogField
				.setLabelText(NewWizardMessages.BuildPathsBlock_buildpath_label);
		fBuildPathStatus = new StatusInfo();
		fPathStatus = new StatusInfo();
		fCurrScriptProject = null;
	}

	protected boolean supportZips() {
		final IDLTKLanguageToolkit toolkit = getLanguageToolkit();
		return toolkit != null && toolkit.languageSupportZIPBuildpath();
	}

	// -------- UI creation ---------
	public Control createControl(Composite parent) {
		fSWTWidget = parent;
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		composite.setLayout(layout);
		TabFolder folder = new TabFolder(composite, SWT.NONE);
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		folder.setFont(composite.getFont());
		TabItem item;
		item = new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.BuildPathsBlock_tab_source);
		item.setImage(DLTKPluginImages
				.get(DLTKPluginImages.IMG_OBJS_PACKFRAG_ROOT));
		if (fUseNewPage) {
			fSourceContainerPage = new NewSourceContainerWorkbookPage(
					fBuildPathList, fRunnableContext, getPreferenceStore());
		} else {
			fSourceContainerPage = new SourceContainerWorkbookPage(
					fBuildPathList);
		}
		item.setData(fSourceContainerPage);
		item.setControl(fSourceContainerPage.getControl(folder));
		IWorkbench workbench = DLTKUIPlugin.getDefault().getWorkbench();
		Image projectImage = workbench.getSharedImages().getImage(
				IDE.SharedImages.IMG_OBJ_PROJECT);
		fProjectsPage = new ProjectsWorkbookPage(fBuildPathList, fPageContainer);
		item = new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.BuildPathsBlock_tab_projects);
		item.setImage(projectImage);
		item.setData(fProjectsPage);
		item.setControl(fProjectsPage.getControl(folder));
		fLibrariesPage = new LibrariesWorkbookPage(this.supportZips(),
				fBuildPathList, fPageContainer);
		item = new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.BuildPathsBlock_tab_libraries);
		item.setImage(DLTKPluginImages.get(DLTKPluginImages.IMG_OBJS_LIBRARY));
		item.setData(fLibrariesPage);
		item.setControl(fLibrariesPage.getControl(folder));
		// a non shared image
		Image cpoImage = DLTKPluginImages.DESC_TOOL_BUILDPATH_ORDER
				.createImage();
		composite.addDisposeListener(new ImageDisposer(cpoImage));
		BuildpathOrderingWorkbookPage ordpage = new BuildpathOrderingWorkbookPage(
				fBuildPathList);
		item = new TabItem(folder, SWT.NONE);
		item.setText(NewWizardMessages.BuildPathsBlock_tab_order);
		item.setImage(cpoImage);
		item.setData(ordpage);
		item.setControl(ordpage.getControl(folder));
		if (fCurrScriptProject != null) {
			fSourceContainerPage.init(fCurrScriptProject);
			fLibrariesPage.init(fCurrScriptProject);
			fProjectsPage.init(fCurrScriptProject);
		}
		folder.setSelection(fPageIndex);
		fCurrPage = (BuildPathBasePage) folder.getItem(fPageIndex).getData();
		folder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tabChanged(e.item);
			}
		});
		fTabFolder = folder;
		Dialog.applyDialogFont(composite);
		return composite;
	}

	private Shell getShell() {
		if (fSWTWidget != null) {
			return fSWTWidget.getShell();
		}
		return DLTKUIPlugin.getActiveWorkbenchShell();
	}

	/**
	 * Initializes the buildpath for the given project. Multiple calls to init
	 * are allowed, but all existing settings will be cleared and replace by the
	 * given or default paths.
	 * 
	 * @param jproject
	 *            The java project to configure. Does not have to exist.
	 * @param outputLocation
	 *            The output location to be set in the page. If
	 *            <code>null</code> is passed, jdt default settings are used, or
	 *            - if the project is an existing script project- the output
	 *            location of the existing project
	 * @param buildpathEntries
	 *            The buildpath entries to be set in the page. If
	 *            <code>null</code> is passed, jdt default settings are used, or
	 *            - if the project is an existing script project - the buildpath
	 *            entries of the existing project
	 */
	public void init(IScriptProject jproject, IBuildpathEntry[] buildpathEntries) {
		fCurrScriptProject = jproject;
		boolean projectExists = false;
		List<BPListElement> newBuildpath = null;
		IProject project = fCurrScriptProject.getProject();
		projectExists = project.exists()
				&& project.getFile(BUILDPATH_FILENAME).exists();
		if (projectExists) {
			if (buildpathEntries == null) {
				buildpathEntries = fCurrScriptProject.readRawBuildpath();
			}
		}
		if (buildpathEntries != null) {
			newBuildpath = getExistingEntries(buildpathEntries);
		}
		if (newBuildpath == null) {
			newBuildpath = getDefaultBuildpath(jproject);
		}
		List<BPListElement> exportedEntries = new ArrayList<BPListElement>();
		for (int i = 0; i < newBuildpath.size(); i++) {
			BPListElement curr = newBuildpath.get(i);
			if (curr.isExported()
					|| curr.getEntryKind() == IBuildpathEntry.BPE_SOURCE) {
				exportedEntries.add(curr);
			}
		}
		// inits the dialog field
		fBuildPathDialogField.enableButton(project.exists());
		fBuildPathList.setElements(newBuildpath);
		fBuildPathList.setCheckedElements(exportedEntries);

		fBuildPathList.selectFirstElement();
		if (fSourceContainerPage != null) {
			fSourceContainerPage.init(fCurrScriptProject);
			fProjectsPage.init(fCurrScriptProject);
			fLibrariesPage.init(fCurrScriptProject);
		}

		initializeTimeStamps();
		updateUI();
	}

	protected void updateUI() {
		if (fSWTWidget == null || fSWTWidget.isDisposed()) {
			return;
		}
		if (Display.getCurrent() != null) {
			doUpdateUI();
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (fSWTWidget == null || fSWTWidget.isDisposed()) {
						return;
					}
					doUpdateUI();
				}
			});
		}
	}

	protected void doUpdateUI() {
		fBuildPathDialogField.refresh();
		fBuildPathList.refresh();
		doStatusLineUpdate();
	}

	private String getEncodedSettings() {
		StringBuffer buf = new StringBuffer();
		int nElements = fBuildPathList.getSize();
		buf.append('[').append(nElements).append(']');
		for (int i = 0; i < nElements; i++) {
			BPListElement elem = (BPListElement) fBuildPathList.getElement(i);
			elem.appendEncodedSettings(buf);
		}
		return buf.toString();
	}

	public boolean hasChangesInDialog() {
		String currSettings = getEncodedSettings();
		return !currSettings.equals(fUserSettingsTimeStamp);
	}

	public boolean hasChangesInBuildpathFile() {
		IFile file = fCurrScriptProject.getProject()
				.getFile(BUILDPATH_FILENAME);
		return fFileTimeStamp != file.getModificationStamp();
	}

	public void initializeTimeStamps() {
		IFile file = fCurrScriptProject.getProject()
				.getFile(BUILDPATH_FILENAME);
		fFileTimeStamp = file.getModificationStamp();
		fUserSettingsTimeStamp = getEncodedSettings();
	}

	private List<BPListElement> getExistingEntries(
			IBuildpathEntry[] buildpathEntries) {
		List<BPListElement> newBuildpath = new ArrayList<BPListElement>();
		for (int i = 0; i < buildpathEntries.length; i++) {
			IBuildpathEntry curr = buildpathEntries[i];
			newBuildpath.add(BPListElement.createFromExisting(curr,
					fCurrScriptProject));
		}
		return newBuildpath;
	}

	// -------- public api --------
	/**
	 * @return Returns the script project. Can return
	 *         <code>null<code> if the page has not
	 * been initialized.
	 */
	public IScriptProject getScriptProject() {
		return fCurrScriptProject;
	}

	/**
	 * @return Returns the current class path (raw). Note that the entries
	 *         returned must not be valid.
	 */
	public IBuildpathEntry[] getRawBuildPath() {
		List<BPListElement> elements = fBuildPathList.getElements();
		int nElements = elements.size();
		IBuildpathEntry[] entries = new IBuildpathEntry[elements.size()];
		for (int i = 0; i < nElements; i++) {
			BPListElement currElement = elements.get(i);
			entries[i] = currElement.getBuildpathEntry();
		}
		return entries;
	}

	public int getPageIndex() {
		return fPageIndex;
	}

	/**
	 * @since 2.0
	 */
	protected IDLTKLanguageToolkit getLanguageToolkit() {
		if (fContext instanceof IScriptLanguageProvider) {
			return ((IScriptLanguageProvider) fContext).getLanguageToolkit();
		} else {
			return null;
		}
	}

	/**
	 * @since 2.0
	 */
	protected IDLTKUILanguageToolkit getUILanguageToolkit() {
		final IDLTKLanguageToolkit toolkit = getLanguageToolkit();
		if (toolkit != null) {
			return DLTKUILanguageManager.getLanguageToolkit(toolkit);
		}
		return null;
	}

	// -------- evaluate default settings --------
	protected IPreferenceStore getPreferenceStore() {
		final IDLTKUILanguageToolkit ui = getUILanguageToolkit();
		if (ui != null) {
			return ui.getPreferenceStore();
		}
		// return default value to avoid NPE
		return DLTKUIPlugin.getDefault().getPreferenceStore();
	}

	private List<BPListElement> getDefaultBuildpath(IScriptProject jproj) {
		List<BPListElement> list = new ArrayList<BPListElement>();
		final IDLTKUILanguageToolkit toolkit = getUILanguageToolkit();
		if (toolkit != null) {
			final IResource srcFolder;
			final String sourceFolderName = toolkit
					.getString(PreferenceConstants.SRC_SRCNAME);
			if (toolkit
					.getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ)
					&& sourceFolderName.length() > 0) {
				srcFolder = jproj.getProject().getFolder(sourceFolderName);
			} else {
				srcFolder = jproj.getProject();
			}
			list.add(new BPListElement(jproj, IBuildpathEntry.BPE_SOURCE,
					srcFolder.getFullPath(), srcFolder, false));
		}
		if (DLTKCore.DEBUG) {
			System.err.println("Add default library"); //$NON-NLS-1$
		}
		// IBuildpathEntry[] InterpreterEnvironmentEntries=
		// PreferenceConstants.getDefaultInterpreterEnvironmentLibrary();
		// list.addAll(getExistingEntries(InterpreterEnvironmentEntries));
		return list;
	}

	private class BuildPathAdapter implements IStringButtonAdapter,
			IDialogFieldListener {
		// -------- IStringButtonAdapter --------
		public void changeControlPressed(DialogField field) {
			buildPathChangeControlPressed(field);
		}

		// ---------- IDialogFieldListener --------
		public void dialogFieldChanged(DialogField field) {
			buildPathDialogFieldChanged(field);
		}
	}

	private void buildPathChangeControlPressed(DialogField field) {
		if (field == fBuildPathDialogField) {
			IContainer container = chooseContainer();
			if (container != null) {
				fBuildPathDialogField.setText(container.getFullPath()
						.toString());
			}
		}
	}

	private void buildPathDialogFieldChanged(DialogField field) {
		if (field == fBuildPathList) {
			updatePathStatus();
		}
		doStatusLineUpdate();
	}

	// -------- verification -------------------------------
	private void doStatusLineUpdate() {
		if (Display.getCurrent() != null) {
			IStatus res = findMostSevereStatus();
			fContext.statusChanged(res);
		}
	}

	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fPathStatus,
				fBuildPathStatus });
	}

	/**
	 * Validates the build path.
	 */
	public void updatePathStatus() {
		fPathStatus.setOK();
		List<BPListElement> elements = fBuildPathList.getElements();
		BPListElement entryMissing = null;
		int nEntriesMissing = 0;
		IBuildpathEntry[] entries = new IBuildpathEntry[elements.size()];
		for (int i = elements.size() - 1; i >= 0; i--) {
			BPListElement currElement = elements.get(i);
			boolean isChecked = fBuildPathList.isChecked(currElement);
			if (currElement.getEntryKind() == IBuildpathEntry.BPE_SOURCE) {
				if (!isChecked) {
					fBuildPathList.setCheckedWithoutUpdate(currElement, true);
				}
				if (!fBuildPathList.isGrayed(currElement)) {
					fBuildPathList.setGrayedWithoutUpdate(currElement, true);
				}
			} else {
				currElement.setExported(isChecked);
			}
			entries[i] = currElement.getBuildpathEntry();
			if (currElement.isMissing()) {
				nEntriesMissing++;
				if (entryMissing == null) {
					entryMissing = currElement;
				}
			}
		}
		if (nEntriesMissing > 0) {
			if (nEntriesMissing == 1) {
				fPathStatus.setWarning(Messages.format(
						NewWizardMessages.BuildPathsBlock_warning_EntryMissing,
						entryMissing.getPath().toString()));
			} else {
				fPathStatus
						.setWarning(Messages
								.format(NewWizardMessages.BuildPathsBlock_warning_EntriesMissing,
										String.valueOf(nEntriesMissing)));
			}
		}

		updateBuildPathStatus();
	}

	protected void updateBuildPathStatus() {
		List<BPListElement> elements = fBuildPathList.getElements();
		IBuildpathEntry[] entries = new IBuildpathEntry[elements.size()];
		for (int i = elements.size() - 1; i >= 0; i--) {
			BPListElement currElement = elements.get(i);
			entries[i] = currElement.getBuildpathEntry();
		}
		IModelStatus status = BuildpathEntry.validateBuildpath(
				fCurrScriptProject, entries);
		if (!status.isOK()) {
			fBuildPathStatus.setError(status.getMessage());
			return;
		}
		fBuildPathStatus.setOK();
	}

	// -------- creation -------------------------------
	public static void createProject(IProject project, URI locationURI,
			IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(
				NewWizardMessages.BuildPathsBlock_operationdesc_project, 100);
		// create the project
		try {
			if (!project.exists()) {
				IProjectDescription desc = project.getWorkspace()
						.newProjectDescription(project.getName());
				if (locationURI != null
						&& ResourcesPlugin.getWorkspace().getRoot()
								.getLocationURI().equals(locationURI)) {
					locationURI = null;
				}
				desc.setLocationURI(locationURI);
				project.create(desc, new SubProgressMonitor(monitor, 50));
			}
			if (!project.isOpen()) {
				project.open(new SubProgressMonitor(monitor, 50));
			}
		} finally {
			// not null
			monitor.done();
		}
	}

	@Deprecated
	public static void addScriptNature(IProject project,
			IProgressMonitor monitor, String nature) throws CoreException {
		ResourceUtil.addNature(project, monitor, nature);
	}

	public void configureScriptProject(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		flush(fBuildPathList.getElements(), getScriptProject(), monitor);
		initializeTimeStamps();
		updateUI();
	}

	/*
	 * Creates the script project and sets the configured build path and output
	 * location. If the project already exists only build paths are updated.
	 */
	public static void flush(List<BPListElement> buildpathEntries,
			IScriptProject javaProject, IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.setTaskName(NewWizardMessages.BuildPathsBlock_operationdesc_Script);
		monitor.beginTask("", buildpathEntries.size() * 4 + 4); //$NON-NLS-1$
		try {
			IProject project = javaProject.getProject();
			IPath projPath = project.getFullPath();
			monitor.worked(1);
			// IWorkspaceRoot fWorkspaceRoot =
			// DLTKUIPlugin.getWorkspace().getRoot();
			// create and set the output path first
			monitor.worked(1);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			int nEntries = buildpathEntries.size();
			IBuildpathEntry[] buildpath = new IBuildpathEntry[nEntries];
			int i = 0;
			for (BPListElement entry : buildpathEntries) {
				buildpath[i] = entry.getBuildpathEntry();
				i++;
				IResource res = entry.getResource();
				// 1 tick
				if (res instanceof IFolder && entry.getLinkTarget() == null
						&& !res.exists()) {
					CoreUtility.createFolder((IFolder) res, true, true,
							new SubProgressMonitor(monitor, 1));
				} else {
					monitor.worked(1);
				}
				// 3 ticks
				if (entry.getEntryKind() == IBuildpathEntry.BPE_SOURCE) {
					monitor.worked(1);
					IPath path = entry.getPath();
					if (projPath.equals(path)) {
						monitor.worked(2);
						continue;
					}
					if (projPath.isPrefixOf(path)) {
						path = path
								.removeFirstSegments(projPath.segmentCount());
					}
					IFolder folder = project.getFolder(path);
					IPath orginalPath = entry.getOrginalPath();
					if (orginalPath == null) {
						if (!folder.exists()) {
							// New source folder needs to be created
							if (entry.getLinkTarget() == null) {
								CoreUtility.createFolder(folder, true, true,
										new SubProgressMonitor(monitor, 2));
							} else {
								folder.createLink(entry.getLinkTarget(),
										IResource.ALLOW_MISSING_LOCAL,
										new SubProgressMonitor(monitor, 2));
							}
						}
					} else {
						if (projPath.isPrefixOf(orginalPath)) {
							orginalPath = orginalPath
									.removeFirstSegments(projPath
											.segmentCount());
						}
						IFolder orginalFolder = project.getFolder(orginalPath);
						if (entry.getLinkTarget() == null) {
							if (!folder.exists()) {
								// Source folder was edited, move to new
								// location
								IPath parentPath = entry.getPath()
										.removeLastSegments(1);
								if (projPath.isPrefixOf(parentPath)) {
									parentPath = parentPath
											.removeFirstSegments(projPath
													.segmentCount());
								}
								if (parentPath.segmentCount() > 0) {
									IFolder parentFolder = project
											.getFolder(parentPath);
									if (!parentFolder.exists()) {
										CoreUtility.createFolder(parentFolder,
												true, true,
												new SubProgressMonitor(monitor,
														1));
									} else {
										monitor.worked(1);
									}
								} else {
									monitor.worked(1);
								}
								orginalFolder.move(entry.getPath(), true, true,
										new SubProgressMonitor(monitor, 1));
							}
						} else {
							if (!folder.exists()
									|| !entry.getLinkTarget().equals(
											entry.getOrginalLinkTarget())) {
								orginalFolder.delete(true,
										new SubProgressMonitor(monitor, 1));
								folder.createLink(entry.getLinkTarget(),
										IResource.ALLOW_MISSING_LOCAL,
										new SubProgressMonitor(monitor, 1));
							}
						}
					}
				} else {
					monitor.worked(3);
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			javaProject.setRawBuildpath(buildpath, new SubProgressMonitor(
					monitor, 2));
		} finally {
			monitor.done();
		}
	}

	public static boolean hasClassfiles(IResource resource)
			throws CoreException {
		if (resource.isDerived()) {
			return true;
		}
		if (resource instanceof IContainer) {
			IResource[] members = ((IContainer) resource).members();
			for (int i = 0; i < members.length; i++) {
				if (hasClassfiles(members[i])) {
					return true;
				}
			}
		}
		return false;
	}

	public static void removeOldClassfiles(IResource resource)
			throws CoreException {
		if (resource.isDerived()) {
			resource.delete(false, null);
		} else if (resource instanceof IContainer) {
			IResource[] members = ((IContainer) resource).members();
			for (int i = 0; i < members.length; i++) {
				removeOldClassfiles(members[i]);
			}
		}
	}

	public static IRemoveOldBinariesQuery getRemoveOldBinariesQuery(
			final Shell shell) {
		return new IRemoveOldBinariesQuery() {
			public boolean doQuery(final IPath oldOutputLocation)
					throws OperationCanceledException {
				final int[] res = new int[] { 1 };
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						Shell sh = shell != null ? shell : DLTKUIPlugin
								.getActiveWorkbenchShell();
						String title = NewWizardMessages.BuildPathsBlock_RemoveBinariesDialog_title;
						String message = Messages
								.format(NewWizardMessages.BuildPathsBlock_RemoveBinariesDialog_description,
										oldOutputLocation.toString());
						MessageDialog dialog = new MessageDialog(sh, title,
								null, message, MessageDialog.QUESTION,
								new String[] { IDialogConstants.YES_LABEL,
										IDialogConstants.NO_LABEL,
										IDialogConstants.CANCEL_LABEL }, 0);
						res[0] = dialog.open();
					}
				});
				if (res[0] == 0) {
					return true;
				} else if (res[0] == 1) {
					return false;
				}
				throw new OperationCanceledException();
			}
		};
	}

	// ---------- util method ------------
	private IContainer chooseContainer() {
		Class<?>[] acceptedClasses = new Class[] { IProject.class,
				IFolder.class };
		ISelectionStatusValidator validator = new TypedElementSelectionValidator(
				acceptedClasses, false);
		IProject[] allProjects = fWorkspaceRoot.getProjects();
		ArrayList<IProject> rejectedElements = new ArrayList<IProject>(
				allProjects.length);
		IProject currProject = fCurrScriptProject.getProject();
		for (int i = 0; i < allProjects.length; i++) {
			if (!allProjects[i].equals(currProject)) {
				rejectedElements.add(allProjects[i]);
			}
		}
		ViewerFilter filter = new TypedViewerFilter(acceptedClasses,
				rejectedElements.toArray());
		ILabelProvider lp = new WorkbenchLabelProvider();
		ITreeContentProvider cp = new WorkbenchContentProvider();
		IResource initSelection = null;
		FolderSelectionDialog dialog = new FolderSelectionDialog(getShell(),
				lp, cp);
		dialog.setTitle(NewWizardMessages.BuildPathsBlock_ChooseOutputFolderDialog_title);
		dialog.setValidator(validator);
		dialog.setMessage(NewWizardMessages.BuildPathsBlock_ChooseOutputFolderDialog_description);
		dialog.addFilter(filter);
		dialog.setInput(fWorkspaceRoot);
		dialog.setInitialSelection(initSelection);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		if (dialog.open() == Window.OK) {
			return (IContainer) dialog.getFirstResult();
		}
		return null;
	}

	// -------- tab switching ----------
	protected void tabChanged(Widget widget) {
		if (widget instanceof TabItem) {
			TabItem tabItem = (TabItem) widget;
			BuildPathBasePage newPage = (BuildPathBasePage) tabItem.getData();
			if (fCurrPage != null) {
				List<?> selection = fCurrPage.getSelection();
				if (!selection.isEmpty()) {
					newPage.setSelection(selection, false);
				}
			}
			fCurrPage = newPage;
			fPageIndex = tabItem.getParent().getSelectionIndex();
		}
	}

	private int getPageIndex(int entryKind) {
		switch (entryKind) {
		case IBuildpathEntry.BPE_CONTAINER:
		case IBuildpathEntry.BPE_LIBRARY:
		case IBuildpathEntry.BPE_PROJECT:
			return 1;
		case IBuildpathEntry.BPE_SOURCE:
			return 0;
		}
		return 0;
	}

	private BPListElement findElement(IBuildpathEntry entry) {
		for (int i = 0, len = fBuildPathList.getSize(); i < len; i++) {
			BPListElement curr = (BPListElement) fBuildPathList.getElement(i);
			if (curr.getEntryKind() == entry.getEntryKind()
					&& curr.getPath().equals(entry.getPath())) {
				return curr;
			}
		}
		return null;
	}

	public void setElementToReveal(IBuildpathEntry entry, String attributeKey) {
		int pageIndex = getPageIndex(entry.getEntryKind());
		if (fTabFolder == null) {
			fPageIndex = pageIndex;
		} else {
			fTabFolder.setSelection(pageIndex);
			BPListElement element = findElement(entry);
			if (element != null) {
				Object elementToSelect = element;
				if (attributeKey != null) {
					Object attrib = element.findAttributeElement(attributeKey);
					if (attrib != null) {
						elementToSelect = attrib;
					}
				}
				BuildPathBasePage page = (BuildPathBasePage) fTabFolder
						.getItem(pageIndex).getData();
				List<Object> selection = new ArrayList<Object>(1);
				selection.add(elementToSelect);
				page.setSelection(selection, true);
			}
		}
	}

	public void addElement(IBuildpathEntry entry) {
		int pageIndex = getPageIndex(entry.getEntryKind());
		if (fTabFolder == null) {
			fPageIndex = pageIndex;
		} else {
			fTabFolder.setSelection(pageIndex);
			Object page = fTabFolder.getItem(pageIndex).getData();
			if (page instanceof LibrariesWorkbookPage) {
				BPListElement element = BPListElement.createFromExisting(entry,
						fCurrScriptProject);
				((LibrariesWorkbookPage) page).addElement(element);
			}
		}
	}
}
