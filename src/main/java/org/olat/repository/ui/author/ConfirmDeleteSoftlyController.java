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
package org.olat.repository.ui.author;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.activity.LearningResourceLoggingAction;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.resource.references.ReferenceInfos;
import org.olat.resource.references.ReferenceManager;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 1 déc. 2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ConfirmDeleteSoftlyController extends FormBasicController {
	
	private FormLink deleteButton;
	private MultipleSelectionElement acknowledgeEl;
	
	private final int numOfMembers;
	private final boolean notAllDeleteable;
	private final List<RepositoryEntry> rows;
	private final List<ReferenceInfos> references;
	
	@Autowired
	private ReferenceManager referenceManager;
	@Autowired
	private RepositoryService repositoryService;
	
	public ConfirmDeleteSoftlyController(UserRequest ureq, WindowControl wControl, List<RepositoryEntry> rows, boolean notAllDeleteable) {
		super(ureq, wControl, "confirm_delete");
		setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));
		
		this.rows = rows;
		this.notAllDeleteable = notAllDeleteable;
		numOfMembers = repositoryService.countMembers(rows, getIdentity());
		references = referenceManager.getReferencesInfos(rows, getIdentity(), ureq.getUserSession().getRoles());
		if(references.size() > 1) {
			Collections.sort(references, new ReferenceInfosComparator(Collator.getInstance(getLocale())));
		}
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layout = (FormLayoutContainer)formLayout;
			layout.contextPut("notAllDeleteable", new Boolean(notAllDeleteable));
			layout.contextPut("numOfMembers", Integer.toString(numOfMembers));

			FormLayoutContainer layoutCont = FormLayoutContainer.createDefaultFormLayout("confirm", getTranslator());
			formLayout.add("confirm", layoutCont);
			layoutCont.setRootForm(mainForm);
			
			StringBuilder message = new StringBuilder();
			for(RepositoryEntry row:rows) {
				if(message.length() > 0) message.append(", ");
				message.append(StringHelper.escapeHtml(row.getDisplayname()));
			}
			uifactory.addStaticTextElement("rows", "details.delete.entries", message.toString(), layoutCont);

			String[] acknowledge = new String[] { translate("details.delete.acknowledge.msg") };
			acknowledgeEl = uifactory.addCheckboxesHorizontal("confirm", "details.delete.acknowledge", layoutCont, new String[]{ "" },  acknowledge);
			
			FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
			layoutCont.add(buttonsCont);
			deleteButton = uifactory.addFormLink("details.delete", buttonsCont, Link.BUTTON);
			uifactory.addFormCancelButton("cancel", buttonsCont, ureq, getWindowControl());
		}
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		acknowledgeEl.clearError();
		if(!acknowledgeEl.isAtLeastSelected(1)) {
			acknowledgeEl.setErrorKey("details.delete.acknowledge.error", null);
			allOk &= false;
		}
		
		return allOk & super.validateFormLogic(ureq);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(deleteButton == source) {
			if(validateFormLogic(ureq)) {
				doCompleteDelete();
				fireEvent(ureq, Event.DONE_EVENT);
			}
		}
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
	
	private void doCompleteDelete() {
		boolean allOk = deleteEntries(rows);
		if(allOk) {
			showInfo("info.entry.deleted");
		} else {
			showWarning("info.could.not.delete.entry");
		}
	}
	
	private boolean deleteEntries(List<RepositoryEntry> entries) {
		boolean allOk = true;
		for(RepositoryEntry entry:entries) {
			RepositoryEntry reloadedEntry = repositoryService.loadByKey(entry.getKey());
			if(reloadedEntry != null) {
				reloadedEntry = repositoryService.deleteSoftly(reloadedEntry);
				ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_TRASH, getClass(),
						LoggingResourceable.wrap(reloadedEntry, OlatResourceableType.genRepoEntry));
			}
		}
		return allOk;
	}
	
	private static class ReferenceInfosComparator implements Comparator<ReferenceInfos> {
		
		private final Collator collator;
		
		public ReferenceInfosComparator(Collator collator) {
			this.collator = collator;
		}

		@Override
		public int compare(ReferenceInfos o1, ReferenceInfos o2) {
			if(o1 == null) return -1;
			if(o2 == null) return 1;
			
			String name1 = o1.getEntry() == null ? null : o2.getEntry().getDisplayname();
			String name2 = o2.getEntry() == null ? null : o2.getEntry().getDisplayname();
			
			if(name1 == null) return -1;
			if(name2 == null) return 1;
			return collator.compare(name1, name2);
		}
	}
}