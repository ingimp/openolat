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
package org.olat.modules.portfolio.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.Invitation;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailContext;
import org.olat.core.util.mail.MailContextImpl;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailManager;
import org.olat.core.util.mail.MailerResult;
import org.olat.modules.portfolio.Binder;
import org.olat.modules.portfolio.Page;
import org.olat.modules.portfolio.PortfolioElement;
import org.olat.modules.portfolio.PortfolioRoles;
import org.olat.modules.portfolio.PortfolioService;
import org.olat.modules.portfolio.Section;
import org.olat.modules.portfolio.model.AccessRightChange;
import org.olat.modules.portfolio.model.AccessRights;
import org.olat.portfolio.manager.InvitationDAO;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 
 * Initial date: 29.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class InvitationEditRightsController extends FormBasicController {
	
	private static final String[] theKeys = new String[]{ "xx" };
	private static final String[] theValues = new String[]{ "" };
	
	private FormLink removeLink;
	private TextElement firstNameEl, lastNameEl, mailEl;
	
	private int counter;
	
	private Binder binder;
	private Identity invitee;
	private Invitation invitation;
	private BinderAccessRightsRow binderRow;
	
	@Autowired
	private MailManager mailManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private InvitationDAO invitationDao;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private PortfolioService portfolioService;
	
	public InvitationEditRightsController(UserRequest ureq, WindowControl wControl, Binder binder) {
		super(ureq, wControl, "invitee_access_rights");
		this.binder = binder;
		invitation = invitationDao.createInvitation();
		initForm(ureq);
		loadModel();
	}
	
	public InvitationEditRightsController(UserRequest ureq, WindowControl wControl, Binder binder, Identity invitee) {
		super(ureq, wControl, "invitee_access_rights");
		this.binder = binder;
		this.invitee = invitee;
		invitation = invitationDao.findInvitation(binder.getBaseGroup(), invitee);
		initForm(ureq);
		loadModel();
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FormLayoutContainer inviteeCont = FormLayoutContainer.createDefaultFormLayout("inviteeInfos", getTranslator());
		inviteeCont.setRootForm(mainForm);
		formLayout.add("inviteeInfos", inviteeCont);
		
		firstNameEl = uifactory.addTextElement("firstName", "firstName", 64, invitation.getFirstName(), inviteeCont);
		firstNameEl.setMandatory(true);
		
		lastNameEl = uifactory.addTextElement("lastName", "lastName", 64, invitation.getLastName(), inviteeCont);
		lastNameEl.setMandatory(true);
		
		mailEl = uifactory.addTextElement("mail", "mail", 128, invitation.getMail(), inviteeCont);
		mailEl.setMandatory(true);
		mailEl.setNotEmptyCheck("map.share.empty.warn");
			
		if(StringHelper.containsNonWhitespace(invitation.getMail()) && MailHelper.isValidEmailAddress(invitation.getMail())) {
			SecurityGroup allUsers = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
			Identity currentIdentity = userManager.findIdentityByEmail(invitation.getMail());
			if(currentIdentity != null && securityManager.isIdentityInSecurityGroup(currentIdentity, allUsers)) {
				mailEl.setErrorKey("map.share.with.mail.error.olatUser", new String[]{ invitation.getMail() });
			}
		}
			
		String link = getInvitationLink();
		StaticTextElement linkEl = uifactory.addStaticTextElement("invitation.link" , link, inviteeCont);
		linkEl.setLabel("invitation.link", null);
		
		
		//binder
		MultipleSelectionElement accessEl = uifactory.addCheckboxesHorizontal("access-" + (counter++), null, formLayout, theKeys, theValues);
		binderRow = new BinderAccessRightsRow(accessEl, binder);
		
		//sections
		List<Section> sections = portfolioService.getSections(binder);
		Map<Long,SectionAccessRightsRow> sectionMap = new HashMap<>();
		for(Section section:sections) {
			MultipleSelectionElement sectionAccessEl = uifactory.addCheckboxesHorizontal("access-" + (counter++), null, formLayout, theKeys, theValues);
			SectionAccessRightsRow sectionRow = new SectionAccessRightsRow(sectionAccessEl, section);
			binderRow.getSections().add(sectionRow);
			sectionMap.put(section.getKey(), sectionRow);	
		}
		
		//pages
		List<Page> pages = portfolioService.getPages(binder, null);
		for(Page page:pages) {
			Section section = page.getSection();
			SectionAccessRightsRow sectionRow = sectionMap.get(section.getKey());
			
			MultipleSelectionElement pageAccessEl = uifactory.addCheckboxesHorizontal("access-" + (counter++), null, formLayout, theKeys, theValues);
			PortfolioElementAccessRightsRow pageRow = new PortfolioElementAccessRightsRow(pageAccessEl, page);
			sectionRow.getPages().add(pageRow);
		}
		
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			layoutCont.contextPut("binderRow", binderRow);
		}

		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		formLayout.add(buttonsCont);
		buttonsCont.setRootForm(mainForm);
		uifactory.addFormCancelButton("cancel", buttonsCont, ureq, getWindowControl());
		if(invitation.getKey() != null) {
			removeLink = uifactory.addFormLink("remove", buttonsCont, Link.BUTTON);
		}
		uifactory.addFormSubmitButton("save", buttonsCont);
	}
	
	private String getInvitationLink() {
		return Settings.getServerContextPathURI() + "/url/BinderInvitation/" + binder.getKey() + "?invitation=" + invitation.getToken();
	}
	
	private void loadModel() {
		if(invitee != null) {
			List<AccessRights> currentRights = portfolioService.getAccessRights(binder, invitee);
			
			binderRow.applyRights(currentRights);
			for(SectionAccessRightsRow sectionRow:binderRow.getSections()) {
				sectionRow.applyRights(currentRights);
				for(PortfolioElementAccessRightsRow pageRow:sectionRow.getPages()) {
					pageRow.applyRights(currentRights);
				}
			}
		}
	}
	
	@Override
	protected void doDispose() {
		// 
	}
	
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
	
		if (mailEl != null) {
			String mail = mailEl.getValue();
			if (StringHelper.containsNonWhitespace(mail)) {
				if (MailHelper.isValidEmailAddress(mail)) {
					SecurityGroup allUsers = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
					Identity currentIdentity = userManager.findIdentityByEmail(mail);
					if (currentIdentity != null && securityManager.isIdentityInSecurityGroup(currentIdentity, allUsers)) {
						mailEl.setErrorKey("map.share.with.mail.error.olatUser", new String[] { mail });
						allOk &= false;
					}
				} else {
					mailEl.setErrorKey("error.mail.invalid", null);
					allOk &= false;
				}
			} else {
				mailEl.setErrorKey("form.legende.mandatory", null);
				allOk &= false;
			}
		}
		
		return allOk & super.validateFormLogic(ureq);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		List<AccessRightChange> changes = getChanges();
		
		if(invitation.getKey() == null) {
			invitation.setFirstName(firstNameEl.getValue());
			invitation.setLastName(lastNameEl.getValue());
			invitation.setMail(mailEl.getValue());
			invitee = invitationDao.createIdentityAndPersistInvitation(invitation, binder.getBaseGroup(), getLocale());
			portfolioService.changeAccessRights(Collections.singletonList(invitee), changes);
			sendInvitation();
			fireEvent(ureq, Event.DONE_EVENT);
		} else {
			invitationDao.update(invitation, firstNameEl.getValue(), lastNameEl.getValue(), mailEl.getValue());
			portfolioService.changeAccessRights(Collections.singletonList(invitee), changes);
			fireEvent(ureq, Event.CHANGED_EVENT);
		}
	}
	
	public List<AccessRightChange> getChanges() {
		List<AccessRightChange> changes = new ArrayList<>();
		binderRow.appendChanges(changes, invitee);
		for(SectionAccessRightsRow sectionRow:binderRow.getSections()) {
			sectionRow.appendChanges(changes, invitee);
			for(PortfolioElementAccessRightsRow pageRow:sectionRow.getPages()) {
				pageRow.appendChanges(changes, invitee);
			}
		}
		return changes;
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(removeLink == source) {
			doRemoveInvitation();
			fireEvent(ureq, Event.DONE_EVENT);
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	private void doRemoveInvitation() {
		portfolioService.removeAccessRights(binder, invitee);
		invitationDao.deleteInvitation(invitation);
	}

	private void sendInvitation() {
		String inviteeEmail = invitee.getUser().getProperty(UserConstants.EMAIL, getLocale());
		ContactList contactList = new ContactList(inviteeEmail);
		contactList.add(inviteeEmail);
		String busLink = getInvitationLink();

		boolean success = false;
		try {
			String first = getIdentity().getUser().getProperty(UserConstants.FIRSTNAME, null);
			String last = getIdentity().getUser().getProperty(UserConstants.LASTNAME, null);
			String sender = first + " " + last;
			String[] bodyArgs = new String[]{busLink, sender};

			MailContext context = new MailContextImpl(binder, null, getWindowControl().getBusinessControl().getAsString()); 
			MailBundle bundle = new MailBundle();
			bundle.setContext(context);
			bundle.setFrom(WebappHelper.getMailConfig("mailReplyTo"));
			bundle.setContactList(contactList);
			bundle.setContent(translate("invitation.mail.subject"), translate("invitation.mail.body", bodyArgs));

			MailerResult result = mailManager.sendExternMessage(bundle, null, true);
			success = result.isSuccessful();
		} catch (Exception e) {
			logError("Error on sending invitation mail to contactlist, invalid address.", e);
		}
		if (success) {
			showInfo("invitation.mail.success");
		}	else {
			showError("invitation.mail.failure");			
		}
	}
	
	public static class BinderAccessRightsRow extends PortfolioElementAccessRightsRow {
		
		private final List<SectionAccessRightsRow> sections = new ArrayList<>();

		public BinderAccessRightsRow(MultipleSelectionElement accessEl, PortfolioElement element) {
			super(accessEl, element);
		}

		public List<SectionAccessRightsRow> getSections() {
			return sections;
		}
	}
	
	public static class SectionAccessRightsRow extends PortfolioElementAccessRightsRow {
		
		private final List<PortfolioElementAccessRightsRow> pages = new ArrayList<>();
		
		public SectionAccessRightsRow(MultipleSelectionElement accessEl, PortfolioElement element) {
			super(accessEl, element);
		}
		
		public List<PortfolioElementAccessRightsRow> getPages() {
			return pages;
		}
	}
	
	public static class PortfolioElementAccessRightsRow {
		
		private PortfolioElement element;
		private MultipleSelectionElement accessEl;
		
		public PortfolioElementAccessRightsRow(MultipleSelectionElement accessEl, PortfolioElement element) {
			this.element = element;
			this.accessEl = accessEl;
			accessEl.setUserObject(Boolean.FALSE);
		}
		
		public void appendChanges(List<AccessRightChange> changes, Identity identity) {
			if(accessEl.isAtLeastSelected(1)) {
				if(!Boolean.TRUE.equals(accessEl.getUserObject())) {
					changes.add(new AccessRightChange(PortfolioRoles.readInvitee, element, identity, true));
				}
			} else if(Boolean.TRUE.equals(accessEl.getUserObject())) {
				changes.add(new AccessRightChange(PortfolioRoles.readInvitee, element, identity, false));
			}
		}
		
		public void applyRights(List<AccessRights> rights) {
			for(AccessRights right:rights) {
				if(element instanceof Page) {
					if(element.getKey().equals(right.getPageKey())) {
						applyRight(right);
					}
				} else if(element instanceof Section) {
					if(element.getKey().equals(right.getSectionKey()) && right.getPageKey() == null) {
						applyRight(right);
					}
				} else if(element instanceof Binder) {
					if(element.getKey().equals(right.getBinderKey()) && right.getSectionKey() == null && right.getPageKey() == null) {
						applyRight(right);
					}
				}
			}
		}
		
		public void applyRight(AccessRights right) {
			if(right.getRole().equals(PortfolioRoles.readInvitee)) {
				accessEl.select("xx", true);
				accessEl.setUserObject(Boolean.TRUE);
			}
		}
		
		public String getTitle() {
			return element.getTitle();
		}

		public PortfolioElement getElement() {
			return element;
		}

		public MultipleSelectionElement getAccess() {
			return accessEl;
		}
	}
}