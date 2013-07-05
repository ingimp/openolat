/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.commons.controllers.filechooser;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.tree.SelectionTree;
import org.olat.core.gui.components.tree.TreeEvent;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.folder.FolderTreeModel;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.filters.VFSItemFilter;

/**
 * Description:<br>
 * The file chooser controller allows selecting of files or directories
 * depending on the configuration. The controller offers a modern ajax based
 * dynamic tree and uses a static fallback for non ajax browsers
 * <p>
 * Note that the rootContainer can contain NamedContainers, however only on the
 * root level! The current implementation does not support hierarchical use of
 * NamedContainers.
 * <p>
 * Events fired by this controller:
 * <ul>
 * <li>FileChoosenEvent</li>
 * <li>Event.FAILED</li>
 * <li>Event.CANCELLED</li>
 * </ul>
 * In case of an Event.DONE you can use the getSelectedItem() method to get the
 * vfs item that was selected by the user
 * <P>
 * Initial Date: 12.06.2008 <br>
 * 
 * @author gnaegi
 */
public class FileChooserController extends BasicController {

	private Link cancelLink, selectLink;
	private SelectionTree selectionTree;
	private FolderTreeModel treeModel;

	private VFSItem selectedItem;
	private VFSContainer rootContainer;
	private final boolean onlyLeafsSelectable;

	/**
	 * Constructor that allows the usage of a custom vfs item filter
	 * 
	 * @param ureq
	 * @param wControl
	 * @param rootContainer
	 *            The root container that should be selected from
	 * @param customItemFilter
	 *            The custom filter to be used or NULL to not use any filter at
	 *            all
	 * @param onlyLeafsSelectable true: container elements can't be selected;
	 *          false: all items can be selected
	 * @param showTitle
	 *            true: show a file chooser title and description; false: show
	 *            only the tree without a title
	 */
	FileChooserController(UserRequest ureq, WindowControl wControl, VFSContainer rootContainer, VFSItemFilter customItemFilter, boolean onlyLeafsSelectable, boolean showTitle) {
		super(ureq, wControl);
		this.rootContainer = rootContainer;
		this.onlyLeafsSelectable = onlyLeafsSelectable;

		treeModel = new FolderTreeModel(ureq.getLocale(), rootContainer,  false, true, !onlyLeafsSelectable, false, customItemFilter);
		selectionTree = new SelectionTree("stTree", getTranslator());
		selectionTree.setTreeModel(treeModel);
		selectionTree.addListener(this);
		selectionTree.setFormButtonKey("select");
		putInitialPanel(selectionTree);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component,
	 *      org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// events from ajax tree view
		if (source == cancelLink) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
		} else if (source == selectLink) {
			if (selectedItem != null) {
				//fxdiff FXOLAT-125: virtual file system for CP
				if (onlyLeafsSelectable && !(selectedItem instanceof VFSLeaf)) {
					showWarning("filechooser.tree.error.only.leafs", selectedItem.getName());
				} else {
					fireEvent(ureq, new FileChoosenEvent(selectedItem));					
				}
			} else {
				fireEvent(ureq, Event.FAILED_EVENT);
			}
		} else if (source == selectionTree) {
			TreeEvent te = (TreeEvent) event;
			if (te.getCommand().equals(TreeEvent.COMMAND_TREENODE_CLICKED)) {
				String selectedPath = treeModel.getSelectedPath(selectionTree.getSelectedNode());
				selectedItem = rootContainer.resolve(selectedPath);
				if (selectedItem != null) {
					fireEvent(ureq, new FileChoosenEvent(selectedItem));
				} else {
					fireEvent(ureq, Event.FAILED_EVENT);
				}
			} else if (te.getCommand().equals(TreeEvent.COMMAND_CANCELLED)) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		}
	}
	
	@Override
	protected void doDispose() {
		// Controllers auto disposed by basic controller. NULL composite objects to help GC
		cancelLink = null;
		selectLink = null;
		treeModel = null;
		selectionTree = null;
	}
}