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
package org.olat.modules.portfolio.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.TypedQuery;

import org.olat.basesecurity.Group;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityRef;
import org.olat.basesecurity.manager.GroupDAO;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.modules.portfolio.Assignment;
import org.olat.modules.portfolio.AssignmentStatus;
import org.olat.modules.portfolio.Binder;
import org.olat.modules.portfolio.BinderRef;
import org.olat.modules.portfolio.PortfolioRoles;
import org.olat.modules.portfolio.Section;
import org.olat.modules.portfolio.SectionRef;
import org.olat.modules.portfolio.SectionStatus;
import org.olat.modules.portfolio.model.AccessRights;
import org.olat.modules.portfolio.model.BinderImpl;
import org.olat.modules.portfolio.model.BinderRow;
import org.olat.modules.portfolio.model.SectionImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.resource.OLATResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 06.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class BinderDAO {

	@Autowired
	private DB dbInstance;
	@Autowired
	private GroupDAO groupDao;
	@Autowired
	private AssignmentDAO assignmentDao;
	
	public BinderImpl createAndPersist(String title, String summary, String imagePath, RepositoryEntry entry) {
		BinderImpl binder = new BinderImpl();
		binder.setCreationDate(new Date());
		binder.setLastModified(binder.getCreationDate());
		binder.setTitle(title);
		binder.setSummary(summary);
		binder.setImagePath(imagePath);
		binder.setBaseGroup(groupDao.createGroup());
		if(entry != null) {
			binder.setOlatResource(entry.getOlatResource());
		}
		dbInstance.getCurrentEntityManager().persist(binder);
		return binder;
	}
	
	public BinderImpl createCopy(BinderImpl template, RepositoryEntry entry, String subIdent) {
		BinderImpl binder = new BinderImpl();
		binder.setCreationDate(new Date());
		binder.setLastModified(binder.getCreationDate());
		binder.setTitle(template.getTitle());
		binder.setSummary(template.getSummary());
		binder.setImagePath(template.getImagePath());
		binder.setBaseGroup(groupDao.createGroup());
		binder.setTemplate(template);
		binder.setCopyDate(binder.getCreationDate());
		if(entry != null) {
			binder.setEntry(entry);
		}
		if(StringHelper.containsNonWhitespace(subIdent)) {
			binder.setSubIdent(subIdent);
		}
		dbInstance.getCurrentEntityManager().persist(binder);
		binder.getSections().size();
		
		for(Section templateSection:template.getSections()) {
			Section section = createInternalSection(binder, templateSection);
			binder.getSections().add(section);
			dbInstance.getCurrentEntityManager().persist(section);
		}
		
		binder = dbInstance.getCurrentEntityManager().merge(binder);
		return binder;
	}
	
	public Binder syncWithTemplate(BinderImpl template, BinderImpl binder, AtomicBoolean changes) {
		List<Section> templateSections = template.getSections();
		List<Section> currentSections = binder.getSections();
		
		if(needUpdate(templateSections, currentSections)) {
			Map<Section,Section> templateToSectionsMap = new HashMap<>();
			for(Section currentSection:currentSections) {
				Section templateRef = currentSection.getTemplateReference();
				if(templateRef != null) {
					templateToSectionsMap.put(templateRef, currentSection);
				}
			}
			
			currentSections.clear();
			for(int i=0; i<templateSections.size(); i++) {
				Section templateSection = templateSections.get(i);
				Section currentSection = templateToSectionsMap.remove(templateSection);
				if(currentSection != null) {
					currentSections.add(currentSection);
					syncAssignments(templateSection, currentSection);
				} else {
					Section section = createInternalSection(binder, templateSection);
					currentSections.add(section);
					dbInstance.getCurrentEntityManager().persist(section);
					syncAssignments(templateSection, section);
				}
			}
			
			for(Section leadingSection:templateToSectionsMap.values()) {
				currentSections.add(leadingSection);
			}
	
			binder = dbInstance.getCurrentEntityManager().merge(binder);
			changes.set(true);
		}
		
		return binder;
	}
	
	private void syncAssignments(Section templateSection, Section currentSection) {
		List<Assignment> templateAssignments = new ArrayList<>(assignmentDao.loadAssignments(templateSection));
		
		List<Assignment> currentAssignments = assignmentDao.loadAssignments(currentSection);
		for(Assignment currentAssignment:currentAssignments) {
			Assignment refAssignment = currentAssignment.getTemplateReference();
			if(!templateAssignments.remove(refAssignment)) {
				currentAssignment.setAssignmentStatus(AssignmentStatus.deleted);
			} else {
				//TODO sync summary
			}
		}
		
		for(Assignment templateAssignment:templateAssignments) {
			assignmentDao.createAssignment(templateAssignment, AssignmentStatus.notStarted, currentSection);
		}
	}
	
	private boolean needUpdate(List<Section> templateSections, List<Section> currentSections) {
		/*boolean same = true;
		if(templateSections.size() == currentSections.size()) {
			for(int i=templateSections.size(); i-->0; ) {
				Section templateSection = templateSections.get(i);
				Section currentSection = currentSections.get(i);
				
				if(!templateSection.equals(currentSection.getTemplateReference())) {
					same &= false;
				}
			}
		} else {
			same &= false;
		}*/
		return true;
	}
	
	private Section createInternalSection(Binder binder, Section templateSection) {
		SectionImpl section = new SectionImpl();
		section.setCreationDate(new Date());
		section.setLastModified(section.getCreationDate());
		section.setBaseGroup(groupDao.createGroup());
		section.setTitle(templateSection.getTitle());
		section.setDescription(templateSection.getDescription());
		section.setBeginDate(templateSection.getBeginDate());
		section.setEndDate(templateSection.getEndDate());
		section.setStatus(SectionStatus.notStarted.name());
		section.setBinder(binder);
		section.setTemplateReference(templateSection);
		return section;
	}
	
	public Binder updateBinder(Binder binder) {
		((BinderImpl)binder).setLastModified(new Date());
		return dbInstance.getCurrentEntityManager().merge(binder);
	}
	
	public List<Binder> getOwnedBinders(IdentityRef owner) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder from pfbinder as binder")
		  .append(" inner join fetch binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where membership.identity.key=:identityKey and membership.role=:role");
		
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Binder.class)
			.setParameter("identityKey", owner.getKey())
			.setParameter("role", GroupRoles.owner.name())
			.getResultList();
	}
	
	public void detachBinderTemplate() {
		//unlink entry
		//unlink template
	}
	
	public int deleteBinderTemplate(Binder binder) {
		String sectionQ = "delete from pfsection section where section.binder.key=:binderKey";
		int sections = dbInstance.getCurrentEntityManager()
				.createQuery(sectionQ)
				.setParameter("binderKey", binder.getKey())
				.executeUpdate();

		String binderQ = "delete from pfbinder binder where binder.key=:binderKey";
		int binders = dbInstance.getCurrentEntityManager()
				.createQuery(binderQ)
				.setParameter("binderKey", binder.getKey())
				.executeUpdate();
		return sections + binders;
	}
	
	/**
	 * The same type of query is user for the categories
	 * @param owner
	 * @return
	 */
	public List<BinderRow> searchOwnedBinders(IdentityRef owner) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder.key, binder.title, binder.imagePath, binder.lastModified, binder.status,")
		  .append(" (select count(section.key) from pfsection as section")
		  .append("   where section.binder.key=binder.key")
		  .append(" ) as numOfSections,")
		  .append(" (select count(page.key) from pfpage as page, pfsection as pageSection")
		  .append("   where pageSection.binder.key=binder.key and page.section.key=pageSection.key")
		  .append(" ) as numOfPages,")
		  .append(" (select count(comment.key) from usercomment as comment, pfpage as page, pfsection as pageSection")
		  .append("   where pageSection.binder.key=binder.key and page.section.key=pageSection.key and comment.resId=page.key and comment.resName='Page'")
		  .append(" ) as numOfComments")
		  .append(" from pfbinder as binder")
		  .append(" inner join binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where binder.olatResource is null and membership.identity.key=:identityKey and membership.role=:role");
		
		List<Object[]> objects = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Object[].class)
			.setParameter("identityKey", owner.getKey())
			.setParameter("role", GroupRoles.owner.name())
			.getResultList();
		
		List<BinderRow> rows = new ArrayList<>(objects.size());
		for(Object[] object:objects) {
			int pos = 0;
			Long key = (Long)object[pos++];
			String title = (String)object[pos++];
			String imagePath = (String)object[pos++];
			Date lastModified = (Date)object[pos++];
			String status = (String)object[pos++];
			int numOfSections = ((Number)object[pos++]).intValue();
			int numOfPages = ((Number)object[pos++]).intValue();
			int numOfComments = ((Number)object[pos++]).intValue();
			rows.add(new BinderRow(key, title, imagePath, lastModified, numOfSections, numOfPages, status, numOfComments));
		}
		return rows;
	}
	
	public Binder loadByKey(Long key) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder from pfbinder as binder")
		  .append(" inner join fetch binder.baseGroup as baseGroup")
		  .append(" where binder.key=:portfolioKey");
		
		List<Binder> binders = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Binder.class)
			.setParameter("portfolioKey", key)
			.getResultList();
		return binders == null || binders.isEmpty() ? null : binders.get(0);
	}
	
	public Binder loadByResource(OLATResource resource) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder from pfbinder as binder")
		  .append(" inner join fetch binder.baseGroup as baseGroup")
		  .append(" inner join fetch binder.olatResource as olatResource")
		  .append(" where olatResource.key=:resourceKey");
		
		List<Binder> binders = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Binder.class)
			.setParameter("resourceKey", resource.getKey())
			.getResultList();
		return binders == null || binders.isEmpty() ? null : binders.get(0);
	}
	
	public Binder loadBySection(SectionRef section) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder from pfsection as section")
		  .append(" inner join section.binder as binder")
		  .append(" inner join fetch binder.baseGroup as baseGroup")
		  .append(" where section.key=:sectionKey");
		
		List<Binder> binders = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Binder.class)
			.setParameter("sectionKey", section.getKey())
			.getResultList();
		return binders == null || binders.isEmpty() ? null : binders.get(0);
	}
	
	public boolean isTemplateInUse(BinderRef template, RepositoryEntryRef entry, String subIdent) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder.key from pfbinder as binder")
		  .append(" where binder.template.key=:templateKey");
		if(entry != null) {
			sb.append(" and binder.entry.key=:entryKey");
		}
		if(StringHelper.containsNonWhitespace(subIdent)) {
			sb.append(" and binder.subIdent=:subIdent");
		}
		
		TypedQuery<Long> binderKeyQuery = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Long.class)
			.setParameter("templateKey", template.getKey());
		if(entry != null) {
			binderKeyQuery.setParameter("entryKey", entry.getKey());
		}
		if(StringHelper.containsNonWhitespace(subIdent)) {
			binderKeyQuery.setParameter("subIdent", subIdent);
		}
		
		List<Long> binderKeys = binderKeyQuery.setFirstResult(0)
			.setMaxResults(1)
			.getResultList();
		return binderKeys != null && binderKeys.size() > 0 && binderKeys.get(0) != null && binderKeys.get(0).longValue() >= 0;
	}
	
	public Binder getBinder(Identity owner, BinderRef template, RepositoryEntryRef entry, String subIdent) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder from pfbinder as binder")
		  .append(" inner join binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership on (membership.identity.key=:identityKey and membership.role='").append(PortfolioRoles.owner.name()).append("')")
		  .append(" where binder.template.key=:templateKey");
		if(entry != null) {
			sb.append(" and binder.entry.key=:entryKey");
		} else {
			sb.append(" and binder.entry.key is null");
		}
		
		if(StringHelper.containsNonWhitespace(subIdent)) {
			sb.append(" and binder.subIdent=:subIdent");
		} else {
			sb.append(" and binder.subIdent is null");
		}
		
		TypedQuery<Binder> binderKeyQuery = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Binder.class)
			.setParameter("templateKey", template.getKey())
			.setParameter("identityKey", owner.getKey());
		if(entry != null) {
			binderKeyQuery.setParameter("entryKey", entry.getKey());
		}
		if(StringHelper.containsNonWhitespace(subIdent)) {
			binderKeyQuery.setParameter("subIdent", subIdent);
		}
		
		List<Binder> binders = binderKeyQuery.setFirstResult(0)
			.setMaxResults(1)
			.getResultList();
		return binders != null && binders.size() > 0 && binders.get(0) != null ? binders.get(0) : null;
	}
	
	public List<Binder> getBinders(Identity owner, RepositoryEntryRef entry, String subIdent) {
		StringBuilder sb = new StringBuilder();
		sb.append("select binder from pfbinder as binder")
		  .append(" inner join binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership on (membership.identity.key=:identityKey and membership.role='").append(PortfolioRoles.owner.name()).append("')")
		  .append(" where  binder.entry.key=:entryKey and ");
		if(StringHelper.containsNonWhitespace(subIdent)) {
			sb.append("binder.subIdent=:subIdent");
		} else {
			sb.append("binder.subIdent is null");
		}

		TypedQuery<Binder> binders = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Binder.class)
			.setParameter("identityKey", owner.getKey())
			.setParameter("entryKey", entry.getKey());
		if(StringHelper.containsNonWhitespace(subIdent)) {
			binders.setParameter("subIdent", subIdent);
		}
		return binders.getResultList();
	}
	
	public Group getGroup(BinderRef binder) {
		StringBuilder sb = new StringBuilder();
		sb.append("select baseGroup from pfbinder as binder ")
		  .append(" inner join binder.baseGroup as baseGroup")
		  .append(" where binder.key=:binderKey");

		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Group.class)
				.setParameter("binderKey", binder.getKey())
				.getSingleResult();
	}
	
	public Group getGroup(SectionRef binder) {
		StringBuilder sb = new StringBuilder();
		sb.append("select baseGroup from pfsection as section ")
		  .append(" inner join section.baseGroup as baseGroup")
		  .append(" where section.key=:sectionKey");

		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Group.class)
				.setParameter("sectionKey", binder.getKey())
				.getSingleResult();
	}
	
	public boolean isMember(BinderRef binder, IdentityRef member, String... roles)  {
		if(binder == null || roles == null || roles.length == 0 || roles[0] == null) {
			return false;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("select count(membership.key) from pfbinder as binder")
		  .append(" inner join binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" inner join membership.identity as ident")
		  .append(" where binder.key=:binderKey and membership.identity.key=:identityKey and membership.role in (:roles)");
		
		List<String> roleList = new ArrayList<>(roles.length);
		for(String role:roles) {
			if(StringHelper.containsNonWhitespace(role)) {
				roleList.add(role);
			}
		}
		
		List<Number> counts = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Number.class)
			.setParameter("binderKey", binder.getKey())
			.setParameter("identityKey", member.getKey())
			.setParameter("roles", roleList)
			.getResultList();
		return counts == null || counts.isEmpty() || counts.get(0) == null ? false : counts.get(0).longValue() > 0;
	}
	
	public List<Identity> getMembers(BinderRef binder, String... roles)  {
		if(binder == null || roles == null || roles.length == 0 || roles[0] == null) {
			return Collections.emptyList();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("select membership.identity from pfbinder as binder")
		  .append(" inner join binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" inner join membership.identity as ident")
		  .append(" inner join fetch ident.user as identUser")
		  .append(" where binder.key=:binderKey and membership.role in (:roles)");
		
		List<String> roleList = new ArrayList<>(roles.length);
		for(String role:roles) {
			if(StringHelper.containsNonWhitespace(role)) {
				roleList.add(role);
			}
		}
		
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Identity.class)
			.setParameter("binderKey", binder.getKey())
			.setParameter("roles", roleList)
			.getResultList();
	}
	
	public List<AccessRights> getBinderAccesRights(BinderRef binder, IdentityRef identity)  {
		if(binder == null) {
			return Collections.emptyList();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("select membership.role, membership.identity from pfbinder as binder")
		  .append(" inner join binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership");
		if(identity != null) {
			sb.append(" on (membership.identity.key =:identityKey)");
		}
		sb.append(" inner join membership.identity as ident")
		  .append(" inner join fetch ident.user as identUser")
		  .append(" where binder.key=:binderKey");

		TypedQuery<Object[]> query = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Object[].class)
			.setParameter("binderKey", binder.getKey());
		if(identity != null) {
			query.setParameter("identityKey", identity.getKey());
		}
		
		List<Object[]> objects = query.getResultList();
		List<AccessRights> rightList = new ArrayList<>(objects.size());
		for(Object[] object:objects) {
			String role = (String)object[0];
			Identity member = (Identity)object[1];
			
			AccessRights rights = new AccessRights();
			rights.setRole(PortfolioRoles.valueOf(role));
			rights.setBinderKey(binder.getKey());
			rights.setIdentity(member);
			rightList.add(rights);
		}
		return rightList;
	}
	
	public List<AccessRights> getSectionAccesRights(BinderRef binder, IdentityRef identity)  {
		if(binder == null) {
			return Collections.emptyList();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("select section.key, membership.role, membership.identity from pfbinder as binder")
		  .append(" inner join binder.sections as section")
		  .append(" inner join section.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership");
		if(identity != null) {
			sb.append(" on (membership.identity.key =:identityKey)");
		}
		sb.append(" inner join membership.identity as ident")
		  .append(" inner join fetch ident.user as identUser")
		  .append(" where binder.key=:binderKey");

		TypedQuery<Object[]> query = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Object[].class)
			.setParameter("binderKey", binder.getKey());
		if(identity != null) {
			query.setParameter("identityKey", identity.getKey());
		}
		
		List<Object[]> objects = query.getResultList();
		List<AccessRights> rightList = new ArrayList<>(objects.size());
		for(Object[] object:objects) {
			Long sectionKey = (Long)object[0];
			String role = (String)object[1];
			Identity member = (Identity)object[2];
			
			AccessRights rights = new AccessRights();
			rights.setRole(PortfolioRoles.valueOf(role));
			rights.setBinderKey(binder.getKey());
			rights.setSectionKey(sectionKey);
			rights.setIdentity(member);
			rightList.add(rights);
		}
		return rightList;
	}
	
	public List<AccessRights> getPageAccesRights(BinderRef binder, IdentityRef identity)  {
		if(binder == null) {
			return Collections.emptyList();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("select section.key, page.key, membership.role, membership.identity from pfbinder as binder")
		  .append(" inner join binder.sections as section")
		  .append(" inner join section.pages as page")
		  .append(" inner join page.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership");
		if(identity != null) {
			sb.append(" on (membership.identity.key =:identityKey)");
		}
		sb.append(" inner join membership.identity as ident")
		  .append(" inner join fetch ident.user as identUser")
		  .append(" where binder.key=:binderKey");

		TypedQuery<Object[]> query = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Object[].class)
			.setParameter("binderKey", binder.getKey());
		if(identity != null) {
			query.setParameter("identityKey", identity.getKey());
		}
		
		List<Object[]> objects = query.getResultList();
		List<AccessRights> rightList = new ArrayList<>(objects.size());
		for(Object[] object:objects) {
			Long sectionKey = (Long)object[0];
			Long pageKey = (Long)object[1];
			String role = (String)object[2];
			Identity member = (Identity)object[3];
			
			AccessRights rights = new AccessRights();
			rights.setRole(PortfolioRoles.valueOf(role));
			rights.setBinderKey(binder.getKey());
			rights.setSectionKey(sectionKey);
			rights.setPageKey(pageKey);
			rights.setIdentity(member);
			rightList.add(rights);
		}
		return rightList;
	}

	
	/**
	 * Add a section to a binder. The binder must be a fresh reload one
	 * with the possibility to lazy load the sections.
	 * 
	 * @param title
	 * @param description
	 * @param begin
	 * @param end
	 * @param binder
	 */
	public SectionImpl createSection(String title, String description, Date begin, Date end, Binder binder) {
		SectionImpl section = new SectionImpl();
		section.setCreationDate(new Date());
		section.setLastModified(section.getCreationDate());
		
		section.setBaseGroup(groupDao.createGroup());
		section.setTitle(title);
		section.setDescription(description);
		section.setBeginDate(begin);
		section.setEndDate(end);
		section.setStatus(SectionStatus.notStarted.name());
		//force load of the list
		((BinderImpl)binder).getSections().size();
		section.setBinder(binder);
		((BinderImpl)binder).getSections().add(section);
		dbInstance.getCurrentEntityManager().persist(section);
		dbInstance.getCurrentEntityManager().merge(binder);
		return section;
	}
	
	public Section updateSection(Section section) {
		((SectionImpl)section).setLastModified(new Date());
		return dbInstance.getCurrentEntityManager().merge(section);
	}
	
	public Section loadSectionByKey(Long key) {
		StringBuilder sb = new StringBuilder();
		sb.append("select section from pfsection as section")
		  .append(" inner join fetch section.baseGroup as baseGroup")
		  .append(" where section.key=:sectionKey");
		
		List<Section> sections = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Section.class)
			.setParameter("sectionKey", key)
			.getResultList();
		return sections == null || sections.isEmpty() ? null : sections.get(0);
	}
	
	public List<Section> getSections(BinderRef binder) {
		StringBuilder sb = new StringBuilder();
		sb.append("select section from pfsection as section")
		  .append(" inner join fetch section.baseGroup as baseGroup")
		  .append(" where section.binder.key=:binderKey")
		  .append(" order by section.pos");
		
		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Section.class)
			.setParameter("binderKey", binder.getKey())
			.getResultList();
	}
}
