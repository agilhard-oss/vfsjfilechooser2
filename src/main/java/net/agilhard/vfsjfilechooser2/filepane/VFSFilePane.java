// CHECKSTYLE:OFF
/*
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
package net.agilhard.vfsjfilechooser2.filepane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.Position;

import net.agilhard.vfsjfilechooser2.VFSJFileChooser;
import net.agilhard.vfsjfilechooser2.plaf.VFSFileChooserUIAccessorIF;
import net.agilhard.vfsjfilechooser2.plaf.basic.BasicVFSDirectoryModel;
import net.agilhard.vfsjfilechooser2.utils.FileObjectComparatorFactory;
import net.agilhard.vfsjfilechooser2.utils.SwingCommonsUtilities;
import net.agilhard.vfsjfilechooser2.utils.VFSResources;
import net.agilhard.vfsjfilechooser2.utils.VFSUtils;
import org.apache.commons.vfs2.FileObject;

import net.agilhard.vfsjfilechooser2.constants.VFSJFileChooserConstants;
import net.agilhard.vfsjfilechooser2.filechooser.AbstractVFSFileSystemView;

/**
 * This class is based on sun.swing.FilePane
 * 
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 * @version 0.0.1
 */
@SuppressWarnings("serial")
public final class VFSFilePane extends JPanel implements PropertyChangeListener {
	public final static String ACTION_APPROVE_SELECTION = "approveSelection";
	public final static String ACTION_CANCEL = "cancelSelection";
	public final static String ACTION_EDIT_FILE_NAME = "editFileName";
	public final static String ACTION_REFRESH = "refresh";
	public final static String ACTION_CHANGE_TO_PARENT_DIRECTORY = "Go Up";
	public final static String ACTION_NEW_FOLDER = "New Folder";
	public final static String ACTION_VIEW_LIST = "viewTypeList";
	public final static String ACTION_VIEW_DETAILS = "viewTypeDetails";
	public final static String ACTION_VIEW_HIDDEN = "viewHidden";
	public static final int VIEWTYPE_LIST = 0;
	public static final int VIEWTYPE_DETAILS = 1;
	private static final int VIEWTYPE_COUNT = 2;
	private static final Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	private static FocusListener repaintListener = new FocusListener() {

		@Override
		public void focusGained(final FocusEvent fe) {
			repaintSelection(fe.getSource());
		}

		@Override
		public void focusLost(final FocusEvent fe) {
			repaintSelection(fe.getSource());
		}

		@SuppressWarnings("rawtypes")
		private void repaintSelection(final Object source) {
			if (source instanceof JList) {
				repaintListSelection((JList) source);
			} else if (source instanceof JTable) {
				repaintTableSelection((JTable) source);
			}
		}

		@SuppressWarnings("rawtypes")
		private void repaintListSelection(final JList list) {
			final int[] indices = list.getSelectedIndices();

			for (final int i : indices) {
				final Rectangle bounds = list.getCellBounds(i, i);
				list.repaint(bounds);
			}
		}

		private void repaintTableSelection(final JTable table) {
			final int minRow = table.getSelectionModel().getMinSelectionIndex();
			final int maxRow = table.getSelectionModel().getMaxSelectionIndex();

			if ((minRow == -1) || (maxRow == -1)) {
				return;
			}

			final int col0 = table.convertColumnIndexToView(COLUMN_FILENAME);

			final Rectangle first = table.getCellRect(minRow, col0, false);
			final Rectangle last = table.getCellRect(maxRow, col0, false);
			final Rectangle dirty = first.union(last);
			table.repaint(dirty);
		}
	};

	private static final int COLUMN_FILENAME = 0;
	private static final int COLUMN_SIZE = 1;
	private static final int COLUMN_DATE = 2;

	// Constants for actions. These are used for the actions' ACTION_COMMAND_KEY
	// and as keys in the action maps for FilePane and the corresponding UI
	// classes
	// private DetailsTableRowSorter rowSorter;
	private JTable detailsTable;
	private Action[] actions;
	private int viewType = -1;
	private final JPanel[] viewPanels = new JPanel[VIEWTYPE_COUNT];
	private JPanel currentViewPanel;
	private String[] viewTypeActionNames;
	private JPopupMenu contextMenu;
	private JMenu viewMenu;
	private String viewMenuLabelText;
	private String refreshActionLabelText;
	private String showHiddenFilesLabelText;
	private String newFolderActionLabelText;
	private String renameErrorTitleText;
	private String renameErrorText;
	private String renameErrorFileExistsText;
	private String fileNameHeaderText = null;
	private String fileSizeHeaderText = null;
	private String fileDateHeaderText = null;
	private transient final FocusListener editorFocusListener = new FocusAdapter() {

		@SuppressWarnings("synthetic-access")
		@Override
		public void focusLost(final FocusEvent e) {
			if (!e.isTemporary()) {
				VFSFilePane.this.applyEdit();
			}
		}
	};

	private final boolean smallIconsView = false;
	private Border listViewBorder;
	private Color listViewBackground;
	private boolean listViewWindowsStyle;
	private boolean readOnly;
	private ListSelectionModel listSelectionModel;

	@SuppressWarnings("rawtypes")
	private JList list;
	private DetailsTableModel detailsTableModel;

	// Provides a way to recognize a newly created folder, so it can
	// be selected when it appears in the model.
	private FileObject newFolderFile;

	// Used for accessing methods in the corresponding UI class
	private final VFSFileChooserUIAccessorIF fileChooserUIAccessor;
	private DetailsTableCellEditor tableCellEditor;
	private int lastIndex = -1;
	private FileObject editFile = null;
	private final int editX = 20;
	private JTextField editCell = null;
	protected Action newFolderAction;
	private Handler handler;

	// details view
	@SuppressWarnings({ "unused", "boxing" })
	private final transient KeyListener detailsKeyListener = new KeyAdapter() {
		private final long timeFactor;
		private final StringBuilder typedString = new StringBuilder();
		private long lastTime = 1000L;

		{
			final Long l = (Long) UIManager.get("Table.timeFactor");
			timeFactor = (l != null) ? l : 1000L;
		}

		/**
		 * Moves the keyboard focus to the first element whose prefix matches
		 * the sequence of alphanumeric keys pressed by the user with delay less
		 * than value of <code>timeFactor</code>. Subsequent same key presses
		 * move the keyboard focus to the next object that starts with the same
		 * letter until another key is pressed, then it is treated as the prefix
		 * with appropriate number of the same letters followed by first typed
		 * another letter.
		 */
		@SuppressWarnings("synthetic-access")
		@Override
		public void keyTyped(final KeyEvent e) {
			final BasicVFSDirectoryModel model = VFSFilePane.this.getModel();
			final int rowCount = model.getSize();

			if ((detailsTable == null) || (rowCount == 0) || e.isAltDown() || e.isControlDown() || e.isMetaDown()) {
				return;
			}

			final InputMap inputMap = detailsTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			final KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);

			if ((inputMap != null) && (inputMap.get(key) != null)) {
				return;
			}

			int startIndex = detailsTable.getSelectionModel().getLeadSelectionIndex();

			if (startIndex < 0) {
				startIndex = 0;
			}

			if (startIndex >= rowCount) {
				startIndex = rowCount - 1;
			}

			final char c = e.getKeyChar();

			final long time = e.getWhen();

			if ((time - lastTime) < timeFactor) {
				if ((typedString.length() == 1) && (typedString.charAt(0) == c)) {
					// Subsequent same key presses move the keyboard focus to
					// the next
					// object that starts with the same letter.
					startIndex++;
				} else {
					typedString.append(c);
				}
			} else {
				startIndex++;

				typedString.setLength(0);
				typedString.append(c);
			}

			lastTime = time;

			if (startIndex >= rowCount) {
				startIndex = 0;
			}

			// Find next file
			int index = getNextMatch(startIndex, rowCount - 1);

			if ((index < 0) && (startIndex > 0)) { // wrap
				index = getNextMatch(0, startIndex - 1);
			}

			if (index >= 0) {
				detailsTable.getSelectionModel().setSelectionInterval(index, index);

				final Rectangle cellRect = detailsTable.getCellRect(index,
						detailsTable.convertColumnIndexToView(COLUMN_FILENAME), false);
				detailsTable.scrollRectToVisible(cellRect);
			}
		}

		private int getNextMatch(final int startIndex, final int finishIndex) {
			return -1;
		}
	};

	/**
	 * @param fileChooserUIAccessor
	 */
	public VFSFilePane(final VFSFileChooserUIAccessorIF fileChooserUIAccessor) {
		super(new BorderLayout());

		this.fileChooserUIAccessor = fileChooserUIAccessor;
		installDefaults();
		createActionMap();
	}

	/**
	 *
	 */
	public void uninstallUI() {
		if (getModel() != null) {
			getModel().removePropertyChangeListener(this);
		}
	}

	/**
	 * @return
	 */
	protected VFSJFileChooser getFileChooser() {
		return fileChooserUIAccessor.getFileChooser();
	}

	/**
	 * @return
	 */
	protected BasicVFSDirectoryModel getModel() {
		return fileChooserUIAccessor.getModel();
	}

	/**
	 * @return
	 */
	public int getViewType() {
		return viewType;
	}

	/**
	 * @param viewType
	 */
	public void setViewType(final int viewType) {
		final int oldValue = this.viewType;

		if (viewType == oldValue) {
			return;
		}

		this.viewType = viewType;

		if (viewType == VIEWTYPE_LIST) {
			if (viewPanels[viewType] == null) {
				JPanel p = fileChooserUIAccessor.createList();

				if (p == null) {
					p = createList();
				}

				setViewPanel(viewType, p);
			}

			list.setLayoutOrientation(JList.VERTICAL_WRAP);
		} else if (viewType == VIEWTYPE_DETAILS) {
			if (viewPanels[viewType] == null) {
				JPanel p = fileChooserUIAccessor.createDetailsView();

				if (p == null) {
					p = createDetailsView();
				}

				setViewPanel(viewType, p);
			}
		}

		final JPanel oldViewPanel = currentViewPanel;
		currentViewPanel = viewPanels[viewType];

		if (currentViewPanel != oldViewPanel) {
			if (oldViewPanel != null) {
				this.remove(oldViewPanel);
			}

			this.add(currentViewPanel, BorderLayout.CENTER);
			revalidate();
			this.repaint();
		}

		updateViewMenu();
		this.firePropertyChange("viewType", oldValue, viewType);
	}

	/**
	 * @param viewType
	 * @param viewPanel
	 */
	@SuppressWarnings("rawtypes")
	public void setViewPanel(final int viewType, final JPanel viewPanel) {
		viewPanels[viewType] = viewPanel;
		recursivelySetInheritsPopupMenu(viewPanel, true);

		// switch to list view
		if (viewType == VIEWTYPE_LIST) {
			list = (JList) findChildComponent(viewPanels[viewType], JList.class);

			if (listSelectionModel == null) {
				listSelectionModel = list.getSelectionModel();

				if (detailsTable != null) {
					detailsTable.setSelectionModel(listSelectionModel);
				}
			} else {
				list.setSelectionModel(listSelectionModel);
			}
		}

		// switch to details view
		else if (viewType == VIEWTYPE_DETAILS) {
			detailsTable = (JTable) findChildComponent(viewPanels[viewType], JTable.class);
			detailsTable.setRowHeight(Math.max(detailsTable.getFont().getSize() + 4, 16 + 1));

			if (listSelectionModel != null) {
				detailsTable.setSelectionModel(listSelectionModel);
			}
		}

		if (this.viewType == viewType) {
			if (currentViewPanel != null) {
				this.remove(currentViewPanel);
			}

			currentViewPanel = viewPanel;
			this.add(currentViewPanel, BorderLayout.CENTER);
			revalidate();
			this.repaint();
		}
	}

	/**
	 * @param viewType
	 * @return
	 */
	public Action getViewTypeAction(final int csViewType) {
		return new ViewTypeAction(csViewType);
	}

	private static void recursivelySetInheritsPopupMenu(final Container container, final boolean b) {
		if (container instanceof JComponent) {
			((JComponent) container).setInheritsPopupMenu(b);
		}

		final Component[] components = container.getComponents();

		for (final Component component : components) {
			recursivelySetInheritsPopupMenu((Container) component, b);
		}
	}

	protected void installDefaults() {

		listViewBorder = UIManager.getBorder("FileChooser.listViewBorder");
		listViewBackground = UIManager.getColor("FileChooser.listViewBackground");
		listViewWindowsStyle = UIManager.getBoolean("FileChooser.listViewWindowsStyle");
		readOnly = UIManager.getBoolean("FileChooser.readOnly");

		// TODO: On windows, get the following localized strings from the OS
		viewMenuLabelText = VFSResources.getMessage("VFSJFileChooser.viewMenuLabelText");
		refreshActionLabelText = VFSResources.getMessage("VFSJFileChooser.refreshActionLabelText");
		showHiddenFilesLabelText = VFSResources.getMessage("VFSJFileChooser.showHiddenFilesLabelText");
		newFolderActionLabelText = VFSResources.getMessage("VFSJFileChooser.newFolderActionLabelText");

		viewTypeActionNames = new String[VIEWTYPE_COUNT];
		viewTypeActionNames[VIEWTYPE_LIST] = VFSResources.getMessage("VFSJFileChooser.listViewActionLabelText");
		viewTypeActionNames[VIEWTYPE_DETAILS] = VFSResources.getMessage("VFSJFileChooser.detailsViewActionLabelText");

		renameErrorTitleText = VFSResources.getMessage("VFSJFileChooser.renameErrorTitleText");
		renameErrorText = VFSResources.getMessage("VFSJFileChooser.renameErrorText");
		renameErrorFileExistsText = VFSResources.getMessage("VFSJFileChooser.renameErrorFileExistsText");

		fileNameHeaderText = VFSResources.getMessage("VFSJFileChooser.fileNameHeaderText");
		fileSizeHeaderText = VFSResources.getMessage("VFSJFileChooser.fileSizeHeaderText");
		fileDateHeaderText = VFSResources.getMessage("VFSJFileChooser.fileDateHeaderText");
	}

	/**
	 * Fetches the command list for the FilePane. These commands are useful for
	 * binding to events, such as in a keymap.
	 *
	 * @return the command list
	 */
	@SuppressWarnings("boxing")
	public Action[] getActions() {
		if (actions == null) {
			class FilePaneAction extends AbstractAction {
				FilePaneAction(final String name) {
					this(name, name);
				}

				FilePaneAction(final String name, final String cmd) {
					super(name);
					putValue(Action.ACTION_COMMAND_KEY, cmd);
				}

				@Override
				@SuppressWarnings({ "synthetic-access", "unused" })
				public void actionPerformed(final ActionEvent e) {
					final String cmd = (String) getValue(Action.ACTION_COMMAND_KEY);

					if (cmd.equals(ACTION_CANCEL)) {
						if (editFile != null) {
							cancelEdit();
						} else {
							getFileChooser().cancelSelection();
						}
					} else if (cmd.equals(ACTION_EDIT_FILE_NAME)) {
						final VFSJFileChooser fc = getFileChooser();
						final int index = listSelectionModel.getMinSelectionIndex();

						if ((index >= 0) && (editFile == null)
								&& (!fc.isMultiSelectionEnabled() || (fc.getSelectedFileObjects().length <= 1))) {
							editFileName(index);
						}
					} else if (cmd.equals(ACTION_REFRESH)) {
						getFileChooser().rescanCurrentDirectory();
					} else if (cmd.equals(ACTION_VIEW_HIDDEN)) {
						getFileChooser().setFileHidingEnabled(!getFileChooser().isFileHidingEnabled());
					}
				}

				@SuppressWarnings("synthetic-access")
				@Override
				public boolean isEnabled() {
					final String cmd = (String) getValue(Action.ACTION_COMMAND_KEY);

					if (cmd.equals(ACTION_CANCEL)) {
						return getFileChooser().isEnabled();
					} else if (cmd.equals(ACTION_EDIT_FILE_NAME)) {
						return !readOnly && getFileChooser().isEnabled();
					} else {
						return true;
					}
				}
			}

			final ArrayList<Action> actionList = new ArrayList<Action>(8);
			Action action;

			actionList.add(new FilePaneAction(ACTION_CANCEL));
			actionList.add(new FilePaneAction(ACTION_EDIT_FILE_NAME));
			final FilePaneAction showHidden = new FilePaneAction(showHiddenFilesLabelText, ACTION_VIEW_HIDDEN);
			showHidden.putValue(Action.SELECTED_KEY, false);
			actionList.add(showHidden);
			actionList.add(new FilePaneAction(refreshActionLabelText, ACTION_REFRESH));

			action = fileChooserUIAccessor.getApproveSelectionAction();

			if (action != null) {
				actionList.add(action);
			}

			action = fileChooserUIAccessor.getChangeToParentDirectoryAction();

			if (action != null) {
				actionList.add(action);
			}

			action = getNewFolderAction();

			if (action != null) {
				actionList.add(action);
			}

			action = getViewTypeAction(VIEWTYPE_LIST);

			if (action != null) {
				actionList.add(action);
			}

			actions = actionList.toArray(new Action[actionList.size()]);
		}

		return actions.clone();
	}

	protected void createActionMap() {
		addActionsToMap(super.getActionMap(), getActions());
	}

	/**
	 * @param map
	 * @param actions
	 */
	public static void addActionsToMap(final ActionMap map, final Action[] actions) {
		if ((map != null) && (actions != null)) {
			for (final Action a : actions) {
				String cmd = (String) a.getValue(Action.ACTION_COMMAND_KEY);

				if (cmd == null) {
					cmd = (String) a.getValue(Action.NAME);
				}

				map.put(cmd, a);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void updateListRowCount(final JList csList) {
		if (smallIconsView) {
			csList.setVisibleRowCount(getModel().getSize() / 3);
		} else {
			csList.setVisibleRowCount(-1);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JPanel createList() {
		final JPanel p = new JPanel(new BorderLayout());
		final VFSJFileChooser fileChooser = getFileChooser();
		final JList aList = new JList() {
			@Override
			public int getNextMatch(final String prefix, final int startIndex, final Position.Bias bias) {
				final ListModel model = getModel();
				final int max = model.getSize();

				if ((prefix == null) || (startIndex < 0) || (startIndex >= max)) {
					throw new IllegalArgumentException();
				}

				// start search from the next element before/after the selected
				// element
				final boolean backwards = (bias == Position.Bias.Backward);

				for (int i = startIndex; backwards ? (i >= 0) : (i < max); i += (backwards ? (-1) : 1)) {
					final String filename = fileChooser.getName((FileObject) model.getElementAt(i));

					if (filename.regionMatches(true, 0, prefix, 0, prefix.length())) {
						return i;
					}
				}

				return -1;
			}
		};

		aList.setCellRenderer(new FileRenderer());
		aList.setLayoutOrientation(JList.VERTICAL_WRAP);

		// 4835633 : tell BasicListUI that this is a file list
		aList.putClientProperty("List.isFileList", Boolean.TRUE);

		if (listViewWindowsStyle) {
			aList.addFocusListener(repaintListener);
		}

		updateListRowCount(aList);

		getModel().addListDataListener(new ListDataListener() {

			@Override
			@SuppressWarnings({ "synthetic-access", "unused" })
			public void intervalAdded(final ListDataEvent e) {
				VFSFilePane.this.updateListRowCount(aList);
			}

			@SuppressWarnings({ "synthetic-access", "unused" })
			@Override
			public void intervalRemoved(final ListDataEvent e) {
				VFSFilePane.this.updateListRowCount(aList);
			}

			@Override
			@SuppressWarnings({ "unused", "synthetic-access" })
			public void contentsChanged(final ListDataEvent e) {
				if (VFSFilePane.this.isShowing()) {
					VFSFilePane.this.clearSelection();
				}

				VFSFilePane.this.updateListRowCount(aList);
			}
		});

		getModel().addPropertyChangeListener(this);

		if (fileChooser.isMultiSelectionEnabled()) {
			aList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		} else {
			aList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		aList.setModel(getModel());

		aList.addListSelectionListener(createListSelectionListener());
		aList.addMouseListener(getMouseHandler());

		final JScrollPane scrollpane = new JScrollPane(aList);

		if (listViewBackground != null) {
			aList.setBackground(listViewBackground);
		}

		if (listViewBorder != null) {
			scrollpane.setBorder(listViewBorder);
		}

		p.add(scrollpane, BorderLayout.CENTER);

		return p;
	}

	/**
	 * Creates a selection listener for the list of files and directories.
	 *
	 * @return a <code>ListSelectionListener</code>
	 */
	public ListSelectionListener createListSelectionListener() {
		return fileChooserUIAccessor.createListSelectionListener();
	}

	private int getEditIndex() {
		return lastIndex;
	}

	private void setEditIndex(final int i) {
		lastIndex = i;
	}

	private void resetEditIndex() {
		lastIndex = -1;
	}

	private void cancelEdit() {
		if (editFile != null) {
			editFile = null;
			list.remove(editCell);
			this.repaint();
		}
	}

	/**
	 * @param index
	 *            visual index of the file to be edited
	 */
	@SuppressWarnings("deprecation")
	private void editFileName(final int index) {
		final FileObject currentDirectory = getFileChooser().getCurrentDirectoryObject();

		if (readOnly || !canWrite(currentDirectory)) {
			return;
		}

		ensureIndexIsVisible(index);

		switch (viewType) {
		case VIEWTYPE_LIST:
			editFile = (FileObject) getModel().getElementAt(index);

			final Rectangle r = list.getCellBounds(index, index);

			if (editCell == null) {
				editCell = new JTextField();

				editCell.addActionListener(new EditActionListener());
				editCell.addFocusListener(editorFocusListener);
				editCell.setNextFocusableComponent(list);
			}

			list.add(editCell);
			editCell.setText(getFileChooser().getName(editFile));

			final ComponentOrientation orientation = list.getComponentOrientation();
			editCell.setComponentOrientation(orientation);

			if (orientation.isLeftToRight()) {
				editCell.setBounds(editX + r.x, r.y, r.width - editX, r.height);
			} else {
				editCell.setBounds(r.x, r.y, r.width - editX, r.height);
			}

			editCell.requestFocus();
			editCell.selectAll();

			break;

		case VIEWTYPE_DETAILS:
			detailsTable.editCellAt(index, COLUMN_FILENAME);

			break;
		}
	}

	private void applyEdit() {
		if ((editFile != null) && VFSUtils.exists(editFile)) {
			final VFSJFileChooser chooser = getFileChooser();
			final String oldDisplayName = chooser.getName(editFile);
			final String oldFileName = editFile.getName().getBaseName();
			final String newDisplayName = editCell.getText().trim();
			String newFileName;

			if (!newDisplayName.equals(oldDisplayName)) {
				newFileName = newDisplayName;

				// Check if extension is hidden from user
				final int i1 = oldFileName.length();
				final int i2 = oldDisplayName.length();

				if ((i1 > i2) && (oldFileName.charAt(i2) == '.')) {
					newFileName = newDisplayName + oldFileName.substring(i2);
				}

				// rename
				final AbstractVFSFileSystemView fsv = chooser.getFileSystemView();
				final FileObject f2 = fsv.createFileObject(VFSUtils.getParentDirectory(editFile), newFileName);

				if (VFSUtils.exists(f2)) {
					JOptionPane.showMessageDialog(chooser, MessageFormat.format(renameErrorFileExistsText, oldFileName),
							renameErrorTitleText, JOptionPane.ERROR_MESSAGE);
				} else {
					if (getModel().renameFile(editFile, f2)) {
						if (fsv.isParent(chooser.getCurrentDirectoryObject(), f2)) {
							if (chooser.isMultiSelectionEnabled()) {
								chooser.setSelectedFileObjects(new FileObject[] { f2 });
							} else {
								chooser.setSelectedFileObject(f2);
							}
						} else {
							// Could be because of delay in updating Desktop
							// folder
							// chooser.setSelectedFile(null);
						}
					} else {
						JOptionPane.showMessageDialog(chooser, MessageFormat.format(renameErrorText, oldFileName),
								renameErrorTitleText, JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}

		if ((detailsTable != null) && detailsTable.isEditing()) {
			detailsTable.getCellEditor().stopCellEditing();
		}

		cancelEdit();
	}

	public Action getNewFolderAction() {
		if (!readOnly && (newFolderAction == null)) {
			newFolderAction = new AbstractAction(newFolderActionLabelText) {
				private Action basicNewFolderAction;

				{
					putValue(Action.ACTION_COMMAND_KEY, VFSFilePane.ACTION_NEW_FOLDER);

					final FileObject currentDirectory = VFSFilePane.this.getFileChooser().getCurrentDirectoryObject();

					if (currentDirectory != null) {
						setEnabled(VFSFilePane.this.canWrite(currentDirectory));
					}
				}

				@SuppressWarnings("synthetic-access")
				@Override
				public void actionPerformed(final ActionEvent ev) {
					if (basicNewFolderAction == null) {
						basicNewFolderAction = fileChooserUIAccessor.getNewFolderAction();
					}

					final VFSJFileChooser fc = VFSFilePane.this.getFileChooser();
					final FileObject oldFile = fc.getSelectedFileObject();
					basicNewFolderAction.actionPerformed(ev);

					final FileObject newFile = fc.getSelectedFileObject();

					if ((newFile != null) && !newFile.equals(oldFile) && VFSUtils.isDirectory(newFile)) {
						newFolderFile = newFile;
					}
				}
			};
		}

		return newFolderAction;
	}

	void setFileSelected() {
		if (getFileChooser().isMultiSelectionEnabled() && !isDirectorySelected()) {
			final FileObject[] files = getFileChooser().getSelectedFileObjects(); // Should
																					// be
																					// selected

			final Object[] selectedObjects = list.getSelectedValues(); // Are
																		// actually
																		// selected

			listSelectionModel.setValueIsAdjusting(true);

			try {
				final int lead = listSelectionModel.getLeadSelectionIndex();
				final int anchor = listSelectionModel.getAnchorSelectionIndex();

				// Arrays.sort(files);
				// Arrays.sort(selectedObjects);
				int shouldIndex = 0;
				int actuallyIndex = 0;

				final int fileCount = files.length;
				final int selectedFileCount = selectedObjects.length;

				// Remove files that shouldn't be selected and add files which
				// should be selected
				// Note: Assume files are already sorted in compareTo order.
				while ((shouldIndex < fileCount) && (actuallyIndex < selectedFileCount)) {
					shouldIndex++;
					actuallyIndex++;
				}

				while (shouldIndex < fileCount) {
					doSelectFile(files[shouldIndex++]);
				}

				while (actuallyIndex < selectedFileCount) {
					doDeselectFile(selectedObjects[actuallyIndex++]);
				}

				// restore the anchor and lead
				if (listSelectionModel instanceof DefaultListSelectionModel) {
					((DefaultListSelectionModel) listSelectionModel).moveLeadSelectionIndex(lead);
					listSelectionModel.setAnchorSelectionIndex(anchor);
				}
			} finally {
				listSelectionModel.setValueIsAdjusting(false);
			}
		} else {
			final VFSJFileChooser chooser = getFileChooser();
			FileObject f;

			if (isDirectorySelected()) {
				f = getDirectory();
			} else {
				f = chooser.getSelectedFileObject();
			}

			int i;

			if ((f != null) && ((i = getModel().indexOf(f)) >= 0)) {
				final int viewIndex = i;

				listSelectionModel.setSelectionInterval(viewIndex, viewIndex);
				ensureIndexIsVisible(viewIndex);
			} else {
				clearSelection();
			}
		}
	}

	private void doSelectFile(final FileObject fileToSelect) {
		final int index = getModel().indexOf(fileToSelect);

		// could be missed in the current directory if it changed
		if (index >= 0) {
			listSelectionModel.addSelectionInterval(index, index);
		}
	}

	private void doDeselectFile(final Object fileToDeselect) {
		final int index = getModel().indexOf(fileToDeselect);
		listSelectionModel.removeSelectionInterval(index, index);
	}

	/* The following methods are used by the PropertyChange Listener */
	private void doSelectedFileChanged(final PropertyChangeEvent e) {
		applyEdit();

		final FileObject f = (FileObject) e.getNewValue();

		if ((f != null)) {
			setFileSelected();
		}
	}

	private void doSelectedFilesChanged(final PropertyChangeEvent e) {
		applyEdit();

		final FileObject[] files = (FileObject[]) e.getNewValue();
		final VFSJFileChooser fc = getFileChooser();

		if ((files != null) && (files.length > 0)
				&& ((files.length > 1) || fc.isDirectorySelectionEnabled() || !VFSUtils.isDirectory(files[0]))) {
			setFileSelected();
		}
	}

	@SuppressWarnings("unused")
	private void doDirectoryChanged(final PropertyChangeEvent e) {
		final VFSJFileChooser fc = getFileChooser();
		final AbstractVFSFileSystemView fsv = fc.getFileSystemView();

		applyEdit();
		resetEditIndex();
		try {
			ensureIndexIsVisible(0);
		} catch (final Exception ex) {
			// ignored, TODO race condition?
		}

		final FileObject currentDirectory = fc.getCurrentDirectoryObject();

		if (currentDirectory != null) {
			if (!readOnly) {
				getNewFolderAction().setEnabled(canWrite(currentDirectory));
			}

			fileChooserUIAccessor.getChangeToParentDirectoryAction().setEnabled(!fsv.isRoot(currentDirectory));
		}

		if (list != null) {
			list.clearSelection();
		}
	}

	@SuppressWarnings("unused")
	private void doFilterChanged(final PropertyChangeEvent e) {
		applyEdit();
		resetEditIndex();
		clearSelection();
	}

	@SuppressWarnings("unused")
	private void doFileSelectionModeChanged(final PropertyChangeEvent e) {
		applyEdit();
		resetEditIndex();
		clearSelection();
	}

	@SuppressWarnings("unused")
	private void doMultiSelectionChanged(final PropertyChangeEvent e) {
		if (getFileChooser().isMultiSelectionEnabled()) {
			listSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		} else {
			listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			clearSelection();
			getFileChooser().setSelectedFiles(null);
		}
	}

	/*
	 * Listen for filechooser property changes, such as the selected file
	 * changing, or the type of the dialog changing.
	 */
	@Override
	@SuppressWarnings("boxing")
	public void propertyChange(final PropertyChangeEvent e) {
		if (viewType == -1) {
			setViewType(VIEWTYPE_LIST);
		}

		final String s = e.getPropertyName();

		if (s.equals(VFSJFileChooserConstants.SELECTED_FILE_CHANGED_PROPERTY)) {
			doSelectedFileChanged(e);
		} else if (s.equals(VFSJFileChooserConstants.SELECTED_FILES_CHANGED_PROPERTY)) {
			doSelectedFilesChanged(e);
		} else if (s.equals(VFSJFileChooserConstants.DIRECTORY_CHANGED_PROPERTY)) {
			doDirectoryChanged(e);
		} else if (s.equals(VFSJFileChooserConstants.FILE_FILTER_CHANGED_PROPERTY)) {
			doFilterChanged(e);
		} else if (s.equals(VFSJFileChooserConstants.FILE_SELECTION_MODE_CHANGED_PROPERTY)) {
			doFileSelectionModeChanged(e);
		} else if (s.equals(VFSJFileChooserConstants.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY)) {
			doMultiSelectionChanged(e);
		} else if (s.equals(VFSJFileChooserConstants.CANCEL_SELECTION)) {
			applyEdit();
		} else if (s.equals("busy")) {
			setCursor((Boolean) e.getNewValue() ? waitCursor : null);
		} else if (s.equals("componentOrientation")) {
			final ComponentOrientation o = (ComponentOrientation) e.getNewValue();
			final VFSJFileChooser cc = (VFSJFileChooser) e.getSource();

			if (o != e.getOldValue()) {
				cc.applyComponentOrientation(o);
			}
		}
	}

	private void ensureIndexIsVisible(final int i) {
		if (i >= 0) {
			if ((list != null) && (list.getModel().getSize() > i)) {
				Rectangle cellBounds = list.getCellBounds(i, i);

				if (cellBounds == null) {
					cellBounds = list.getCellBounds(i - 1, i - 1);

					if (cellBounds != null) {
						// 2* so that you get bottom of cell
						cellBounds.translate(0, 2 * cellBounds.height);
						list.scrollRectToVisible(cellBounds);
					}
				} else {
					list.ensureIndexIsVisible(i);
				}
			}

			if (detailsTable != null) {
				final Rectangle r = detailsTable.getCellRect(i, COLUMN_FILENAME, true);
				detailsTable.scrollRectToVisible(r);
			}
		}
	}

	@SuppressWarnings("unused")
	public void ensureFileIsVisible(final VFSJFileChooser fc, final FileObject f) {
		final int modelIndex = getModel().indexOf(f);

		if (modelIndex >= 0) {
			ensureIndexIsVisible(modelIndex);
		}
	}

	public void rescanCurrentDirectory() {
		getModel().validateFileCache();
	}

	public void clearSelection() {
		if (listSelectionModel != null) {
			listSelectionModel.clearSelection();

			if (listSelectionModel instanceof DefaultListSelectionModel) {
				((DefaultListSelectionModel) listSelectionModel).moveLeadSelectionIndex(0);
				listSelectionModel.setAnchorSelectionIndex(0);
			}
		}
	}

	public JMenu getViewMenu() {
		if (viewMenu == null) {
			viewMenu = new JMenu(viewMenuLabelText);

			final ButtonGroup viewButtonGroup = new ButtonGroup();

			for (int i = 0; i < VIEWTYPE_COUNT; i++) {
				final JRadioButtonMenuItem mi = new JRadioButtonMenuItem();
				mi.setAction(new ViewTypeAction(i));
				viewButtonGroup.add(mi);
				viewMenu.add(mi);
			}

			updateViewMenu();
		}

		return viewMenu;
	}

	@SuppressWarnings("synthetic-access")
	private void updateViewMenu() {
		if (viewMenu != null) {
			final Component[] components = viewMenu.getMenuComponents();

			for (final Component component : components) {
				if (component instanceof JRadioButtonMenuItem) {
					final JRadioButtonMenuItem mi = (JRadioButtonMenuItem) component;

					if (((ViewTypeAction) mi.getAction()).vta_viewType == viewType) {
						mi.setSelected(true);
					}
				} else if (component instanceof JCheckBoxMenuItem) {
					final JCheckBoxMenuItem mi = (JCheckBoxMenuItem) component;
					if (mi.getActionCommand().equals(ACTION_VIEW_HIDDEN)) {
						mi.setSelected(getFileChooser().isFileHidingEnabled());
					}
				}
			}
		}
	}

	@SuppressWarnings("boxing")
	@Override
	public JPopupMenu getComponentPopupMenu() {
		final JPopupMenu popupMenu = getFileChooser().getComponentPopupMenu();

		if (popupMenu != null) {
			return popupMenu;
		}

		final JMenu aViewMenu = getViewMenu();

		if (contextMenu == null) {
			contextMenu = new JPopupMenu();

			if (aViewMenu != null) {
				contextMenu.add(aViewMenu);

				if (listViewWindowsStyle) {
					contextMenu.addSeparator();
				}
			}

			final ActionMap actionMap = getActionMap();
			final Action refreshAction = actionMap.get(ACTION_REFRESH);
			final Action aNewFolderAction = actionMap.get(ACTION_NEW_FOLDER);
			final Action showHiddenFiles = actionMap.get(ACTION_VIEW_HIDDEN);

			if (refreshAction != null) {
				contextMenu.add(refreshAction);

				if (listViewWindowsStyle && (aNewFolderAction != null)) {
					contextMenu.addSeparator();
				}
			}

			if (showHiddenFiles != null) {
				final JCheckBoxMenuItem menuitem = new JCheckBoxMenuItem(showHiddenFiles);
				menuitem.setSelected((Boolean) showHiddenFiles.getValue(Action.SELECTED_KEY));
				contextMenu.add(menuitem);
			}

			if (aNewFolderAction != null) {
				contextMenu.add(aNewFolderAction);
			}
		}

		if (aViewMenu != null) {
			aViewMenu.getPopupMenu().setInvoker(aViewMenu);
		}

		return contextMenu;
	}

	@SuppressWarnings("synthetic-access")
	protected Handler getMouseHandler() {
		if (handler == null) {
			handler = new Handler();
		}

		return handler;
	}

	/**
	 * Property to remember whether a directory is currently selected in the UI.
	 *
	 * @return <code>true</code> iff a directory is currently selected.
	 */
	protected boolean isDirectorySelected() {
		return fileChooserUIAccessor.isDirectorySelected();
	}

	/**
	 * Property to remember the directory that is currently selected in the UI.
	 *
	 * @return the value of the <code>directory</code> property
	 * @see javax.swing.plaf.basic.BasicFileChooserUI#setDirectory
	 */
	protected FileObject getDirectory() {
		return fileChooserUIAccessor.getDirectory();
	}

	private Component findChildComponent(final Container container, final Class<? extends Object> cls) {
		final Component[] components = container.getComponents();

		for (final Component component : components) {
			if (cls.isInstance(component)) {
				return component;
			} else if (component instanceof Container) {
				final Component c = findChildComponent((Container) component, cls);

				if (c != null) {
					return c;
				}
			}
		}

		return null;
	}

	public boolean canWrite(final FileObject f) {
		return VFSUtils.canWrite(f);
	}

	private void updateDetailsColumnModel(final JTable table) {
		if (table != null) {
			// Install cell editor for editing file name
			if (!readOnly && (table.getColumnCount() > COLUMN_FILENAME)) {
				table.getColumnModel().getColumn(COLUMN_FILENAME).setCellEditor(getDetailsTableCellEditor());
			}
		}
	}

	private DetailsTableCellEditor getDetailsTableCellEditor() {
		if (tableCellEditor == null) {
			tableCellEditor = new DetailsTableCellEditor(new JTextField());
		}

		return tableCellEditor;
	}

	private DetailsTableModel getDetailsTableModel() {
		if (detailsTableModel == null) {
			detailsTableModel = new DetailsTableModel(getFileChooser());
		}

		return detailsTableModel;
	}

	public JPanel createDetailsView() {
		final VFSJFileChooser chooser = getFileChooser();

		final JPanel p = new JPanel(new BorderLayout());

		detailsTable = new JTable(getDetailsTableModel()) {
			// Handle Escape key events here
			@Override
			protected boolean processKeyBinding(final KeyStroke ks, final KeyEvent e, final int condition,
					final boolean pressed) {
				if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) && (this.getCellEditor() == null)) {
					// We are not editing, forward to filechooser.
					chooser.dispatchEvent(e);

					return true;
				}

				return super.processKeyBinding(ks, e, condition, pressed);
			}

			@SuppressWarnings("synthetic-access")
			@Override
			public void tableChanged(final TableModelEvent e) {
				super.tableChanged(e);

				if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
					// update header with possibly changed column set
					VFSFilePane.this.updateDetailsColumnModel(this);
				}
			}
		};

		// detailsTable.setRowSorter(getRowSorter());
		detailsTable.setAutoCreateColumnsFromModel(false);
		detailsTable.setComponentOrientation(chooser.getComponentOrientation());
		// detailsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		detailsTable.setShowGrid(false);
		detailsTable.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);

		// detailsTable.addKeyListener(detailsKeyListener);
		final Font font = list.getFont();
		detailsTable.setFont(font);
		detailsTable.setIntercellSpacing(new Dimension(0, 0));

		final TableCellRenderer headerRenderer = new AlignableTableHeaderRenderer(
				detailsTable.getTableHeader().getDefaultRenderer());
		detailsTable.getTableHeader().setDefaultRenderer(headerRenderer);

		final TableCellRenderer cellRenderer = new DetailsTableCellRenderer(chooser);
		detailsTable.setDefaultRenderer(Object.class, cellRenderer);

		// So that drag can be started on a mouse press
		detailsTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		detailsTable.addMouseListener(getMouseHandler());
		// No need to addListSelectionListener because selections are forwarded
		// to our JList.

		// 4835633 : tell BasicTableUI that this is a file list
		detailsTable.putClientProperty("Table.isFileList", Boolean.TRUE);

		if (listViewWindowsStyle) {
			detailsTable.addFocusListener(repaintListener);
		}

		final JTableHeader header = detailsTable.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(detailsTableModel.new ColumnListener());
		header.setReorderingAllowed(true);

		// TAB/SHIFT-TAB should transfer focus and ENTER should select an item.
		// We don't want them to navigate within the table
		final ActionMap am = SwingUtilities.getUIActionMap(detailsTable);
		am.remove("selectNextRowCell");
		am.remove("selectPreviousRowCell");
		am.remove("selectNextColumnCell");
		am.remove("selectPreviousColumnCell");
		detailsTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
		detailsTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

		final JScrollPane scrollpane = new JScrollPane(detailsTable);
		scrollpane.setComponentOrientation(chooser.getComponentOrientation());
		LookAndFeel.installColors(scrollpane.getViewport(), "Table.background", "Table.foreground");

		// Adjust width of first column so the table fills the viewport when
		// first displayed (temporary listener).
		scrollpane.addComponentListener(new ComponentAdapter() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void componentResized(final ComponentEvent e) {
				final JScrollPane sp = (JScrollPane) e.getComponent();
				VFSFilePane.this.fixNameColumnWidth(sp.getViewport().getSize().width);
				sp.removeComponentListener(this);
			}
		});

		// 4835633.
		// If the mouse is pressed in the area below the Details view table, the
		// event is not dispatched to the Table MouseListener but to the
		// scrollpane. Listen for that here so we can clear the selection.
		scrollpane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				final JScrollPane jsp = ((JScrollPane) e.getComponent());
				final JTable table = (JTable) jsp.getViewport().getView();

				if (!e.isShiftDown()
						|| (table.getSelectionModel().getSelectionMode() == ListSelectionModel.SINGLE_SELECTION)) {
					VFSFilePane.this.clearSelection();

					final TableCellEditor tce = table.getCellEditor();

					if (tce != null) {
						tce.stopCellEditing();
					}
				}
			}
		});

		detailsTable.setForeground(list.getForeground());
		detailsTable.setBackground(list.getBackground());

		if (listViewBorder != null) {
			scrollpane.setBorder(listViewBorder);
		}

		p.add(scrollpane, BorderLayout.CENTER);

		detailsTableModel.fireTableStructureChanged();

		return p;
	} // createDetailsView

	private void fixNameColumnWidth(final int viewWidth) {
		final TableColumn nameCol = detailsTable.getColumnModel().getColumn(COLUMN_FILENAME);
		final int tableWidth = detailsTable.getPreferredSize().width;

		if (tableWidth < viewWidth) {
			nameCol.setPreferredWidth((nameCol.getPreferredWidth() + viewWidth) - tableWidth);
		}
	}

	class DetailsTableCellRenderer extends DefaultTableCellRenderer {
		VFSJFileChooser chooser;
		DateFormat df;

		DetailsTableCellRenderer(final VFSJFileChooser chooser) {
			this.chooser = chooser;
			df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, chooser.getLocale());
		}

		@Override
		public void setBounds(int x, final int y, int width, final int height) {
			if (getHorizontalAlignment() == SwingConstants.LEADING) {
				// Restrict width to actual text
				width = Math.min(width, getPreferredSize().width + 4);
			} else {
				x -= 4;
			}

			super.setBounds(x, y, width, height);
		}

		@Override
		public Insets getInsets(Insets i) {
			// Provide some space between columns
			i = super.getInsets(i);
			i.left += 4;
			i.right += 4;

			return i;
		}

		@SuppressWarnings({ "synthetic-access", "boxing" })
		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, boolean isSelected,
				final boolean hasFocus, final int row, final int column) {
			if ((table.convertColumnIndexToModel(column) != COLUMN_FILENAME)
					|| (listViewWindowsStyle && !table.isFocusOwner())) {
				isSelected = false;
			}

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			setIcon(null);
			setHorizontalAlignment(SwingConstants.LEFT);

			// formatting cell text
			// TODO: it's rather a temporary trick, to be revised
			String text;

			if (value == null) {
				text = "";
			} else if (value instanceof FileObject) {
				final FileObject file = (FileObject) value;
				text = chooser.getName(file);

				final Icon icon = chooser.getIcon(file);
				setIcon(icon);
			} else if (value instanceof Long) {
				final long len = (Long) value; // ((Long) value) / 1024L;

				text = VFSUtils.byteCountToDisplaySize(len);
			} else if (value instanceof Date) {
				text = df.format((Date) value);
			} else {
				text = value.toString();
			}

			setText(text);

			return this;
		}
	}

	// This interface is used to access methods in the FileChooserUI
	// that are not public.
	class ViewTypeAction extends AbstractAction {
		private final int vta_viewType;

		@SuppressWarnings("synthetic-access")
		ViewTypeAction(final int viewType) {
			super(viewTypeActionNames[viewType]);
			vta_viewType = viewType;

			String cmd;

			if (viewType == VIEWTYPE_LIST) {
				cmd = ACTION_VIEW_LIST;
			} else if (viewType == VIEWTYPE_DETAILS) {
				cmd = ACTION_VIEW_DETAILS;
			} else {
				cmd = (String) getValue(Action.NAME);
			}

			putValue(Action.ACTION_COMMAND_KEY, cmd);
		}

		@Override
		@SuppressWarnings("unused")
		public void actionPerformed(final ActionEvent e) {
			setViewType(vta_viewType);
		}
	}

	private class DetailsTableCellEditor extends DefaultCellEditor {
		private final JTextField tf;

		@SuppressWarnings("synthetic-access")
		public DetailsTableCellEditor(final JTextField tf) {
			super(tf);
			this.tf = tf;
			tf.addFocusListener(editorFocusListener);
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public boolean isCellEditable(final EventObject e) {
			if (e instanceof MouseEvent) {
				final MouseEvent me = (MouseEvent) e;
				final int index = detailsTable.rowAtPoint(me.getPoint());

				return ((me.getClickCount() == 1) && detailsTable.isRowSelected(index));
			}

			return super.isCellEditable(e);
		}

		@Override
		public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
				final int row, final int column) {
			final Component comp = super.getTableCellEditorComponent(table, value, isSelected, row, column);

			if (value instanceof FileObject) {
				tf.setText(getFileChooser().getName((FileObject) value));
				tf.selectAll();
			}

			return comp;
		}
	}

	private class DelayedSelectionUpdater implements Runnable {

		FileObject dsu_editFile;

		DelayedSelectionUpdater() {
			this(null);
		}

		DelayedSelectionUpdater(final FileObject editFile) {
			dsu_editFile = editFile;

			if (isShowing()) {
				SwingUtilities.invokeLater(this);
			}
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public void run() {
			setFileSelected();

			if (dsu_editFile != null) {
				editFileName(getModel().indexOf(dsu_editFile));
				dsu_editFile = null;
			}
		}
	}

	class EditActionListener implements ActionListener {

		@Override
		@SuppressWarnings({ "synthetic-access", "unused" })
		public void actionPerformed(final ActionEvent e) {
			applyEdit();
		}
	}

	protected class FileRenderer extends DefaultListCellRenderer {

		@SuppressWarnings({ "rawtypes", "hiding", "unused" })
		@Override
		public Component getListCellRendererComponent(final JList list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus) {
			final FileObject f = (FileObject) value;

			if (f != null) {
				setText(getFileChooser().getName(f));
				setIcon(getFileChooser().getIcon(f));
			} else {
				setText("");
				setIcon(null);
			}

			setOpaque(true);

			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			setEnabled(list.isEnabled());
			setFont(list.getFont());

			if (cellHasFocus) {
				setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
			} else {
				setBorder(noFocusBorder);
			}

			return this;
		}
	}

	private class Handler implements MouseListener {
		private MouseListener doubleClickListener;

		@Override
		@SuppressWarnings("synthetic-access")
		public void mouseClicked(MouseEvent evt) {
			final JComponent source = (JComponent) evt.getSource();

			int index;

			if (source instanceof JList) {
				index = SwingCommonsUtilities.loc2IndexFileList(list, evt.getPoint());
			} else if (source instanceof JTable) {
				final JTable table = (JTable) source;
				final Point p = evt.getPoint();
				index = table.rowAtPoint(p);

				if (SwingCommonsUtilities.pointOutsidePrefSize(table, index, table.columnAtPoint(p), p)) {
					return;
				}

				// Translate point from table to list
				if ((index >= 0) && (list != null) && listSelectionModel.isSelectedIndex(index)) {
					// Make a new event with the list as source, placing the
					// click in the corresponding list cell.
					final Rectangle r = list.getCellBounds(index, index);
					evt = new MouseEvent(list, evt.getID(), evt.getWhen(), evt.getModifiers(), r.x + 1,
							r.y + (r.height / 2), evt.getClickCount(), evt.isPopupTrigger(), evt.getButton());
				}
			} else {
				return;
			}

			if ((index >= 0) && SwingUtilities.isLeftMouseButton(evt)) {
				final VFSJFileChooser fc = getFileChooser();

				// For single click, we handle editing file name
				if ((evt.getClickCount() == 1) && source instanceof JList) {
					if ((!fc.isMultiSelectionEnabled() || (fc.getSelectedFileObjects().length <= 1)) && (index >= 0)
							&& listSelectionModel.isSelectedIndex(index) && (getEditIndex() == index)
							&& (editFile == null)) {
						editFileName(index);
					} else {
						if (index >= 0) {
							setEditIndex(index);
						} else {
							resetEditIndex();
						}
					}
				} else if (evt.getClickCount() == 2) {
					// System.out.println("double click");

					// on double click (open or drill down one directory) be
					// sure to clear the edit index
					resetEditIndex();
				}
			}

			// Forward event to Basic
			if (getDoubleClickListener() != null) {
				getDoubleClickListener().mouseClicked(evt);
			}
		}

		@Override
		public void mouseEntered(final MouseEvent evt) {
			final JComponent source = (JComponent) evt.getSource();

			if (source instanceof JTable) {
				final JTable table = (JTable) evt.getSource();

				final TransferHandler th1 = getFileChooser().getTransferHandler();
				final TransferHandler th2 = table.getTransferHandler();

				if (th1 != th2) {
					table.setTransferHandler(th1);
				}

				final boolean dragEnabled = getFileChooser().getDragEnabled();

				if (dragEnabled != table.getDragEnabled()) {
					table.setDragEnabled(dragEnabled);
				}
			} else if (source instanceof JList) {
				// Forward event to Basic
				if (getDoubleClickListener() != null) {
					getDoubleClickListener().mouseEntered(evt);
				}
			}
		}

		@Override
		public void mouseExited(final MouseEvent evt) {
			if (evt.getSource() instanceof JList) {
				// Forward event to Basic
				if (getDoubleClickListener() != null) {
					getDoubleClickListener().mouseExited(evt);
				}
			}
		}

		@Override
		public void mousePressed(final MouseEvent evt) {
			if (evt.getSource() instanceof JList) {
				// Forward event to Basic
				if (getDoubleClickListener() != null) {
					getDoubleClickListener().mousePressed(evt);
				}
			}
		}

		@Override
		public void mouseReleased(final MouseEvent evt) {
			if (evt.getSource() instanceof JList) {
				// Forward event to Basic
				if (getDoubleClickListener() != null) {
					getDoubleClickListener().mouseReleased(evt);
				}
			}
		}

		@SuppressWarnings("synthetic-access")
		private MouseListener getDoubleClickListener() {
			// Lazy creation of Basic's listener
			if ((doubleClickListener == null) && (list != null)) {
				doubleClickListener = fileChooserUIAccessor.createDoubleClickListener(list);
			}

			return doubleClickListener;
		}
	}

	class DetailsTableModel extends AbstractTableModel implements ListDataListener {
		public static final long ONE_KB = 1024;
		public static final long ONE_MB = ONE_KB * ONE_KB;
		public static final long ONE_GB = ONE_KB * ONE_MB;
		protected int sortCol = 0;
		protected boolean isSortAsc = true;

		private VFSJFileChooser chooser;
		private final BasicVFSDirectoryModel directoryModel;
		private final List<String> columns;
		private final int columnsCount = 3;
		int[] columnMap;
		DateFormat df;

		@SuppressWarnings("synthetic-access")
		final String[] headers = { fileNameHeaderText, fileSizeHeaderText, fileDateHeaderText };

		DetailsTableModel(final VFSJFileChooser fc) {
			chooser = fc;
			directoryModel = getModel();
			directoryModel.addListDataListener(this);
			columns = new ArrayList<String>(headers.length);
			df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, chooser.getLocale());

			for (final String header : headers) {
				columns.add(header);
			}

			updateColumnInfo();
		}

		void updateColumnInfo() {
			// .
		}

		@Override
		public int getRowCount() {
			return directoryModel.getSize();
		}

		@Override
		public int getColumnCount() {
			return columnsCount;
		}

		@Override
		public Object getValueAt(final int row, final int col) {
			// Note: It is very important to avoid getting info on drives, as
			// this will trigger "No disk in A:" and similar dialogs.
			//
			// Use (f.exists() &&
			// !chooser.getFileSystemView().isFileSystemRoot(f)) to
			// determine if it is safe to call methods directly on f.
			return getFileColumnValue((FileObject) directoryModel.getElementAt(row), col);
		}

		@SuppressWarnings("boxing")
		private Object getFileColumnValue(final FileObject f, final int col) {
			Object o = f;

			try {
				switch (col) {
				case COLUMN_FILENAME:
					o = f;

					break;

				case COLUMN_SIZE:

					if (VFSUtils.isDirectory(f)) {
						o = null;
					} else {
						o = Long.parseLong("" + f.getContent().getSize());
					}

					break;

				case COLUMN_DATE:
					o = f.getContent().getLastModifiedTime() + "";
					o = new Date(Long.parseLong(o.toString()));

					break;
				}
			} catch (final Exception e) {
				// .
			}

			return o;
		}

		@Override
		public void setValueAt(final Object value, final int row, final int col) {
			if (col == COLUMN_FILENAME) {
				chooser = getFileChooser();
				final FileObject f = (FileObject) getValueAt(row, col);

				if (f != null) {
					final String oldDisplayName = chooser.getName(f);
					final String oldFileName = f.getName().getBaseName();
					final String newDisplayName = ((String) value).trim();
					String newFileName;

					if (!newDisplayName.equals(oldDisplayName)) {
						newFileName = newDisplayName;

						// Check if extension is hidden from user
						final int i1 = oldFileName.length();
						final int i2 = oldDisplayName.length();

						if ((i1 > i2) && (oldFileName.charAt(i2) == '.')) {
							newFileName = newDisplayName + oldFileName.substring(i2);
						}

						// rename
						final AbstractVFSFileSystemView fsv = chooser.getFileSystemView();
						final FileObject f2 = fsv.createFileObject(VFSUtils.getParentDirectory(f), newFileName);

						if (!VFSUtils.exists(f2) && getModel().renameFile(f, f2)) {
							if (fsv.isParent(chooser.getCurrentDirectoryObject(), f2)) {
								if (chooser.isMultiSelectionEnabled()) {
									chooser.setSelectedFileObjects(new FileObject[] { f2 });
								} else {
									chooser.setSelectedFileObject(f2);
								}
							}
						}
					}
				}
			}
		}

		@SuppressWarnings({ "unused", "synthetic-access" })
		@Override
		public boolean isCellEditable(final int row, final int column) {
			final FileObject currentDirectory = getFileChooser().getCurrentDirectoryObject();

			return (!readOnly && (column == COLUMN_FILENAME) && canWrite(currentDirectory));
		}

		@Override
		@SuppressWarnings("unused")
		public void contentsChanged(final ListDataEvent e) {
			// Update the selection after the model has been updated
			new DelayedSelectionUpdater();
			fireTableDataChanged();
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public void intervalAdded(final ListDataEvent e) {
			final int i0 = e.getIndex0();
			final int i1 = e.getIndex1();

			if (i0 == i1) {
				final FileObject file = (FileObject) getModel().getElementAt(i0);

				if (file.getName().equals(newFolderFile.getName())) {
					new DelayedSelectionUpdater(file);
					newFolderFile = null;
				}
			}

			fireTableRowsInserted(e.getIndex0(), e.getIndex1());
		}

		@Override
		public void intervalRemoved(final ListDataEvent e) {
			fireTableRowsDeleted(e.getIndex0(), e.getIndex1());
		}

		@Override
		public String getColumnName(final int column) {
			String str = columns.get(column);

			if (column == sortCol) {
				str += (isSortAsc ? " >>" : " <<");
			}

			return str;
		}

		public List<String> getColumns() {
			return columns;
		}

		class ColumnListener extends MouseAdapter {

			@SuppressWarnings("synthetic-access")
			@Override
			public void mouseClicked(final MouseEvent e) {
				final TableColumnModel colModel = detailsTable.getColumnModel();
				final int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
				final int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

				if (modelIndex < 0) {
					return;
				}

				if (sortCol == modelIndex) {
					isSortAsc = !isSortAsc;
				} else {
					sortCol = modelIndex;
				}

				for (int i = 0; i < columnsCount; i++) {
					final TableColumn column = colModel.getColumn(i);
					column.setHeaderValue(getColumnName(column.getModelIndex()));
				}

				detailsTable.getTableHeader().repaint();

				Comparator<FileObject> cpt = FileObjectComparatorFactory.newFileNameComparator(isSortAsc);

				if (modelIndex == 1) {
					cpt = FileObjectComparatorFactory.newSizeComparator(isSortAsc);
				} else if (modelIndex == 2) {
					cpt = FileObjectComparatorFactory.newDateComparator(isSortAsc);
				}

				directoryModel.sort(cpt);

				detailsTable.tableChanged(new TableModelEvent(DetailsTableModel.this));
				detailsTable.revalidate();
				detailsTable.repaint();
			}
		}
	}

	private static class AlignableTableHeaderRenderer implements TableCellRenderer {
		TableCellRenderer wrappedRenderer;

		public AlignableTableHeaderRenderer(final TableCellRenderer wrappedRenderer) {
			this.wrappedRenderer = wrappedRenderer;
		}

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column) {
			final Component c = wrappedRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);

			if (c instanceof JLabel) {
				((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
			}

			return c;
		}
	}
}
// CHECKSTYLE:ON