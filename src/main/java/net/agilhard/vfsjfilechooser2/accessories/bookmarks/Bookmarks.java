// CHECKSTYLE:OFF
/*
 *
 * Copyright (C) 2005-2008 Yves Zoundi
 * Copyright (C) 2005-2008 Stan Love
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
package net.agilhard.vfsjfilechooser2.accessories.bookmarks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import net.agilhard.vfsjfilechooser2.constants.VFSJFileChooserConstants;
import net.agilhard.vfsjfilechooser2.utils.VFSResources;
import net.agilhard.vfsjfilechooser2.utils.VFSURIValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bookmarks table model
 *
 * @author Dirk Moebius (JEdit)
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 * @author Stan Love
 * @version 0.0.5
 */
public class Bookmarks extends AbstractTableModel {

	/** The Logger. */
	private final Logger log = LoggerFactory.getLogger(Bookmarks.class);

	private static final long serialVersionUID = 6142063286592461932L;

	private static final String COLUMN_TITLE_NAME = VFSResources.getMessage("VFSJFileChooser.fileNameHeaderText");

	private static final String COLUMN_URL_NAME = VFSResources.getMessage("VFSJFileChooser.pathLabelText");

	private static final int COLUMN_TITLE_INDEX = 0;

	private static final int COLUMN_URL_INDEX = 1;

	private static final int NB_COLUMNS = 2;

	private final List<TitledURLEntry> entries = new ArrayList<TitledURLEntry>();

	private final File favorites = VFSJFileChooserConstants.BOOKMARKS_FILE;

	private transient final BookmarksWriter writer = new BookmarksWriter();

	/**
	 *
	 */
	public Bookmarks() {
		if (!VFSJFileChooserConstants.CONFIG_DIRECTORY.exists()) {
			if (!VFSJFileChooserConstants.CONFIG_DIRECTORY.mkdirs()) {
				log.error("Unable to create config directory");
			}
		}

		final List<TitledURLEntry> values = load();

		for (final TitledURLEntry entry : values) {
			add(entry);
		}
	}

	/**
	 * @param e
	 */
	public void add(final TitledURLEntry e) {
		synchronized (entries) {
			entries.add(e);
			fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
			// sl start
			final VFSURIValidator v = new VFSURIValidator();
			if (!v.isValid(e.getURL())) {
				// popup a warning
				JOptionPane.showMessageDialog(null, VFSResources.getMessage("VFSFileChooser.errBADURI"));
				// System.out.println("bad uri -- add");
			} else {
				// System.out.println("Good uri -- add");
			}
			// sl stop
			save(); // sl
		}
	}

	/**
	 * @return
	 */
	public int getSize() {
		return entries.size();
	}

	/**
	 * @param index
	 * @return
	 */
	public String getTitle(final int index) {
		final TitledURLEntry e = getEntry(index);

		return (e == null) ? null : e.getTitle();
	}

	/**
	 * @param index
	 * @return
	 */
	public String getURL(final int index) {
		final TitledURLEntry e = getEntry(index);

		return (e == null) ? null : e.getURL();
	}

	/**
	 * @param index
	 * @return
	 */
	public TitledURLEntry getEntry(final int index) {
		if ((index < 0) || (index > entries.size())) {
			return null;
		}

		synchronized (entries) {
			final TitledURLEntry e = entries.get(index);

			return e;
		}
	}

	/**
	 * @param row
	 */
	public void delete(final int row) {
		synchronized (entries) {
			entries.remove(row);
			fireTableRowsDeleted(row, row);
			save(); // sl
		}
	}

	/**
	 * @param row
	 */
	public void moveup(final int row) {
		if (row == 0) {
			return;
		}

		final TitledURLEntry b = getEntry(row);

		synchronized (entries) {
			entries.remove(row);
			entries.add(row - 1, b);
			// sl start
			final VFSURIValidator v = new VFSURIValidator();
			if (!v.isValid(b.getURL())) {
				// popup a warning
				JOptionPane.showMessageDialog(null, VFSResources.getMessage("VFSFileChooser.errBADURI"));
				// System.out.println("moveup -- baduri");
			} else {
				// System.out.println("moveup -- good uri");
			}
			// sl stop
			save(); // sl
		}

		fireTableRowsUpdated(row - 1, row);
	}

	public void movedown(final int row) {
		if (row == (entries.size() - 1)) {
			return;
		}

		final TitledURLEntry b = getEntry(row);

		synchronized (entries) {
			entries.remove(row);
			entries.add(row + 1, b);
			// sl start
			final VFSURIValidator v = new VFSURIValidator();
			if (!v.isValid(b.getURL())) {
				// popup a warning
				JOptionPane.showMessageDialog(null, VFSResources.getMessage("VFSFileChooser.errBADURI"));
				// System.out.println("movedown -- bad uri");
			} else {
				// System.out.println("movedown -- good uri");
			}
			// sl stop
			save(); // sl
		}

		fireTableRowsUpdated(row, row + 1);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return NB_COLUMNS;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		return entries.size();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(final int row, final int col) {
		Object obj = null;

		if (row < entries.size()) {
			if (col == 0) {
				obj = getEntry(row).getTitle();
			} else if (col == 1) {
				obj = getEntry(row).getURL();
			}
		}

		return obj;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	@SuppressWarnings("unused")
	@Override
	public boolean isCellEditable(final int row, final int col) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object,
	 * int, int)
	 */
	@Override
	public void setValueAt(final Object value, final int row, final int col) {
		final TitledURLEntry e = getEntry(row);

		if (col == COLUMN_TITLE_INDEX) {
			e.setTitle(value.toString());
		} else if (col == COLUMN_URL_INDEX) {
			e.setURL(value.toString());
		}

		fireTableRowsUpdated(row, row);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(final int index) {
		return (index == COLUMN_TITLE_INDEX) ? COLUMN_TITLE_NAME : COLUMN_URL_NAME;
	}

	public List<TitledURLEntry> load() {
		try {
			if (favorites.exists()) {
				return new BookmarksReader(favorites).getParsedEntries();
			}
			writeDefaultFavorites();

		} catch (final Exception e) {
			log.warn("Rebuilding bookmarks: ", e);
			writeDefaultFavorites();
		}

		return new ArrayList<TitledURLEntry>();
	}

	private void writeDefaultFavorites() {
		try {
			writer.writeToFile(new ArrayList<TitledURLEntry>(0), favorites);
		} catch (final IOException ex) {
			log.error("Unable to write bookmarks:", ex);
		} catch (final Exception e) {
			log.error("Unable to write bookmarks:", e);
		}
	}

	// end AbstractTableModel implementation
	public void save() {
		try {
			writer.writeToFile(entries, favorites);
		} catch (final IOException ex) {
			log.error("Unable to write bookmarks:", ex);
		} catch (final Exception e) {
			log.error("Unable to write bookmarks:", e);
		}
	}
}
// CHECKSTYLE:ON