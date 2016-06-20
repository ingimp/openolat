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
package org.olat.modules.portfolio;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.IdentityRef;
import org.olat.core.id.Identity;
import org.olat.modules.portfolio.model.AccessRightChange;
import org.olat.modules.portfolio.model.AccessRights;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.resource.OLATResource;

/**
 * 
 * Initial date: 06.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public interface PortfolioService {
	
	public Binder createNewBinder(String title, String summary, String imagePath, Identity owner);
	
	public OLATResource createBinderTemplateResource();
	
	public void createAndPersistBinderTemplate(Identity owner, RepositoryEntry entry, Locale locale);
	
	public Binder updateBinder(Binder binder);
	
	/**
	 * Add a new section at the end of the sections list of the specified binder.
	 * 
	 * @param title
	 * @param description
	 * @param begin
	 * @param end
	 * @param binder
	 */
	public void appendNewSection(String title, String description, Date begin, Date end, BinderRef binder);
	
	public Section updateSection(Section section);
	
	public List<Binder> searchOwnedBinders(Identity owner);
	
	/**
	 * Search all binders which are shared with the specified member.
	 * 
	 * @param member
	 * @return
	 */
	public List<Binder> searchSharedBindersWith(Identity member);
	
	/**
	 * Search all the binders which the specified identity shared
	 * with some people.
	 * 
	 * @param identity
	 * @return
	 */
	public List<Binder> searchSharedBindersBy(Identity owner);
	
	public Binder getBinderByKey(Long portfolioKey);
	
	public Binder getBinderByResource(OLATResource resource);
	
	public Binder getBinderBySection(SectionRef section);
	
	/**
	 * It will search against all parameters. If course and subIdent are null, it will
	 * search binders with these values set to null.
	 * 
	 * @param owner
	 * @param templateBinder
	 * @param courseEntry
	 * @param subIdent
	 * @return
	 */
	public Binder getBinder(Identity owner, BinderRef templateBinder, RepositoryEntryRef courseEntry, String subIdent);
	
	public List<Binder> getBinders(Identity owner, RepositoryEntryRef courseEntry, String subIdent);
	
	public boolean isTemplateInUse(Binder binder, RepositoryEntry courseEntry, String subIdent);
	
	public Binder assignBinder(Identity owner, BinderRef templateBinder, RepositoryEntry courseEntry, String subIdent, Date deadline);
	
	/**
	 * The list of owners of the binder.
	 * @param binder
	 * @param roles At least a role need to be specified
	 * @return
	 */
	public List<Identity> getMembers(BinderRef binder, String... roles);
	
	public List<AccessRights> getAccessRights(Binder binder);
	
	public List<AccessRights> getAccessRights(Binder binder, Identity identity);
	
	public void addAccessRights(PortfolioElement element, Identity identity, PortfolioRoles role);
	
	public void changeAccessRights(List<Identity> identities, List<AccessRightChange> changes);
	
	public List<Category> getCategories(Binder binder);

	public void updateCategories(Binder binder, List<String> categories);
	
	public File getPosterImage(Binder binder);
	
	public String addPosterImageForBinder(File file, String filename);
	
	public void removePosterImage(Binder binder);
	
	/**
	 * Load the sections
	 */
	public List<Section> getSections(BinderRef binder);
	
	/**
	 * Reload the section to have up to date values
	 * @param section
	 * @return
	 */
	public Section getSection(SectionRef section);
	
	/**
	 * Load the pages and the sections order by sections and pages.
	 * 
	 * @param binder
	 * @return the list of pages of the specified binder.
	 */
	public List<Page> getPages(BinderRef binder);
	
	/**
	 * 
	 * @param section
	 * @return
	 */
	public List<Page> getPages(SectionRef section);
	
	public List<Page> searchOwnedPages(IdentityRef owner);
	
	/**
	 * 
	 * @param title
	 * @param summary
	 * @param section
	 */
	public Page appendNewPage(Identity owner, String title, String summary, String imagePath, SectionRef section);
	
	public Page getPageByKey(Long key);
	
	public Page updatePage(Page page);
	

	public File getPosterImage(Page page);

	public String addPosterImageForPage(File file, String filename);
	
	public void removePosterImage(Page page);
	
	/**
	 * 
	 * @param title
	 * @param summary
	 * @param page
	 * @return
	 */
	public <U extends PagePart> U appendNewPagePart(Page page, U part);
	
	/**
	 * The list of page fragments
	 * @param page
	 * @return
	 */
	public List<PagePart> getPageParts(Page page);
	
	/**
	 * Merge the page part
	 * @param part
	 * @return
	 */
	public PagePart updatePart(PagePart part);

}