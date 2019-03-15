// CHECKSTYLE:OFF
/*
 *
 * Copyright (C) 2008-2009 Yves Zoundi
 * Copyright (C) 2012 University of Waikato, Hamilton, NZ (made GlobFilter functional)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 */
package net.agilhard.vfsjfilechooser2.plaf.basic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ActionMapUIResource;

import net.agilhard.vfsjfilechooser2.VFSJFileChooser;
import net.agilhard.vfsjfilechooser2.plaf.AbstractVFSFileChooserUI;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.agilhard.vfsjfilechooser2.filechooser.AbstractVFSFileFilter;
import net.agilhard.vfsjfilechooser2.filechooser.AbstractVFSFileSystemView;
import net.agilhard.vfsjfilechooser2.filechooser.AbstractVFSFileView;
import net.agilhard.vfsjfilechooser2.filepane.VFSFilePane;
import net.agilhard.vfsjfilechooser2.utils.SwingCommonsUtilities;
import net.agilhard.vfsjfilechooser2.utils.VFSResources;
import net.agilhard.vfsjfilechooser2.utils.VFSUtils;

/**
 * The BasicFileChooserUI implementation using commons-vfs based on Swing
 * BasicFileChooserUI
 *
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 * @version 0.0.1
 */
public class BasicVFSFileChooserUI extends AbstractVFSFileChooserUI {

	/** The Logger. */
	private final Logger log = LoggerFactory.getLogger(BasicVFSFileChooserUI.class);

	/* FileView icons */
	protected Icon directoryIcon = null;
	protected Icon fileIcon = null;
	protected Icon computerIcon = null;
	protected Icon hardDriveIcon = null;
	protected Icon floppyDriveIcon = null;
	protected Icon newFolderIcon = null;
	protected Icon upFolderIcon = null;
	protected Icon homeFolderIcon = null;
	protected Icon listViewIcon = null;
	protected Icon detailsViewIcon = null;
	protected Icon viewMenuIcon = null;
	protected int saveButtonMnemonic = 0;
	protected int openButtonMnemonic = 0;
	protected int cancelButtonMnemonic = 0;
	protected int updateButtonMnemonic = 0;
	protected int helpButtonMnemonic = 0;

	/**
	 * The mnemonic keycode used for the approve button when a directory is
	 * selected and the current selection mode is FILES_ONLY.
	 *
	 * @since 1.4
	 */
	protected int directoryOpenButtonMnemonic = 0;
	protected String saveButtonText = null;
	protected String openButtonText = null;
	protected String cancelButtonText = null;
	protected String updateButtonText = null;
	protected String helpButtonText = null;

	/**
	 * The label text displayed on the approve button when a directory is
	 * selected and the current selection mode is FILES_ONLY.
	 *
	 * @since 1.4
	 */
	protected String directoryOpenButtonText = null;
	private String openDialogTitleText = null;
	private String saveDialogTitleText = null;
	protected String saveButtonToolTipText = null;
	protected String openButtonToolTipText = null;
	protected String cancelButtonToolTipText = null;
	protected String updateButtonToolTipText = null;
	protected String helpButtonToolTipText = null;

	/**
	 * The tooltip text displayed on the approve button when a directory is
	 * selected and the current selection mode is FILES_ONLY.
	 *
	 * @since 1.4
	 */
	protected String directoryOpenButtonToolTipText = null;

	// Some generic FileChooser functions
	private final Action approveSelectionAction = new ApproveSelectionAction();
	private final Action cancelSelectionAction = new CancelSelectionAction();
	private final Action updateAction = new UpdateAction();
	private Action newFolderAction;
	private final Action goHomeAction = new GoHomeAction();
	private final Action changeToParentDirectoryAction = new ChangeToParentDirectoryAction();
	private String newFolderErrorSeparator = null;
	private String newFolderErrorText = null;
	private String newFolderParentDoesntExistTitleText = null;
	private String newFolderParentDoesntExistText = null;
	private String fileDescriptionText = null;
	private String directoryDescriptionText = null;
	private VFSJFileChooser filechooser = null;
	private boolean directorySelected = false;
	private FileObject directory = null;
	private PropertyChangeListener propertyChangeListener = null;
	private final AcceptAllFileFilter acceptAllFileFilter = new AcceptAllFileFilter();
	private AbstractVFSFileFilter actualFileFilter = null;
	private GlobFilter globFilter = null;
	private BasicVFSDirectoryModel model = null;
	private final BasicVFSFileView fileView = new BasicVFSFileView();
	private boolean usesSingleFilePane;
	private boolean readOnly;

	// The accessoryPanel is a container to place the VFSJFileChooser accessory
	// component
	private JPanel accessoryPanel = null;
	private Handler handler;

	public BasicVFSFileChooserUI(final VFSJFileChooser b) {
		this.filechooser = b;
	}

	/**
	 *
	 * @param c
	 */
	@Override
	public void installUI(final JComponent c) {
		this.accessoryPanel = new JPanel(new BorderLayout());
		this.filechooser = (VFSJFileChooser) c;

		this.createModel();

		this.clearIconCache();

		this.installDefaults(this.filechooser);
		this.installComponents(this.filechooser);
		this.installListeners(this.filechooser);

		this.filechooser.applyComponentOrientation(this.filechooser.getComponentOrientation());
	}

	/**
	 *
	 * @param c
	 */
	@SuppressWarnings("unused")
	@Override
	public void uninstallUI(final JComponent c) {
		this.uninstallListeners(this.filechooser);
		this.uninstallComponents(this.filechooser);
		this.uninstallDefaults(this.filechooser);

		if (this.accessoryPanel != null) {
			this.accessoryPanel.removeAll();
		}

		this.accessoryPanel = null;
		this.getFileChooser().removeAll();

		this.handler = null;
	}

	/**
	 *
	 * @param fc
	 */
	@SuppressWarnings("unused")
	public void installComponents(final VFSJFileChooser fc) {
		// .
	}

	/**
	 *
	 * @param fc
	 */
	@SuppressWarnings("unused")
	public void uninstallComponents(final VFSJFileChooser fc) {
		// .

	}

	protected void installListeners(final VFSJFileChooser fc) {
		this.propertyChangeListener = this.createPropertyChangeListener(fc);

		if (this.propertyChangeListener != null) {
			fc.addPropertyChangeListener(this.propertyChangeListener);
		}

		fc.addPropertyChangeListener(this.getModel());

		final ActionMap actionMap = this.getActionMap();
		SwingUtilities.replaceUIActionMap(fc, actionMap);
	}

	protected ActionMap getActionMap() {
		return this.createActionMap();
	}

	protected ActionMap createActionMap() {
		final ActionMap map = new ActionMapUIResource();

		final Action refreshAction = new AbstractVFSUIAction(VFSFilePane.ACTION_REFRESH) {

			@SuppressWarnings("unused")
			@Override
			public void actionPerformed(final ActionEvent evt) {
				BasicVFSFileChooserUI.this.getFileChooser().rescanCurrentDirectory();
			}
		};

		map.put(VFSFilePane.ACTION_APPROVE_SELECTION, this.getApproveSelectionAction());
		map.put(VFSFilePane.ACTION_CANCEL, this.getCancelSelectionAction());
		map.put(VFSFilePane.ACTION_REFRESH, refreshAction);
		map.put(VFSFilePane.ACTION_CHANGE_TO_PARENT_DIRECTORY, this.getChangeToParentDirectoryAction());

		return map;
	}

	protected void uninstallListeners(final VFSJFileChooser fc) {
		if (this.propertyChangeListener != null) {
			fc.removePropertyChangeListener(this.propertyChangeListener);
		}

		fc.removePropertyChangeListener(this.getModel());
		SwingUtilities.replaceUIInputMap(fc, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
		SwingUtilities.replaceUIActionMap(fc, null);
	}

	protected void installDefaults(final VFSJFileChooser fc) {
		this.installIcons(fc);
		this.installStrings(fc);
		this.usesSingleFilePane = UIManager.getBoolean("FileChooser.usesSingleFilePane");
		this.readOnly = UIManager.getBoolean("FileChooser.readOnly");
		LookAndFeel.installProperty(fc, "opaque", Boolean.FALSE);
	}

	@SuppressWarnings("unused")
	protected void installIcons(final VFSJFileChooser fc) {
		final UIDefaults defaults = UIManager.getLookAndFeel().getDefaults();
		this.directoryIcon = this.lookupIcon("folder.png");
		this.fileIcon = this.lookupIcon("file.png");
		this.computerIcon = defaults.getIcon("FileView.computerIcon");
		this.hardDriveIcon = defaults.getIcon("FileView.hardDriveIcon");
		this.floppyDriveIcon = defaults.getIcon("FileView.floppyDriveIcon");
		this.newFolderIcon = this.lookupIcon("folder_add.png");
		this.upFolderIcon = this.lookupIcon("go-up.png");
		this.homeFolderIcon = this.lookupIcon("folder_user.png");
		this.detailsViewIcon = this.lookupIcon("application_view_detail.png");
		this.listViewIcon = this.lookupIcon("application_view_list.png");
		this.viewMenuIcon = defaults.getIcon("FileChooser.viewMenuIcon");
	}

	// Hardcoded path until we get the possibility to have themes
	// or to use the default laf icons
	private Icon lookupIcon(final String path) {
		return new ImageIcon(this.getClass().getResource("/net/agilhard/vfsjfilechooser2/plaf/icons/" + path));
	}

	protected void installStrings(final VFSJFileChooser fc) {
		final Locale l = fc.getLocale();
		this.newFolderErrorText = VFSResources.getMessage("VFSJFileChooser.newFolderErrorText");
		this.newFolderErrorSeparator = VFSResources.getMessage("VFSJFileChooser.newFolderErrorSeparator");

		this.newFolderParentDoesntExistTitleText = VFSResources
				.getMessage("VFSJFileChooser.newFolderParentDoesntExistTitleText");
		this.newFolderParentDoesntExistText = VFSResources.getMessage("VFSJFileChooser.newFolderParentDoesntExistText");

		this.fileDescriptionText = VFSResources.getMessage("VFSJFileChooser.fileDescriptionText");
		this.directoryDescriptionText = VFSResources.getMessage("VFSJFileChooser.directoryDescriptionText");

		this.saveButtonText = VFSResources.getMessage("VFSJFileChooser.saveButtonText");
		this.openButtonText = VFSResources.getMessage("VFSJFileChooser.openButtonText");
		this.saveDialogTitleText = VFSResources.getMessage("VFSJFileChooser.saveDialogTitleText");
		this.openDialogTitleText = VFSResources.getMessage("VFSJFileChooser.openDialogTitleText");
		this.cancelButtonText = VFSResources.getMessage("VFSJFileChooser.cancelButtonText");
		this.updateButtonText = VFSResources.getMessage("VFSJFileChooser.updateButtonText");
		this.helpButtonText = VFSResources.getMessage("VFSJFileChooser.helpButtonText");
		this.directoryOpenButtonText = VFSResources.getMessage("VFSJFileChooser.directoryOpenButtonText");

		this.saveButtonMnemonic = this.getMnemonic("VFSJFileChooser.saveButtonMnemonic", l);
		this.openButtonMnemonic = this.getMnemonic("VFSJFileChooser.openButtonMnemonic", l);
		this.cancelButtonMnemonic = this.getMnemonic("VFSJFileChooser.cancelButtonMnemonic", l);
		this.updateButtonMnemonic = this.getMnemonic("VFSJFileChooser.updateButtonMnemonic", l);
		this.helpButtonMnemonic = this.getMnemonic("VFSJFileChooser.helpButtonMnemonic", l);
		this.directoryOpenButtonMnemonic = this.getMnemonic("VFSJFileChooser.directoryOpenButtonMnemonic", l);

		this.saveButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.saveButtonToolTipText");
		this.openButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.openButtonToolTipText");
		this.cancelButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.cancelButtonToolTipText");
		this.updateButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.updateButtonToolTipText");
		this.helpButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.helpButtonToolTipText");
		this.directoryOpenButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.directoryOpenButtonToolTipText");
	}

	protected void uninstallDefaults(final VFSJFileChooser fc) {
		this.uninstallIcons(fc);
		this.uninstallStrings(fc);
	}

	@SuppressWarnings("unused")
	protected void uninstallIcons(final VFSJFileChooser fc) {
		this.directoryIcon = null;
		this.fileIcon = null;
		this.computerIcon = null;
		this.hardDriveIcon = null;
		this.floppyDriveIcon = null;

		this.newFolderIcon = null;
		this.upFolderIcon = null;
		this.homeFolderIcon = null;
		this.detailsViewIcon = null;
		this.listViewIcon = null;
		this.viewMenuIcon = null;
	}

	@SuppressWarnings("unused")
	protected void uninstallStrings(final VFSJFileChooser fc) {
		this.saveButtonText = null;
		this.openButtonText = null;
		this.cancelButtonText = null;
		this.updateButtonText = null;
		this.helpButtonText = null;
		this.directoryOpenButtonText = null;

		this.saveButtonToolTipText = null;
		this.openButtonToolTipText = null;
		this.cancelButtonToolTipText = null;
		this.updateButtonToolTipText = null;
		this.helpButtonToolTipText = null;
		this.directoryOpenButtonToolTipText = null;
	}

	protected void createModel() {
		if (this.model != null) {
			this.model.invalidateFileCache();
		}

		this.model = new BasicVFSDirectoryModel(this.getFileChooser());
	}

	public BasicVFSDirectoryModel getModel() {
		return this.model;
	}

	@SuppressWarnings("unused")
	public PropertyChangeListener createPropertyChangeListener(final VFSJFileChooser fc) {
		return null;
	}

	public String getFileName() {
		return null;
	}

	public String getDirectoryName() {
		return null;
	}

	@SuppressWarnings("unused")
	public void setFileName(final String filename) {
		// .
	}

	@SuppressWarnings("unused")
	public void setDirectoryName(final String dirname) {
		// .
	}

	@SuppressWarnings("unused")
	@Override
	public void rescanCurrentDirectory(final VFSJFileChooser fc) {
		// .
	}

	@SuppressWarnings("unused")
	@Override
	public void ensureFileIsVisible(final VFSJFileChooser fc, final FileObject f) {
		// .
	}

	public VFSJFileChooser getFileChooser() {
		return this.filechooser;
	}

	public JPanel getAccessoryPanel() {
		return this.accessoryPanel;
	}

	@SuppressWarnings("unused")
	protected JButton getApproveButton(final VFSJFileChooser fc) {
		return null;
	}

	public String getApproveButtonToolTipText(final VFSJFileChooser fc) {
		final String tooltipText = fc.getApproveButtonToolTipText();

		if (tooltipText != null) {
			return tooltipText;
		}

		if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.OPEN) {
			return this.openButtonToolTipText;
		} else if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.SAVE) {
			return this.saveButtonToolTipText;
		}

		return null;
	}

	public void clearIconCache() {
		this.fileView.clearIconCache();
	}

	// ********************************************
	// ************ Create Listeners **************
	// ********************************************
	private Handler getHandler() {
		if (this.handler == null) {
			this.handler = new Handler();
		}

		return this.handler;
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	protected MouseListener createDoubleClickListener(final VFSJFileChooser fc, final JList list) {
		return new Handler(list);
	}

	@SuppressWarnings("unused")
	public ListSelectionListener createListSelectionListener(final VFSJFileChooser fc) {
		return this.getHandler();
	}

	/**
	 * Property to remember whether a directory is currently selected in the UI.
	 *
	 * @return <code>true</code> iff a directory is currently selected.
	 * @since 1.4
	 */
	protected boolean isDirectorySelected() {
		return this.directorySelected;
	}

	/**
	 * Property to remember whether a directory is currently selected in the UI.
	 * This is normally called by the UI on a selection event.
	 *
	 * @param b
	 *            iff a directory is currently selected.
	 * @since 1.4
	 */
	protected void setDirectorySelected(final boolean b) {
		this.directorySelected = b;
	}

	/**
	 * Property to remember the directory that is currently selected in the UI.
	 *
	 * @return the value of the <code>directory</code> property
	 * @see #setDirectory
	 * @since 1.4
	 */
	protected FileObject getDirectory() {
		return this.directory;
	}

	/**
	 * Property to remember the directory that is currently selected in the UI.
	 * This is normally called by the UI on a selection event.
	 *
	 * @param f
	 *            the <code>File</code> object representing the directory that
	 *            is currently selected
	 * @since 1.4
	 */
	protected void setDirectory(final FileObject f) {
		this.directory = f;
	}

	public static int getUIDefaultsInt(final Object key, final Locale l, final int defaultValue) {
		final Object value = UIManager.get(key, l);

		if (value instanceof Integer) {
			return ((Integer) value).intValue();
		}

		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (final NumberFormatException nfe) {
				// .
			}
		}

		return defaultValue;
	}

	/**
	 * Returns the mnemonic for the given key.
	 */
	private int getMnemonic(final String key, final Locale l) {
		return getUIDefaultsInt(key, l, 0);
	}

	// *******************************************************
	// ************ FileChooser UI PLAF methods **************
	// *******************************************************
	/**
	 * Returns the default accept all file filter
	 */
	@SuppressWarnings("unused")
	@Override
	public AbstractVFSFileFilter getAcceptAllFileFilter(final VFSJFileChooser fc) {
		return this.acceptAllFileFilter;
	}

	@SuppressWarnings("unused")
	@Override
	public AbstractVFSFileView getFileView(final VFSJFileChooser fc) {
		return this.fileView;
	}

	/**
	 * Returns the title of this dialog
	 */
	@Override
	public String getDialogTitle(final VFSJFileChooser fc) {
		final String dialogTitle = fc.getDialogTitle();

		if (dialogTitle != null) {
			return dialogTitle;
		} else if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.OPEN) {
			return this.openDialogTitleText;
		} else if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.SAVE) {
			return this.saveDialogTitleText;
		} else {
			return this.getApproveButtonText(fc);
		}
	}

	public int getApproveButtonMnemonic(final VFSJFileChooser fc) {
		final int mnemonic = fc.getApproveButtonMnemonic();

		if (mnemonic > 0) {
			return mnemonic;
		} else if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.OPEN) {
			return this.openButtonMnemonic;
		} else if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.SAVE) {
			return this.saveButtonMnemonic;
		} else {
			return mnemonic;
		}
	}

	@Override
	public String getApproveButtonText(final VFSJFileChooser fc) {
		final String buttonText = fc.getApproveButtonText();

		if (buttonText != null) {
			return buttonText;
		} else if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.OPEN) {
			return this.openButtonText;
		} else if (fc.getDialogType() == VFSJFileChooser.DIALOG_TYPE.SAVE) {
			return this.saveButtonText;
		} else {
			return null;
		}
	}

	// *****************************
	// ***** Directory Actions *****
	// *****************************
	public Action getNewFolderAction() {
		if (this.newFolderAction == null) {
			this.newFolderAction = new NewFolderAction();

			// Note: Don't return null for readOnly, it might
			// break older apps.
			if (this.readOnly) {
				this.newFolderAction.setEnabled(false);
			}
		}

		return this.newFolderAction;
	}

	public Action getGoHomeAction() {
		return this.goHomeAction;
	}

	public Action getChangeToParentDirectoryAction() {
		return this.changeToParentDirectoryAction;
	}

	public Action getApproveSelectionAction() {
		return this.approveSelectionAction;
	}

	public Action getCancelSelectionAction() {
		return this.cancelSelectionAction;
	}

	public Action getUpdateAction() {
		return this.updateAction;
	}

	private void resetGlobFilter() {
		if (this.actualFileFilter != null) {
			final VFSJFileChooser chooser = this.getFileChooser();
			final AbstractVFSFileFilter currentFilter = chooser.getFileFilter();

			if ((currentFilter != null) && currentFilter.equals(this.globFilter)) {
				chooser.setFileFilter(this.actualFileFilter);
				chooser.removeChoosableFileFilter(this.globFilter);
			}

			this.actualFileFilter = null;
		}
	}

	private static boolean isGlobPattern(final String filename) {
		return (((File.separatorChar == '\\') && ((filename.indexOf('*') >= 0) || (filename.indexOf('?') >= 0)))
				|| ((File.separatorChar == '/') && ((filename.indexOf('*') >= 0) || (filename.indexOf('?') >= 0)
						|| (filename.indexOf('[') >= 0))));
	}

	public void changeDirectory(final FileObject dir) {
		final VFSJFileChooser fc = this.getFileChooser();

		fc.setCurrentDirectoryObject(dir);

		if ((fc.getFileSelectionMode() == VFSJFileChooser.SELECTION_MODE.FILES_AND_DIRECTORIES)
				&& fc.getFileSystemView().isFileSystem(dir)) {
			this.setFileName(dir.getName().getBaseName());
		}
	}

	private class Handler implements MouseListener, ListSelectionListener {

		@SuppressWarnings("rawtypes")
		JList list;

		Handler() {
		}

		@SuppressWarnings("rawtypes")
		Handler(final JList list) {
			this.list = list;
		}

		@Override
		public void mouseClicked(final MouseEvent evt) {
			// System.out.println("count2:" + evt.getClickCount());

			// Note: we can't depend on evt.getSource() because of backward
			// compatability
			if ((this.list != null) && SwingUtilities.isLeftMouseButton(evt) && (evt.getClickCount() == 2)) {
				final int index = SwingCommonsUtilities.loc2IndexFileList(this.list, evt.getPoint());

				if (index >= 0) {
					final FileObject f = (FileObject) this.list.getModel().getElementAt(index);

					if (BasicVFSFileChooserUI.this.getFileChooser().isTraversable(f)) {
						this.list.clearSelection();
						BasicVFSFileChooserUI.this.changeDirectory(f);
					} else {
						BasicVFSFileChooserUI.this.getFileChooser().approveSelection();
					}
				}
			}
		}

		@Override
		@SuppressWarnings("unused")
		public void mouseEntered(final MouseEvent evt) {
			if (this.list != null) {
				final TransferHandler th1 = BasicVFSFileChooserUI.this.getFileChooser().getTransferHandler();
				final TransferHandler th2 = this.list.getTransferHandler();

				if (th1 != th2) {
					this.list.setTransferHandler(th1);
				}

				if (BasicVFSFileChooserUI.this.getFileChooser().getDragEnabled() != this.list.getDragEnabled()) {
					this.list.setDragEnabled(BasicVFSFileChooserUI.this.getFileChooser().getDragEnabled());
				}
			}
		}

		@SuppressWarnings("unused")
		@Override
		public void mouseExited(final MouseEvent evt) {
			// .
		}

		@SuppressWarnings("unused")
		@Override
		public void mousePressed(final MouseEvent evt) {
			// .
		}

		@SuppressWarnings("unused")
		@Override
		public void mouseReleased(final MouseEvent evt) {
			// .
		}

		@SuppressWarnings({ "rawtypes", "hiding", "synthetic-access" })
		@Override
		public void valueChanged(final ListSelectionEvent evt) {
			if (!evt.getValueIsAdjusting()) {
				final VFSJFileChooser chooser = BasicVFSFileChooserUI.this.getFileChooser();
				final AbstractVFSFileSystemView fsv = chooser.getFileSystemView();
				final JList list = (JList) evt.getSource();

				final VFSJFileChooser.SELECTION_MODE fsm = chooser.getFileSelectionMode();
				final boolean useSetDirectory = BasicVFSFileChooserUI.this.usesSingleFilePane && (fsm == VFSJFileChooser.SELECTION_MODE.FILES_ONLY);

				if (chooser.isMultiSelectionEnabled()) {
					FileObject[] files = new FileObject[0];

					final Object[] objects = list.getSelectedValues();

					if (objects != null) {
						final int count = objects.length;

						if ((count == 1) && (VFSUtils.isDirectory((FileObject) objects[0])
								&& chooser.isTraversable(((FileObject) objects[0]))
								&& (useSetDirectory || !fsv.isFileSystem(((FileObject) objects[0]))))) {
							BasicVFSFileChooserUI.this.setDirectorySelected(true);
							BasicVFSFileChooserUI.this.setDirectory(((FileObject) objects[0]));
						} else {
							final List<FileObject> fList = new ArrayList<FileObject>(count);

							for (int i = 0; i < count; i++) {
								final FileObject f = (FileObject) objects[i];
								final boolean isDir = VFSUtils.isDirectory(f);

								if ((chooser.isFileSelectionEnabled() && !isDir)
										|| (chooser.isDirectorySelectionEnabled() && fsv.isFileSystem(f) && isDir)) {
									fList.add(f);
								}
							}

							if (!fList.isEmpty()) {
								files = fList.toArray(new FileObject[fList.size()]);
							}

							BasicVFSFileChooserUI.this.setDirectorySelected(false);
						}
					}

					chooser.setSelectedFileObjects(files);
				} else {
					final FileObject file = (FileObject) list.getSelectedValue();

					if ((file != null) && chooser.isTraversable(file) && (useSetDirectory || !fsv.isFileSystem(file))) {
						BasicVFSFileChooserUI.this.setDirectorySelected(true);
						BasicVFSFileChooserUI.this.setDirectory(file);

						if (BasicVFSFileChooserUI.this.usesSingleFilePane) {
							chooser.setSelectedFileObject(null);
						}
					} else {
						BasicVFSFileChooserUI.this.setDirectorySelected(false);

						if (file != null) {
							chooser.setSelectedFileObject(file);
						}
					}
				}
			}
		}
	}

	protected class DoubleClickListener extends MouseAdapter {
		// NOTE: This class exists only for backward compatability. All
		// its functionality has been moved into Handler. If you need to add
		// new functionality add it to the Handler, but make sure this
		// class calls into the Handler.
		Handler dclHandler;

		@SuppressWarnings("rawtypes")
		public DoubleClickListener(final JList list) {
			this.dclHandler = new Handler(list);
		}

		/**
		 * The JList used for representing the files is created by subclasses,
		 * but the selection is monitored in this class. The TransferHandler
		 * installed in the VFSJFileChooser is also installed in the file list
		 * as it is used as the actual transfer source. The list is updated on a
		 * mouse enter to reflect the current data transfer state of the file
		 * chooser.
		 */
		@Override
		public void mouseEntered(final MouseEvent e) {
			this.dclHandler.mouseEntered(e);
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			this.dclHandler.mouseClicked(e);
		}
	}

	protected class SelectionListener implements ListSelectionListener {
		// NOTE: This class exists only for backward compatability. All
		// its functionality has been moved into Handler. If you need to add
		// new functionality add it to the Handler, but make sure this
		// class calls into the Handler.
		@SuppressWarnings("synthetic-access")
		@Override
		public void valueChanged(final ListSelectionEvent e) {
			BasicVFSFileChooserUI.this.getHandler().valueChanged(e);
		}
	}

	/**
	 * Creates a new folder.
	 */
	@SuppressWarnings("serial")
	protected class NewFolderAction extends AbstractAction {
		protected NewFolderAction() {
			super(VFSFilePane.ACTION_NEW_FOLDER);
		}

		@Override
		@SuppressWarnings({ "synthetic-access", "unused" })
		public void actionPerformed(final ActionEvent e) {
			if (BasicVFSFileChooserUI.this.readOnly) {
				return;
			}

			final VFSJFileChooser fc = BasicVFSFileChooserUI.this.getFileChooser();
			final FileObject currentDirectory = fc.getCurrentDirectoryObject();

			if (!VFSUtils.exists(currentDirectory)) {
				JOptionPane.showMessageDialog(fc, BasicVFSFileChooserUI.this.newFolderParentDoesntExistText, BasicVFSFileChooserUI.this.newFolderParentDoesntExistTitleText,
						JOptionPane.WARNING_MESSAGE);

				return;
			}

			FileObject newFolder;

			try {
				newFolder = fc.getFileSystemView().createNewFolder(currentDirectory);

				if (fc.isMultiSelectionEnabled()) {
					fc.setSelectedFileObjects(new FileObject[] { newFolder });
				} else {
					fc.setSelectedFileObject(newFolder);
				}
			} catch (final IOException exc) {
				JOptionPane.showMessageDialog(fc, BasicVFSFileChooserUI.this.newFolderErrorText + BasicVFSFileChooserUI.this.newFolderErrorSeparator + exc,
						BasicVFSFileChooserUI.this.newFolderErrorText, JOptionPane.ERROR_MESSAGE);

				return;
			}

			fc.rescanCurrentDirectory();
		}
	}

	/**
	 * Acts on the "home" key event or equivalent event.
	 */
	@SuppressWarnings("serial")
	protected class GoHomeAction extends AbstractAction {
		protected GoHomeAction() {
			super("Go Home");
		}

		@SuppressWarnings({ "synthetic-access", "unused" })
		@Override
		public void actionPerformed(final ActionEvent e) {
			final VFSJFileChooser fc = BasicVFSFileChooserUI.this.getFileChooser();
			final FileObject currentDir = fc.getCurrentDirectoryObject();

			if (currentDir instanceof LocalFile) {
				BasicVFSFileChooserUI.this.changeDirectory(fc.getFileSystemView().getHomeDirectory());
			} else {
				try {
					BasicVFSFileChooserUI.this.changeDirectory(fc.getCurrentDirectoryObject().getFileSystem().getRoot());
				} catch (final FileSystemException ex) {
					BasicVFSFileChooserUI.this.log.error("Execption while changing directory:", ex);
				}
			}
		}
	}

	@SuppressWarnings("serial")
	protected class ChangeToParentDirectoryAction extends AbstractAction {
		protected ChangeToParentDirectoryAction() {
			super("Go Up");
			this.putValue(Action.ACTION_COMMAND_KEY, VFSFilePane.ACTION_CHANGE_TO_PARENT_DIRECTORY);
		}

		@Override
		@SuppressWarnings("unused")
		public void actionPerformed(final ActionEvent e) {
			final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

			if ((focusOwner == null) || !(focusOwner instanceof javax.swing.text.JTextComponent)) {
				BasicVFSFileChooserUI.this.getFileChooser().changeToParentDirectory();
			}
		}
	}

	/**
	 * Responds to an Open or Save request
	 */
	@SuppressWarnings("serial")
	protected class ApproveSelectionAction extends AbstractAction {
		protected ApproveSelectionAction() {
			super(VFSFilePane.ACTION_APPROVE_SELECTION);
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public void actionPerformed(final ActionEvent e) {
			if (BasicVFSFileChooserUI.this.isDirectorySelected()) {
				final FileObject dir = BasicVFSFileChooserUI.this.getDirectory();

				if (dir != null) {
					BasicVFSFileChooserUI.this.changeDirectory(dir);

					return;
				}
			}

			final VFSJFileChooser chooser = BasicVFSFileChooserUI.this.getFileChooser();

			String filename = BasicVFSFileChooserUI.this.getFileName();
			final AbstractVFSFileSystemView fs = chooser.getFileSystemView();
			final FileObject dir = chooser.getCurrentDirectoryObject();

			if (filename != null) {
				// Remove whitespace from beginning and end of filename
				filename = filename.trim();
			}

			if ((filename == null) || filename.equals("")) {
				// no file selected, multiple selection off, therefore cancel
				// the approve action
				BasicVFSFileChooserUI.this.resetGlobFilter();

				return;
			}

			FileObject selectedFile = null;
			FileObject[] selectedFiles = null;

			if (!filename.equals("")) {
				// Unix: Resolve '~' to user's home directory
				if (File.separatorChar == '/') {
					if (filename.startsWith("~/")) {
						filename = System.getProperty("user.home") + filename.substring(1);
					} else if (filename.equals("~")) {
						filename = System.getProperty("user.home");
					}
				}

				if (chooser.isMultiSelectionEnabled() && (filename.charAt(0) == '\"')) {
					final List<FileObject> fList = new ArrayList<FileObject>();

					filename = filename.substring(1);

					if (filename.endsWith("\"")) {
						filename = filename.substring(0, filename.length() - 1);
					}

					do {
						String str;
						final int i = filename.indexOf("\" \"");

						if (i > 0) {
							str = filename.substring(0, i);
							filename = filename.substring(i + 3);
						} else {
							str = filename;
							filename = "";
						}

                        // BEI: changed order of create with dir and create with fs because otherwise on sftp:// constructed file is local file instead of remote
                        FileObject file = fs.createFileObject(dir, str);

                        if (file == null) {
                            file = fs.createFileObject(str);
						}

						fList.add(file);
					} while (filename.length() > 0);

					if (!fList.isEmpty()) {
						selectedFiles = fList.toArray(new FileObject[fList.size()]);
					}

					BasicVFSFileChooserUI.this.resetGlobFilter();
				} else {
					/// ZOUNDI MARK
					selectedFile = fs.createFileObject(filename);

					if (!VFSUtils.exists(selectedFile)) {
						selectedFile = VFSUtils.resolveFileObject(BasicVFSFileChooserUI.this.getFileName());

						if ((selectedFile == null) || !VFSUtils.exists(selectedFile)) {
							selectedFile = fs.getChild(dir, filename);
						}
					}

					// check for wildcard pattern
					final AbstractVFSFileFilter currentFilter = chooser.getFileFilter();

					if (!VFSUtils.exists(selectedFile) && isGlobPattern(filename)) {
						BasicVFSFileChooserUI.this.changeDirectory(VFSUtils.getParentDirectory(selectedFile));

						if (BasicVFSFileChooserUI.this.globFilter == null) {
							BasicVFSFileChooserUI.this.globFilter = new GlobFilter();
						}

						try {
							BasicVFSFileChooserUI.this.globFilter.setPattern(selectedFile.getName().getBaseName());

							if (!(currentFilter instanceof GlobFilter)) {
								BasicVFSFileChooserUI.this.actualFileFilter = currentFilter;
							}

							chooser.setFileFilter(null);
							chooser.setFileFilter(BasicVFSFileChooserUI.this.globFilter);

							return;
						} catch (final PatternSyntaxException pse) {
							// Not a valid glob pattern. Abandon filter.
						}
					}

					BasicVFSFileChooserUI.this.resetGlobFilter();

					// Check for directory change action
					final boolean isDir = ((selectedFile != null) && VFSUtils.isDirectory(selectedFile));
					final boolean isTrav = ((selectedFile != null) && chooser.isTraversable(selectedFile));
					final boolean isDirSelEnabled = chooser.isDirectorySelectionEnabled();
					final boolean isFileSelEnabled = chooser.isFileSelectionEnabled();
					final boolean isCtrl = ((e != null) && ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0));

					if (isDir && isTrav && (isCtrl || !isDirSelEnabled)) {
						BasicVFSFileChooserUI.this.changeDirectory(selectedFile);

						return;
					} else if ((isDir || !isFileSelEnabled) && (!isDir || !isDirSelEnabled)
							&& (!isDirSelEnabled || VFSUtils.exists(selectedFile))) {
						selectedFile = null;
					}
				}
			}

			if ((selectedFiles != null) || (selectedFile != null)) {
				if ((selectedFiles != null) || chooser.isMultiSelectionEnabled()) {
					if (selectedFiles == null) {
						selectedFiles = new FileObject[] { selectedFile };
					}

					chooser.setSelectedFileObjects(selectedFiles);
					// Do it again. This is a fix for bug 4949273 to force the
					// selected value in case the ListSelectionModel clears it
					// for non-existing file names.
					chooser.setSelectedFileObjects(selectedFiles);
				} else {
					chooser.setSelectedFileObject(selectedFile);
				}

				chooser.approveSelection();
			} else {
				if (chooser.isMultiSelectionEnabled()) {
					chooser.setSelectedFileObjects(null);
				} else {
					chooser.setSelectedFile(null);
				}

				chooser.cancelSelection();
			}
		}
	}

	/*
	 * A file filter which accepts file patterns containing the special
	 * wildcards *? on Windows and *?[] on Unix.
	 */
	static class GlobFilter extends AbstractVFSFileFilter {
		String globPattern;

		public void setPattern(final String globPattern) {
			this.globPattern = globPattern;
		}

		@Override
		public boolean accept(final FileObject f) {
			if (f == null) {
				return false;
			}
			if (VFSUtils.isDirectory(f)) {
				return true;
			}
			return FilenameUtils.wildcardMatch(f.getName().getBaseName(), this.globPattern);
		}

		@Override
		public String getDescription() {
			return this.globPattern;
		}
	}

	/**
	 * Responds to a cancel request.
	 */
	@SuppressWarnings("serial")
	protected class CancelSelectionAction extends AbstractAction {

		@SuppressWarnings("unused")
		@Override
		public void actionPerformed(final ActionEvent e) {
			BasicVFSFileChooserUI.this.getFileChooser().cancelSelection();
		}
	}

	/**
	 * Rescans the files in the current directory
	 */
	@SuppressWarnings("serial")
	protected class UpdateAction extends AbstractAction {

		@SuppressWarnings("unused")
		@Override
		public void actionPerformed(final ActionEvent e) {
			final VFSJFileChooser fc = BasicVFSFileChooserUI.this.getFileChooser();
			fc.setCurrentDirectoryObject(fc.getFileSystemView().createFileObject(BasicVFSFileChooserUI.this.getDirectoryName()));
			fc.rescanCurrentDirectory();
		}
	}

	// *****************************************
	// ***** default AcceptAll file filter *****
	// *****************************************
	protected static class AcceptAllFileFilter extends AbstractVFSFileFilter {
		public AcceptAllFileFilter() {
		}

		@SuppressWarnings("unused")
		@Override
		public boolean accept(final FileObject f) {
			return true;
		}

		@Override
		public String getDescription() {
			return VFSResources.getMessage("VFSJFileChooser.acceptAllFileFilterText");
		}

		@Override
		public String toString() {
			return this.getDescription();
		}
	}

	// ***********************
	// * FileView operations *
	// ***********************
	public class BasicVFSFileView extends AbstractVFSFileView {
		/* FileView type descriptions */
		// PENDING(jeff) - pass in the icon cache size
		protected Map<FileObject, Icon> iconCache = new ConcurrentHashMap<FileObject, Icon>();

		public BasicVFSFileView() {
		}

		public void clearIconCache() {
			this.iconCache = null;
			this.iconCache = new ConcurrentHashMap<FileObject, Icon>();
		}

		@Override
		public String getName(final FileObject f) {
			// Note: Returns display name rather than file name
			String fileName = null;

			if (f != null) {
				fileName = f.getName().getBaseName();
			}

			if (fileName != null) {
				if (fileName.trim().equals("")) {
					fileName = (f != null) ? f.getName().toString() : null;
				}
			}

			return fileName;
		}

		@Override
		public String getDescription(final FileObject f) {
			return f.getName().getBaseName();
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public String getTypeDescription(final FileObject f) {
			String type = BasicVFSFileChooserUI.this.getFileChooser().getFileSystemView().getSystemTypeDescription(f);

			if (type == null) {
				if (VFSUtils.isDirectory(f)) {
					type = BasicVFSFileChooserUI.this.directoryDescriptionText;
				} else {
					type = BasicVFSFileChooserUI.this.fileDescriptionText;
				}
			}

			return type;
		}

		public Icon getCachedIcon(final FileObject f) {
			return this.iconCache.get(f);
		}

		public void cacheIcon(final FileObject f, final Icon i) {
			if ((f == null) || (i == null)) {
				return;
			}

			this.iconCache.put(f, i);
		}

		@Override
		public Icon getIcon(final FileObject f) {
			Icon icon = this.getCachedIcon(f);

			if (icon != null) {
				return icon;
			}

			icon = BasicVFSFileChooserUI.this.fileIcon;

			if (f != null) {
				final AbstractVFSFileSystemView fsv = BasicVFSFileChooserUI.this.getFileChooser().getFileSystemView();

				if (fsv.isFloppyDrive(f)) {
					icon = BasicVFSFileChooserUI.this.floppyDriveIcon;
				} else if (fsv.isDrive(f)) {
					icon = BasicVFSFileChooserUI.this.hardDriveIcon;
				} else if (fsv.isComputerNode(f)) {
					icon = BasicVFSFileChooserUI.this.computerIcon;
				} else if (VFSUtils.isDirectory(f)) {
					icon = BasicVFSFileChooserUI.this.directoryIcon;
				}
			}

			this.cacheIcon(f, icon);

			return icon;
		}

		@SuppressWarnings("boxing")
		public Boolean isHidden(final FileObject f) {
			return VFSUtils.isHiddenFile(f);
		}
	}
}
// CHECKSTYLE:ON
