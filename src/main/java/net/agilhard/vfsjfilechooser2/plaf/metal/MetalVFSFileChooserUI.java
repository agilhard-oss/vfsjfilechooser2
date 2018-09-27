// CHECKSTYLE:OFF

/*
 * The MetalFileChooserUI implementation using commons-VFS
 * based on Swing MetalFileChooserUI
 *
 * Copyright (C) 2005-2008 Yves Zoundi
 * Copyright (C) 2012 University of Waikato, Hamilton, NZ
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
package net.agilhard.vfsjfilechooser2.plaf.metal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;

import net.agilhard.vfsjfilechooser2.VFSJFileChooser;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import net.agilhard.vfsjfilechooser2.constants.VFSJFileChooserConstants;
import net.agilhard.vfsjfilechooser2.filechooser.AbstractVFSFileFilter;
import net.agilhard.vfsjfilechooser2.filechooser.AbstractVFSFileSystemView;
import net.agilhard.vfsjfilechooser2.filechooser.PopupHandler;
import net.agilhard.vfsjfilechooser2.filepane.VFSFilePane;
import net.agilhard.vfsjfilechooser2.plaf.VFSFileChooserUIAccessorIF;
import net.agilhard.vfsjfilechooser2.plaf.basic.BasicVFSDirectoryModel;
import net.agilhard.vfsjfilechooser2.plaf.basic.BasicVFSFileChooserUI;
import net.agilhard.vfsjfilechooser2.utils.VFSResources;
import net.agilhard.vfsjfilechooser2.utils.VFSUtils;

/**
 * <p>
 * The MetalFileChooserUI implementation using commons-VFS based on Swing
 * MetalFileChooserUI
 * </p>
 *
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 * @author Stephan Schuster <stephanschuster at users.sourceforge.net>
 * @version 0.0.1
 */
public class MetalVFSFileChooserUI extends BasicVFSFileChooserUI {

	private static final Dimension hstrut5 = new Dimension(5, 1);

	// private static final Dimension hstrut11 = new Dimension(11, 1);
	private static final Dimension vstrut5 = new Dimension(1, 5);

	private static final Insets shrinkwrap = new Insets(0, 0, 0, 0);

	// Preferred and Minimum sizes for the dialog box
	private static int PREF_WIDTH = 500;

	private static int PREF_HEIGHT = 326;

	private static Dimension PREF_SIZE = new Dimension(PREF_WIDTH, PREF_HEIGHT);

	private static int MIN_WIDTH = 500;

	private static int MIN_HEIGHT = 326;

	private static Dimension MIN_SIZE = new Dimension(MIN_WIDTH, MIN_HEIGHT);

	private static int LIST_PREF_WIDTH = 405;

	private static int LIST_PREF_HEIGHT = 135;

	private static Dimension LIST_PREF_SIZE = new Dimension(LIST_PREF_WIDTH, LIST_PREF_HEIGHT);

	private JLabel lookInLabel;

	@SuppressWarnings("rawtypes")
	private JComboBox directoryComboBox;

	private DirectoryComboBoxModel directoryComboBoxModel;

	private final Action directoryComboBoxAction = new DirectoryComboBoxAction();

	private FilterComboBoxModel filterComboBoxModel;

	private JTextField fileNameTextField;

	private VFSFilePane filePane;

	private JToggleButton listViewButton;

	private JToggleButton detailsViewButton;

	private boolean useShellFolder;

	private JButton approveButton;

	private JButton cancelButton;

	private JPanel buttonPanel;

	private JPanel bottomPanel;

	@SuppressWarnings("rawtypes")
	private JComboBox filterComboBox;

	// Labels, mnemonics, and tooltips (oh my!)
	private String lookInLabelText = null;

	private String saveInLabelText = null;

	private String fileNameLabelText = null;

	private String folderNameLabelText = null;

	private String filesOfTypeLabelText = null;

	private String upFolderToolTipText = null;

	private String upFolderAccessibleName = null;

	private String homeFolderToolTipText = null;

	private String homeFolderAccessibleName = null;

	private String newFolderToolTipText = null;

	private String newFolderAccessibleName = null;

	private String listViewButtonToolTipText = null;

	private String listViewButtonAccessibleName = null;

	private String detailsViewButtonToolTipText = null;

	private String detailsViewButtonAccessibleName = null;

	private final VFSJFileChooser chooser;

	private AlignedLabel fileNameLabel;

	private JPanel topButtonPanel;

	private JButton upFolderButton;

	private JButton homeFolderButton;

	private JButton newFolderButton;

	public MetalVFSFileChooserUI(final VFSJFileChooser filechooser) {
		super(filechooser);
		chooser = filechooser;
	}

	private void populateFileNameLabel() {
		if (getFileChooser().getFileSelectionMode() == VFSJFileChooser.SELECTION_MODE.DIRECTORIES_ONLY) {
			fileNameLabel.setText(folderNameLabelText);
		} else {
			fileNameLabel.setText(fileNameLabelText);
		}
	}

	//
	// ComponentUI Interface Implementation methods
	//
	public static ComponentUI createUI(final JComponent c) {
		final ComponentUI mui = new MetalVFSFileChooserUI((VFSJFileChooser) c);

		return mui;
	}

	@Override
	public void installUI(final JComponent c) {
		super.installUI(c);
	}

	@Override
	public void uninstallComponents(final VFSJFileChooser fc) {
		fc.removeAll();
		bottomPanel = null;
		buttonPanel = null;
	}

	public JPanel getNavigationButtonsPanel() {
		return topButtonPanel;
	}

	public JButton getUpFolderButton() {
		return upFolderButton;
	}

	public JButton getHomeFolderButton() {
		return homeFolderButton;
	}

	public JButton getNewFolderButton() {
		return newFolderButton;
	}

	@SuppressWarnings({ "serial", "rawtypes", "unchecked" })
	@Override
	public void installComponents(final VFSJFileChooser fc) {
		final AbstractVFSFileSystemView fsv = fc.getFileSystemView();

		fc.setBorder(new EmptyBorder(12, 12, 11, 11));
		fc.setLayout(new BorderLayout(0, 11));

		filePane = new VFSFilePane(new MetalVFSFileChooserUIAccessor());
		fc.addPropertyChangeListener(filePane);

		updateUseShellFolder();

		// ********************************* //
		// **** Construct the top panel **** //
		// ********************************* //

		// Directory manipulation buttons
		final JPanel topPanel = new JPanel(new BorderLayout(11, 0));
		topButtonPanel = new JPanel();
		topButtonPanel.setLayout(new BoxLayout(topButtonPanel, BoxLayout.LINE_AXIS));
		topPanel.add(topButtonPanel, BorderLayout.AFTER_LINE_ENDS);

		// Add the top panel to the fileChooser
		fc.add(topPanel, BorderLayout.NORTH);

		// ComboBox Label
		lookInLabel = new JLabel(lookInLabelText);
		topPanel.add(lookInLabel, BorderLayout.BEFORE_LINE_BEGINS);

		// CurrentDir ComboBox
		directoryComboBox = new JComboBox() {

			@Override
			public Dimension getPreferredSize() {
				final Dimension d = super.getPreferredSize();
				// Must be small enough to not affect total width.
				d.width = 150;

				return d;
			}
		};
		directoryComboBox.putClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY, lookInLabelText);
		directoryComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
		lookInLabel.setLabelFor(directoryComboBox);
		directoryComboBoxModel = createDirectoryComboBoxModel(fc);
		directoryComboBox.setModel(directoryComboBoxModel);
		directoryComboBox.addActionListener(directoryComboBoxAction);
		directoryComboBox.setRenderer(createDirectoryComboBoxRenderer(fc));
		directoryComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		directoryComboBox.setAlignmentY(Component.TOP_ALIGNMENT);
		directoryComboBox.setMaximumRowCount(8);

		topPanel.add(directoryComboBox, BorderLayout.CENTER);

		// Up Button
		upFolderButton = new JButton(getChangeToParentDirectoryAction());
		upFolderButton.setText(null);
		upFolderButton.setIcon(upFolderIcon);
		upFolderButton.setToolTipText(upFolderToolTipText);
		upFolderButton.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, upFolderAccessibleName);
		upFolderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		upFolderButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		upFolderButton.setMargin(shrinkwrap);

		topButtonPanel.add(upFolderButton);
		topButtonPanel.add(Box.createRigidArea(hstrut5));

		// Home Button
		final FileObject homeDir = fsv.getHomeDirectory();
		String toolTipText = homeFolderToolTipText;

		if (fsv.isRoot(homeDir)) {
			toolTipText = getFileView(fc).getName(homeDir); // Probably
															// "Desktop".
		}

		JButton b = new JButton(homeFolderIcon);
		b.setToolTipText(toolTipText);
		b.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, homeFolderAccessibleName);
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		b.setAlignmentY(Component.CENTER_ALIGNMENT);
		b.setMargin(shrinkwrap);

		b.addActionListener(getGoHomeAction());
		topButtonPanel.add(b);
		topButtonPanel.add(Box.createRigidArea(hstrut5));

		// New Directory Button
		if (!UIManager.getBoolean("FileChooser.readOnly")) {
			b = new JButton(filePane.getNewFolderAction());
			b.setText(null);
			b.setIcon(newFolderIcon);
			b.setToolTipText(newFolderToolTipText);
			b.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, newFolderAccessibleName);
			b.setAlignmentX(Component.LEFT_ALIGNMENT);
			b.setAlignmentY(Component.CENTER_ALIGNMENT);
			b.setMargin(shrinkwrap);
		}

		topButtonPanel.add(b);
		topButtonPanel.add(Box.createRigidArea(hstrut5));

		// View button group
		final ButtonGroup viewButtonGroup = new ButtonGroup();

		// List Button
		listViewButton = new JToggleButton(listViewIcon);
		listViewButton.setToolTipText(listViewButtonToolTipText);
		listViewButton.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, listViewButtonAccessibleName);
		listViewButton.setSelected(true);
		listViewButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		listViewButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		listViewButton.setMargin(shrinkwrap);
		listViewButton.addActionListener(filePane.getViewTypeAction(VFSFilePane.VIEWTYPE_LIST));
		topButtonPanel.add(listViewButton);
		viewButtonGroup.add(listViewButton);

		// Details Button
		detailsViewButton = new JToggleButton(detailsViewIcon);
		detailsViewButton.setToolTipText(detailsViewButtonToolTipText);
		detailsViewButton.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
				detailsViewButtonAccessibleName);
		detailsViewButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		detailsViewButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		detailsViewButton.setMargin(shrinkwrap);
		detailsViewButton.addActionListener(filePane.getViewTypeAction(VFSFilePane.VIEWTYPE_DETAILS));
		topButtonPanel.add(detailsViewButton);
		viewButtonGroup.add(detailsViewButton);
		filePane.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			@SuppressWarnings("synthetic-access")
			public void propertyChange(final PropertyChangeEvent e) {
				if ("viewType".equals(e.getPropertyName())) {
					final int viewType = filePane.getViewType();

					if (viewType == VFSFilePane.VIEWTYPE_LIST) {
						listViewButton.setSelected(true);
					} else if (viewType == VFSFilePane.VIEWTYPE_DETAILS) {
						detailsViewButton.setSelected(true);
					}
				}
			}
		});

		// ************************************** //
		// ******* Add the directory pane ******* //
		// ************************************** //
		fc.add(getAccessoryPanel(), BorderLayout.AFTER_LINE_ENDS);

		final JComponent accessory = fc.getAccessory();

		if (accessory != null) {
			getAccessoryPanel().add(accessory);
		}

		filePane.setPreferredSize(LIST_PREF_SIZE);
		fc.add(filePane, BorderLayout.CENTER);

		// ********************************** //
		// **** Construct the bottom panel ** //
		// ********************************** //
		bottomPanel = getBottomPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
		fc.add(bottomPanel, BorderLayout.SOUTH);

		// FileName label and textfield
		final JPanel fileNamePanel = new JPanel();
		fileNamePanel.setLayout(new BoxLayout(fileNamePanel, BoxLayout.LINE_AXIS));
		bottomPanel.add(fileNamePanel);
		bottomPanel.add(Box.createRigidArea(vstrut5));

		fileNameLabel = new AlignedLabel();
		populateFileNameLabel();
		fileNamePanel.add(fileNameLabel);

		fileNameTextField = new JTextField(35) {

			@Override
			public Dimension getMaximumSize() {
				return new Dimension(Short.MAX_VALUE, super.getPreferredSize().height);
			}
		};

		PopupHandler.installDefaultMouseListener(fileNameTextField);

		fileNamePanel.add(fileNameTextField);
		fileNameLabel.setLabelFor(fileNameTextField);
		fileNameTextField.addFocusListener(new FocusAdapter() {

			@SuppressWarnings({ "unused", "synthetic-access" })
			@Override
			public void focusGained(final FocusEvent e) {
				if (!MetalVFSFileChooserUI.this.getFileChooser().isMultiSelectionEnabled()) {
					filePane.clearSelection();
				}
			}
		});

		if (fc.isMultiSelectionEnabled()) {
			setFileName(this.fileNameString(fc.getSelectedFileObjects()));
		} else {
			setFileName(this.fileNameString(fc.getSelectedFileObject()));
		}

		// Filetype label and combobox
		final JPanel filesOfTypePanel = new JPanel();
		filesOfTypePanel.setLayout(new BoxLayout(filesOfTypePanel, BoxLayout.LINE_AXIS));
		bottomPanel.add(filesOfTypePanel);

		final AlignedLabel filesOfTypeLabel = new AlignedLabel(filesOfTypeLabelText);
		filesOfTypePanel.add(filesOfTypeLabel);

		filterComboBoxModel = createFilterComboBoxModel();
		fc.addPropertyChangeListener(filterComboBoxModel);
		filterComboBox = new JComboBox(filterComboBoxModel);
		filterComboBox.putClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY, filesOfTypeLabelText);
		filesOfTypeLabel.setLabelFor(filterComboBox);
		filterComboBox.setRenderer(createFilterComboBoxRenderer());
		filesOfTypePanel.add(filterComboBox);

		// buttons
		getButtonPanel().setLayout(new ButtonAreaLayout());

		approveButton = new JButton(getApproveButtonText(fc));
		// Note: Metal does not use mnemonics for approve and cancel
		approveButton.addActionListener(getApproveSelectionAction());
		fileNameTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					MetalVFSFileChooserUI.this.getApproveSelectionAction().actionPerformed(null);
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					MetalVFSFileChooserUI.this.getFileChooser().cancelSelection();
				}
			}
		});
		approveButton.setToolTipText(getApproveButtonToolTipText(fc));
		getButtonPanel().add(approveButton);

		cancelButton = new JButton(cancelButtonText);
		cancelButton.setToolTipText(cancelButtonToolTipText);
		cancelButton.addActionListener(getCancelSelectionAction());
		getButtonPanel().add(cancelButton);

		if (fc.getControlButtonsAreShown()) {
			addControlButtons();
		}

		groupLabels(new AlignedLabel[] { fileNameLabel, filesOfTypeLabel });
	}

	private void updateUseShellFolder() {
		// Decide whether to use the ShellFolder class to populate shortcut
		// panel and combobox.
		final VFSJFileChooser fc = getFileChooser();
		final Boolean prop = (Boolean) fc.getClientProperty("FileChooser.useShellFolder");

		if (prop != null) {
			useShellFolder = prop.booleanValue();
		} else {
			useShellFolder = fc.getFileSystemView().equals(AbstractVFSFileSystemView.getFileSystemView());
		}
	}

	protected JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
		}

		return buttonPanel;
	}

	protected JPanel getBottomPanel() {
		if (bottomPanel == null) {
			bottomPanel = new JPanel();
		}

		return bottomPanel;
	}

	@Override
	protected void installStrings(final VFSJFileChooser fc) {
		super.installStrings(fc);

		lookInLabelText = VFSResources.getMessage("VFSJFileChooser.lookInLabelText");
		saveInLabelText = VFSResources.getMessage("VFSJFileChooser.saveInLabelText");

		fileNameLabelText = VFSResources.getMessage("VFSJFileChooser.fileNameLabelText");

		folderNameLabelText = fileNameLabelText;

		filesOfTypeLabelText = VFSResources.getMessage("VFSJFileChooser.filesOfTypeLabelText");

		upFolderToolTipText = VFSResources.getMessage("VFSJFileChooser.upFolderToolTipText");
		upFolderAccessibleName = VFSResources.getMessage("VFSJFileChooser.upFolderAccessibleName");

		homeFolderToolTipText = VFSResources.getMessage("VFSJFileChooser.homeFolderToolTipText");
		homeFolderAccessibleName = VFSResources.getMessage("VFSJFileChooser.homeFolderAccessibleName");

		newFolderToolTipText = VFSResources.getMessage("VFSJFileChooser.newFolderToolTipText");
		newFolderAccessibleName = VFSResources.getMessage("VFSJFileChooser.newFolderAccessibleName");

		listViewButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.listViewButtonToolTipText");
		listViewButtonAccessibleName = VFSResources.getMessage("VFSJFileChooser.listViewButtonAccessibleName");

		detailsViewButtonToolTipText = VFSResources.getMessage("VFSJFileChooser.detailsViewButtonToolTipText");
		detailsViewButtonAccessibleName = VFSResources.getMessage("VFSJFileChooser.detailsViewButtonAccessibleName");
	}

	@Override
	protected void installListeners(final VFSJFileChooser fc) {
		super.installListeners(fc);

		final ActionMap actionMap = getActionMap();
		SwingUtilities.replaceUIActionMap(fc, actionMap);
	}

	@Override
	protected ActionMap getActionMap() {
		return createActionMap();
	}

	@Override
	protected ActionMap createActionMap() {
		final ActionMap map = new ActionMapUIResource();
		VFSFilePane.addActionsToMap(map, filePane.getActions());

		return map;
	}

	@SuppressWarnings("unused")
	protected JPanel createList(final VFSJFileChooser fc) {
		return filePane.createList();
	}

	@SuppressWarnings("unused")
	protected JPanel createDetailsView(final VFSJFileChooser fc) {
		return filePane.createDetailsView();
	}

	/**
	 * Creates a selection listener for the list of files and directories.
	 *
	 * @param fc
	 *            a <code>VFSJFileChooser</code>
	 * @return a <code>ListSelectionListener</code>
	 */
	@Override
	public ListSelectionListener createListSelectionListener(final VFSJFileChooser fc) {
		return super.createListSelectionListener(fc);
	}

	@Override
	public void uninstallUI(final JComponent c) {
		// Remove listeners
		c.removePropertyChangeListener(filterComboBoxModel);
		c.removePropertyChangeListener(filePane);
		cancelButton.removeActionListener(getCancelSelectionAction());
		approveButton.removeActionListener(getApproveSelectionAction());
		fileNameTextField.removeActionListener(getApproveSelectionAction());

		if (filePane != null) {
			filePane.uninstallUI();
			filePane = null;
		}

		super.uninstallUI(c);
	}

	/**
	 * Returns the preferred size of the specified <code>VFSJFileChooser</code>.
	 * The preferred size is at least as large, in both height and width, as the
	 * preferred size recommended by the file chooser's layout manager.
	 *
	 * @param c
	 *            a <code>VFSJFileChooser</code>
	 * @return a <code>Dimension</code> specifying the preferred width and
	 *         height of the file chooser
	 */
	@Override
	public Dimension getPreferredSize(final JComponent c) {
		final int prefWidth = PREF_SIZE.width;
		final Dimension d = c.getLayout().preferredLayoutSize(c);

		if (d != null) {
			return new Dimension((d.width < prefWidth) ? prefWidth : d.width,
					(d.height < PREF_SIZE.height) ? PREF_SIZE.height : d.height);
		}
		return new Dimension(prefWidth, PREF_SIZE.height);

	}

	/**
	 * Returns the minimum size of the <code>VFSJFileChooser</code>.
	 *
	 * @param c
	 *            a <code>VFSJFileChooser</code>
	 * @return a <code>Dimension</code> specifying the minimum width and height
	 *         of the file chooser
	 */
	@SuppressWarnings("unused")
	@Override
	public Dimension getMinimumSize(final JComponent c) {
		return MIN_SIZE;
	}

	/**
	 * Returns the maximum size of the <code>VFSJFileChooser</code>.
	 *
	 * @param c
	 *            a <code>VFSJFileChooser</code>
	 * @return a <code>Dimension</code> specifying the maximum width and height
	 *         of the file chooser
	 */
	@SuppressWarnings("unused")
	@Override
	public Dimension getMaximumSize(final JComponent c) {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	private String fileNameString(final FileObject fileObject) {
		if (fileObject == null) {
			return null;
		}
		final VFSJFileChooser fc = getFileChooser();

		if ((fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()) || (fc.isDirectorySelectionEnabled()
				&& fc.isFileSelectionEnabled() && fc.getFileSystemView().isFileSystemRoot(fileObject))) {
			String url = null;

			try {
				url = fileObject.getURL().toExternalForm();
			} catch (final FileSystemException ex) {
				ex.printStackTrace();
			}

			return url;
		}
		return fileObject.getName().getBaseName();
	}

	private String fileNameString(final FileObject[] files) {
		final StringBuilder buf = new StringBuilder();

		final int fileCount = files.length;

		for (int i = 0; (i < fileCount); i++) {
			if (i > 0) {
				buf.append(" ");
			}

			if (fileCount > 1) {
				buf.append("\"");
			}

			buf.append(this.fileNameString(files[i]));

			if (fileCount > 1) {
				buf.append("\"");
			}
		}

		return buf.toString();
	}

	/* The following methods are used by the PropertyChange Listener */
	private void doSelectedFileChanged(final PropertyChangeEvent e) {
		final FileObject f = (FileObject) e.getNewValue();
		final VFSJFileChooser fc = getFileChooser();

		if ((f != null) && ((fc.isFileSelectionEnabled() && !VFSUtils.isDirectory(f))
				|| (VFSUtils.isDirectory(f) && fc.isDirectorySelectionEnabled()))) {
			setFileName(this.fileNameString(f));
		}
	}

	private void doSelectedFilesChanged(final PropertyChangeEvent e) {
		final FileObject[] files = (FileObject[]) e.getNewValue();
		final VFSJFileChooser fc = getFileChooser();

		if ((files != null) && (files.length > 0)
				&& ((files.length > 1) || fc.isDirectorySelectionEnabled() || !VFSUtils.isDirectory(files[0]))) {
			setFileName(this.fileNameString(files));
		}
	}

	@SuppressWarnings({ "synthetic-access", "unused" })
	private void doDirectoryChanged(final PropertyChangeEvent e) {
		final VFSJFileChooser fc = getFileChooser();
		final AbstractVFSFileSystemView fsv = fc.getFileSystemView();

		clearIconCache();

		final FileObject currentDirectory = fc.getCurrentDirectoryObject();

		if (currentDirectory != null) {
			directoryComboBoxModel.addItem(currentDirectory);
			directoryComboBox.setSelectedItem(currentDirectory);
			fc.setCurrentDirectoryObject(currentDirectory);

			if (fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()) {
				if (fsv.isFileSystem(currentDirectory)) {
					String url = null;

					try {
						url = currentDirectory.getURL().toExternalForm();
					} catch (final FileSystemException e1) {
						e1.printStackTrace();
					}

					setFileName(url);
				} else {
					setFileName(null);
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void doFilterChanged(final PropertyChangeEvent e) {
		clearIconCache();
	}

	@SuppressWarnings("unused")
	private void doFileSelectionModeChanged(final PropertyChangeEvent e) {
		if (fileNameLabel != null) {
			populateFileNameLabel();
		}

		clearIconCache();

		final VFSJFileChooser fc = getFileChooser();
		final FileObject currentDirectory = fc.getCurrentDirectoryObject();

		if ((currentDirectory != null) && fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()
				&& fc.getFileSystemView().isFileSystem(currentDirectory)) {
			String url = null;

			try {
				url = currentDirectory.getURL().toExternalForm();
			} catch (final FileSystemException e1) {
				e1.printStackTrace();
			}

			setFileName(url);
		} else {
			setFileName(null);
		}
	}

	private void doAccessoryChanged(final PropertyChangeEvent e) {
		if (getAccessoryPanel() != null) {
			if (e.getOldValue() != null) {
				getAccessoryPanel().remove((JComponent) e.getOldValue());
			}

			final JComponent accessory = (JComponent) e.getNewValue();

			if (accessory != null) {
				getAccessoryPanel().add(accessory, BorderLayout.CENTER);
			}
		}
	}

	@SuppressWarnings("unused")
	private void doApproveButtonTextChanged(final PropertyChangeEvent e) {
		final VFSJFileChooser m_chooser = getFileChooser();
		approveButton.setText(getApproveButtonText(m_chooser));
		approveButton.setToolTipText(getApproveButtonToolTipText(m_chooser));
	}

	@SuppressWarnings("unused")
	private void doDialogTypeChanged(final PropertyChangeEvent e) {
		final VFSJFileChooser m_chooser = getFileChooser();
		approveButton.setText(getApproveButtonText(m_chooser));
		approveButton.setToolTipText(getApproveButtonToolTipText(m_chooser));

		if (m_chooser.getDialogType() == VFSJFileChooser.DIALOG_TYPE.SAVE) {
			lookInLabel.setText(saveInLabelText);
		} else {
			lookInLabel.setText(lookInLabelText);
		}
	}

	@SuppressWarnings("unused")
	private void doApproveButtonMnemonicChanged(final PropertyChangeEvent e) {
		// Note: Metal does not use mnemonics for approve and cancel
	}

	@SuppressWarnings("unused")
	private void doControlButtonsChanged(final PropertyChangeEvent e) {
		if (getFileChooser().getControlButtonsAreShown()) {
			addControlButtons();
		} else {
			removeControlButtons();
		}
	}

	/*
	 * Listen for filechooser property changes, such as the selected file
	 * changing, or the type of the dialog changing.
	 */
	@SuppressWarnings("unused")
	@Override
	public PropertyChangeListener createPropertyChangeListener(final VFSJFileChooser fc) {
		return new PropertyChangeListener() {

			@Override
			@SuppressWarnings("synthetic-access")
			public void propertyChange(final PropertyChangeEvent e) {
				final String s = e.getPropertyName();

				if (s.equals(VFSJFileChooserConstants.SELECTED_FILE_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doSelectedFileChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.SELECTED_FILES_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doSelectedFilesChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.DIRECTORY_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doDirectoryChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.FILE_FILTER_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doFilterChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.FILE_SELECTION_MODE_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doFileSelectionModeChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.ACCESSORY_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doAccessoryChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY)
						|| s.equals(VFSJFileChooserConstants.APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doApproveButtonTextChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.DIALOG_TYPE_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doDialogTypeChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doApproveButtonMnemonicChanged(e);
				} else if (s.equals(VFSJFileChooserConstants.CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY)) {
					MetalVFSFileChooserUI.this.doControlButtonsChanged(e);
				} else if (s.equals("componentOrientation")) {
					final ComponentOrientation o = (ComponentOrientation) e.getNewValue();
					final VFSJFileChooser cc = (VFSJFileChooser) e.getSource();

					if (o != (ComponentOrientation) e.getOldValue()) {
						cc.applyComponentOrientation(o);
					}
				} else if (s.equals("FileChooser.useShellFolder")) {
					MetalVFSFileChooserUI.this.updateUseShellFolder();
					MetalVFSFileChooserUI.this.doDirectoryChanged(e);
				} else if (s.equals("ancestor")) {
					if ((e.getOldValue() == null) && (e.getNewValue() != null)) {
						// Ancestor was added, set initial focus
						fileNameTextField.selectAll();
						fileNameTextField.requestFocus();
					}
				}
			}
		};
	}

	/**
	 *
	 */
	protected void removeControlButtons() {
		getBottomPanel().remove(getButtonPanel());
	}

	/**
	 *
	 */
	protected void addControlButtons() {
		getBottomPanel().add(getButtonPanel());
	}

	@Override
	public void ensureFileIsVisible(final VFSJFileChooser fc, final FileObject f) {
		filePane.ensureFileIsVisible(fc, f);
	}

	@SuppressWarnings("unused")
	@Override
	public void rescanCurrentDirectory(final VFSJFileChooser fc) {
		filePane.rescanCurrentDirectory();
	}

	@Override
	public String getFileName() {
		if (fileNameTextField != null) {
			return fileNameTextField.getText();
		}
		return null;

	}

	@Override
	public void setFileName(final String filename) {
		if (fileNameTextField != null) {
			fileNameTextField.setText(filename);
		}
	}

	/**
	 * Property to remember whether a directory is currently selected in the UI.
	 * This is normally called by the UI on a selection event.
	 *
	 * @param directorySelected
	 *            if a directory is currently selected.
	 * @since 1.4
	 */
	@Override
	protected void setDirectorySelected(final boolean directorySelected) {
		super.setDirectorySelected(directorySelected);

		if (directorySelected) {
			if (approveButton != null) {
				approveButton.setText(directoryOpenButtonText);
				approveButton.setToolTipText(directoryOpenButtonToolTipText);
			}
		} else {
			if (approveButton != null) {
				approveButton.setText(getApproveButtonText(chooser));
				approveButton.setToolTipText(getApproveButtonToolTipText(chooser));
			}
		}
	}

	@Override
	public String getDirectoryName() {
		// PENDING(jeff) - get the name from the directory combobox
		return null;
	}

	@SuppressWarnings({ "unused" })
	@Override
	public void setDirectoryName(final String dirname) {
		// PENDING(jeff) - set the name in the directory combobox
	}

	/**
	 * @param fc
	 * @return
	 */
	@SuppressWarnings({ "synthetic-access", "unused", "rawtypes" })
	protected ListCellRenderer createDirectoryComboBoxRenderer(final VFSJFileChooser fc) {
		return new DirectoryComboBoxRenderer();
	}

	//
	// DataModel for DirectoryComboxbox
	//
	/**
	 * @param fc
	 * @return
	 */
	@SuppressWarnings("unused")
	protected DirectoryComboBoxModel createDirectoryComboBoxModel(final VFSJFileChooser fc) {
		return new DirectoryComboBoxModel();
	}

	//
	// Renderer for Types ComboBox
	//
	@SuppressWarnings("rawtypes")
	protected ListCellRenderer createFilterComboBoxRenderer() {
		return new FilterComboBoxRenderer();
	}

	//
	// DataModel for Types Comboxbox
	//
	protected FilterComboBoxModel createFilterComboBoxModel() {
		return new FilterComboBoxModel();
	}

	/**
	 * @param e
	 */
	public void valueChanged(final ListSelectionEvent e) {
		final VFSJFileChooser fc = getFileChooser();
		final FileObject f = fc.getSelectedFileObject();

		if (!e.getValueIsAdjusting() && (f != null) && !getFileChooser().isTraversable(f)) {
			setFileName(this.fileNameString(f));
		}
	}

	@SuppressWarnings("unused")
	@Override
	protected JButton getApproveButton(final VFSJFileChooser fc) {
		return approveButton;
	}

	private static void groupLabels(final AlignedLabel[] group) {
		for (final AlignedLabel grp : group) {
			grp.group = group;
		}
	}

	/**
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public ComboBoxModel getCombo() {
		return directoryComboBoxModel;
	}

	final class MetalVFSFileChooserUIAccessor implements VFSFileChooserUIAccessorIF {

		@Override
		@SuppressWarnings("synthetic-access")
		public VFSJFileChooser getFileChooser() {
			return chooser;
		}

		@Override
		public BasicVFSDirectoryModel getModel() {
			return MetalVFSFileChooserUI.this.getModel();
		}

		@Override
		public JPanel createList() {
			return MetalVFSFileChooserUI.this.createList(getFileChooser());
		}

		@Override
		public JPanel createDetailsView() {
			return MetalVFSFileChooserUI.this.createDetailsView(getFileChooser());
		}

		@SuppressWarnings({ "synthetic-access" })
		@Override
		public boolean isDirectorySelected() {
			return MetalVFSFileChooserUI.this.isDirectorySelected();
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public FileObject getDirectory() {
			return MetalVFSFileChooserUI.this.getDirectory();
		}

		@Override
		public Action getChangeToParentDirectoryAction() {
			return MetalVFSFileChooserUI.this.getChangeToParentDirectoryAction();
		}

		@Override
		public Action getApproveSelectionAction() {
			return MetalVFSFileChooserUI.this.getApproveSelectionAction();
		}

		@Override
		public Action getNewFolderAction() {
			return MetalVFSFileChooserUI.this.getNewFolderAction();
		}

		@Override
		@SuppressWarnings({ "synthetic-access", "rawtypes" })
		public MouseListener createDoubleClickListener(final JList list) {
			return MetalVFSFileChooserUI.this.createDoubleClickListener(getFileChooser(), list);
		}

		@Override
		public ListSelectionListener createListSelectionListener() {
			return MetalVFSFileChooserUI.this.createListSelectionListener(getFileChooser());
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public boolean usesShellFolder() {
			return useShellFolder;
		}
	}

	//
	// Renderer for DirectoryComboBox
	//
	@SuppressWarnings("serial")
	private final class DirectoryComboBoxRenderer extends DefaultListCellRenderer {

		IndentIcon ii = null; // new IndentIcon();

		@SuppressWarnings({ "rawtypes", "synthetic-access" })
		@Override
		public Component getListCellRendererComponent(final JList list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value == null) {
				setText("");

				return this;
			}

			final FileObject directory = (FileObject) value;
			final String texte = VFSUtils.getFriendlyName(getFileChooser().getName(directory));
			setText(texte);

			final Icon icon = getFileChooser().getIcon(directory);

			ii = new IndentIcon(icon);
			ii.depth = directoryComboBoxModel.getDepth(index);

			setIcon(ii);

			return this;
		}
	}

	/**
	 * Data model for a type-face selection combo-box.
	 */
	@SuppressWarnings({ "serial", "rawtypes" })
	public final class DirectoryComboBoxModel extends AbstractListModel implements ComboBoxModel {

		List<FileObject> directories = new CopyOnWriteArrayList<FileObject>();

		int[] depths = null;

		FileObject selectedDirectory = null;

		@SuppressWarnings("synthetic-access")
		AbstractVFSFileSystemView fsv = chooser.getFileSystemView();

		public DirectoryComboBoxModel() {
			// Add the current directory to the model, and make it the
			// selectedDirectory
			final FileObject dir = getFileChooser().getCurrentDirectoryObject();

			if (dir != null) {
				addItem(dir);
			}
		}

		/**
		 * Adds the directory to the model and sets it to be selected,
		 * additionally clears out the previous selected directory and the paths
		 * leading up to it, if any.
		 */
		private void addItem(final FileObject directory) {
			if (directory == null) {
				return;
			}

			directories.clear();

			FileObject[] baseFolders;

			baseFolders = fsv.getRoots(directory);

			directories.addAll(Arrays.asList(baseFolders));

			// Get the canonical (full) path. This has the side
			// benefit of removing extraneous chars from the path,
			// for example /foo/bar/ becomes /foo/bar
			// FileObject canonical = directory;

			// create FileObject instances of each directory leading up to the
			// top
			try {
				FileObject f = directory;
				final List<FileObject> path = new ArrayList<FileObject>(10);

				do {
					path.add(f);
				} while ((f = VFSUtils.getParentDirectory(f)) != null);

				final int pathCount = path.size();

				// Insert chain at appropriate place in vector
				for (int i = 0; i < pathCount; i++) {
					f = path.get(i);

					final int topIndex = directories.indexOf(f);

					if (topIndex != -1) {
						for (int j = i - 1; j >= 0; j--) {
							directories.add((topIndex + i) - j, path.get(j));
						}

						break;
					}
				}

				calculateDepths();
			} catch (final Exception ex) {
				calculateDepths();
			}
		}

		private void calculateDepths() {
			depths = new int[directories.size()];

			final int count = depths.length;

			for (int i = 0; i < count; i++) {
				final FileObject dir = directories.get(i);
				final FileObject parent = VFSUtils.getParentDirectory(dir);
				depths[i] = 0;

				if (parent != null) {
					for (int j = i - 1; j >= 0; j--) {
						if (parent.equals(directories.get(j))) {
							depths[i] = depths[j] + 1;

							break;
						}
					}
				}
			}
		}

		public int getDepth(final int i) {
			return ((depths != null) && (i >= 0) && (i < depths.length)) ? depths[i] : 0;
		}

		@Override
		public void setSelectedItem(final Object selectedDirectory) {
			if (selectedDirectory.equals(this.selectedDirectory)) {
				return;
			}

			this.selectedDirectory = (FileObject) selectedDirectory;
			fireContentsChanged(this, -1, -1);
		}

		@Override
		public Object getSelectedItem() {
			return selectedDirectory;
		}

		@Override
		public int getSize() {
			return directories.size();
		}

		@Override
		public Object getElementAt(final int index) {
			return directories.get(index);
		}
	}

	/**
	 * Data model for a type-face selection combo-box.
	 */
	@SuppressWarnings({ "serial", "rawtypes" })
	public final class FilterComboBoxModel extends AbstractListModel implements ComboBoxModel, PropertyChangeListener {

		protected AbstractVFSFileFilter[] filters;

		protected FilterComboBoxModel() {
			super();
			filters = getFileChooser().getChoosableFileFilters();
		}

		@Override
		public void propertyChange(final PropertyChangeEvent e) {
			final String prop = e.getPropertyName();

			if (prop.equals(VFSJFileChooserConstants.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY)) {
				filters = (AbstractVFSFileFilter[]) e.getNewValue();
				fireContentsChanged(this, -1, -1);
			} else if (prop.equals(VFSJFileChooserConstants.FILE_FILTER_CHANGED_PROPERTY)) {
				fireContentsChanged(this, -1, -1);
			}
		}

		@Override
		public void setSelectedItem(final Object filter) {
			if (filter != null) {
				getFileChooser().setFileFilter((AbstractVFSFileFilter) filter);
				fireContentsChanged(this, -1, -1);
			}
		}

		@Override
		public Object getSelectedItem() {
			// Ensure that the current filter is in the list.
			// NOTE: we shouldnt' have to do this, since VFSJFileChooser adds
			// the filter to the choosable filters list when the filter
			// is set. Lets be paranoid just in case someone overrides
			// setFileFilter in VFSJFileChooser.
			final AbstractVFSFileFilter currentFilter = getFileChooser().getFileFilter();
			boolean found = false;

			if (currentFilter != null) {
				for (final AbstractVFSFileFilter aFilter : filters) {
					if (aFilter == currentFilter) {
						found = true;
					}
				}

				if (found == false) {
					getFileChooser().addChoosableFileFilter(currentFilter);
				}
			}

			return getFileChooser().getFileFilter();
		}

		@Override
		public int getSize() {
			if (filters != null) {
				return filters.length;
			}
			return 0;

		}

		@Override
		public Object getElementAt(final int index) {
			if (index > (getSize() - 1)) {
				// This shouldn't happen. Try to recover gracefully.
				return getFileChooser().getFileFilter();
			}

			if (filters != null) {
				return filters[index];
			}
			return null;

		}
	}

	/**
	 * Acts when DirectoryComboBox has changed the selected item.
	 */
	@SuppressWarnings("serial")
	final class DirectoryComboBoxAction extends AbstractAction {

		protected DirectoryComboBoxAction() {
			super("DirectoryComboBoxAction");
		}

		@SuppressWarnings({ "synthetic-access", "unused" })
		@Override
		public void actionPerformed(final ActionEvent e) {
			directoryComboBox.hidePopup();

			final FileObject folder = (FileObject) directoryComboBox.getSelectedItem();

			if (!getFileChooser().getCurrentDirectoryObject().equals(folder)) {
				getFileChooser().setCurrentDirectoryObject(folder);
			}
		}
	}
}
// CHECKSTYLE:ON
