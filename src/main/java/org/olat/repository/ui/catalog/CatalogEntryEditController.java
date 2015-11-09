/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.repository.ui.catalog;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.repository.CatalogEntry;
import org.olat.repository.CatalogEntry.Style;
import org.olat.repository.manager.CatalogManager;


/**
 * Description: <br>
 * The form allows to edit, create respective, a new catalog entry, which is
 * either a category or an alias for the linked repository entry. Further it is
 * abused as input form for import feature within the catalog.
 * <p>
 * 
 * Initial Date: Oct 3, 2004 <br>
 * @author patrick
 */

public class CatalogEntryEditController extends FormBasicController {
	
	private static final int picUploadlimitKB = 5024;
	
	private static final Set<String> mimeTypes = new HashSet<String>();
	static {
		mimeTypes.add("image/gif");
		mimeTypes.add("image/jpg");
		mimeTypes.add("image/jpeg");
		mimeTypes.add("image/png");
	}
	
	private static final String[] styleKeys = new String[]{
		Style.tiles.name(), Style.list.name(), Style.compact.name()
	};

	private TextElement nameEl;
	private SingleSelection styleEl;
	private RichTextElement descriptionEl;
	private FormLink deleteImage;
	private FileElement fileUpload;

	private CatalogEntry parentEntry;
	private CatalogEntry catalogEntry;
	private final CatalogManager catalogManager;
	
	public CatalogEntryEditController(UserRequest ureq, WindowControl wControl, CatalogEntry entry) {
		this(ureq, wControl, entry, null);
	}
	
	public CatalogEntryEditController(UserRequest ureq, WindowControl wControl, CatalogEntry entry, CatalogEntry parentEntry) {
		super(ureq, wControl);
		
		this.catalogEntry = entry;
		this.parentEntry = parentEntry;
		catalogManager = CoreSpringFactory.getImpl(CatalogManager.class);
		initForm (ureq);
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormStyle("o_catalog");
		String name = catalogEntry == null ? "" : catalogEntry.getName();
		nameEl = uifactory.addTextElement("name", "entry.category", 255, name, formLayout);
		nameEl.setMandatory(true);
		nameEl.setNotEmptyCheck("form.legende.mandatory");
		
		String desc = catalogEntry == null ? "" : catalogEntry.getDescription();
		descriptionEl = uifactory.addRichTextElementForStringDataMinimalistic("description", "entry.description", desc, 10, -1, formLayout, getWindowControl());
		
		String[] styleValues = new String[]{ translate("tiles"), translate("list"), translate("list.compact")};
		styleEl = uifactory.addDropdownSingleselect("style", "style", formLayout, styleKeys, styleValues, null);
		Style style = catalogEntry == null ? null : catalogEntry.getStyle();
		if(style != null) {
			for(String styleKey:styleKeys) {
				if(styleKey.equals(style.name())) {
					styleEl.select(styleKey, true);
				}
			}
		}
		if(!styleEl.isOneSelected()) {
			styleEl.select(styleKeys[0], true);
		}
		
		VFSLeaf img = catalogEntry == null || catalogEntry.getKey() == null ? null : catalogManager.getImage(catalogEntry);
		fileUpload = uifactory.addFileElement("entry.pic", "entry.pic", formLayout);
		fileUpload.setMaxUploadSizeKB(picUploadlimitKB, null, null);
		fileUpload.addActionListener(FormEvent.ONCHANGE);
		fileUpload.setPreview(ureq.getUserSession(), true);
		fileUpload.setCropSelectionEnabled(true);
		if(img instanceof LocalFileImpl) {
			fileUpload.setInitialFile(((LocalFileImpl)img).getBasefile());
		}
		fileUpload.limitToMimeType(mimeTypes, null, null);

		deleteImage = uifactory.addFormLink("delete", "tools.delete.catalog.image", null, formLayout, Link.BUTTON_SMALL);
		deleteImage.setElementCssClass("o_catalog_delete_img");
		deleteImage.setIconLeftCSS("o_icon o_icon_delete o_icon-lg");
		deleteImage.setVisible(img != null);

		FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		buttonLayout.setElementCssClass("o_sel_catalog_entry_form_buttons");
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton("submit", buttonLayout);
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());
	}
	
	public CatalogEntry getEditedCatalogEntry() {
		return catalogEntry;
	}

	@Override
	protected void doDispose() {
		//
	}
	
	public void setElementCssClass(String cssClass) {
		flc.setElementCssClass(cssClass);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		 if (source == fileUpload) {
			if (fileUpload.isUploadSuccess()) {
				deleteImage.setVisible(true);
				fileUpload.setLabel(null, null);
				flc.setDirty(true);
			}
		} else if (source == deleteImage) {
			VFSLeaf img = catalogManager.getImage(catalogEntry);
			if(fileUpload.getUploadFile() != null && fileUpload.getUploadFile() != fileUpload.getInitialFile()) {
				fileUpload.reset();
				
				if(img == null) {
					deleteImage.setVisible(false);
					fileUpload.setLabel("entry.pic", null);
				} else {
					deleteImage.setVisible(true);
					fileUpload.setLabel(null, null);
				}
			} else if(img != null) {
				catalogManager.deleteImage(catalogEntry);
				deleteImage.setVisible(false);
				fileUpload.setLabel("entry.pic", null);
				fileUpload.setInitialFile(null);
			}

			flc.setDirty(true);
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		catalogEntry.setName(nameEl.getValue());
		if(styleEl.isOneSelected()) {
			catalogEntry.setStyle(Style.valueOf(styleEl.getSelectedKey()));
		} else {
			catalogEntry.setStyle(null);
		}
		catalogEntry.setDescription(descriptionEl.getValue());
		
		if(catalogEntry.getKey() == null) {
			//a new one
			catalogEntry.setOwnerGroup(BaseSecurityManager.getInstance().createAndPersistSecurityGroup());
			catalogEntry.setRepositoryEntry(null);
			catalogEntry.setParent(parentEntry);
			catalogEntry = catalogManager.saveCatalogEntry(catalogEntry);
		} else {
			catalogEntry = catalogManager.updateCatalogEntry(catalogEntry);
		}
		
		File uploadedFile = fileUpload.getUploadFile();
		if(uploadedFile != null) {
			VFSContainer tmpHome = new LocalFolderImpl(new File(WebappHelper.getTmpDir()));
			VFSContainer container = tmpHome.createChildContainer(UUID.randomUUID().toString());
			VFSLeaf newFile = fileUpload.moveUploadFileTo(container, true);//give it it's real name and extension
			if(newFile != null) {
				boolean ok = catalogManager.setImage(newFile, catalogEntry);
				if (!ok) {
					showError("error.download.image");
				}
			} else {
				logError("Cannot move and or crop: " + fileUpload.getUploadFileName() + " ( " + fileUpload.getUploadMimeType() + " )", null);
				showError("error.download.image");
			}
		}
		
		fireEvent(ureq, Event.DONE_EVENT);
	}
	
	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
}

