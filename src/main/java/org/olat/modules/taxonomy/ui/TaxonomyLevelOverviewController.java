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
package org.olat.modules.taxonomy.ui;

import org.olat.core.commons.persistence.DB;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.util.StringHelper;
import org.olat.modules.taxonomy.Taxonomy;
import org.olat.modules.taxonomy.TaxonomyLevel;
import org.olat.modules.taxonomy.TaxonomyLevelManagedFlag;
import org.olat.modules.taxonomy.TaxonomyService;
import org.olat.modules.taxonomy.ui.events.DeleteTaxonomyLevelEvent;
import org.olat.modules.taxonomy.ui.events.NewTaxonomyLevelEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 14 nov. 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class TaxonomyLevelOverviewController extends BasicController {
	
	private final VelocityContainer mainVC;
	private final SegmentViewComponent segmentView;
	private final Link metadataLink, competencesLink, relationsLink, actionButton;
	
	private CloseableModalController cmc;
	private ActionsController actionsCtrl;
	private DialogBoxController confirmDeleteDialog;
	private EditTaxonomyLevelController metadataCtrl;
	private TaxonomyLevelRelationsController relationsCtrl;
	private TaxonomyLevelCompetenceController competencesCtrl;
	private CloseableCalloutWindowController actionsCalloutCtrl;
	private EditTaxonomyLevelController createTaxonomyLevelCtrl;
	
	private TaxonomyLevel taxonomyLevel;
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private TaxonomyService taxonomyService;
	
	public TaxonomyLevelOverviewController(UserRequest ureq, WindowControl wControl, TaxonomyLevel taxonomyLevel) {
		super(ureq, wControl);
		
		this.taxonomyLevel = taxonomyLevel;

		mainVC = createVelocityContainer("taxonomy_level_overview");
		
		actionButton = LinkFactory.createButton("actions", mainVC, this);
		actionButton.setIconLeftCSS("o_icon o_icon_actions");
		
		segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);
		metadataLink = LinkFactory.createLink("taxonomy.metadata", mainVC, this);
		segmentView.addSegment(metadataLink, true);
		doOpenMetadata(ureq);
		
		competencesLink = LinkFactory.createLink("taxonomy.level.competences", mainVC, this);
		segmentView.addSegment(competencesLink, false);
		relationsLink = LinkFactory.createLink("taxonomy.level.relations", mainVC, this);
		segmentView.addSegment(relationsLink, false);

		putInitialPanel(mainVC);
		updateProperties();
	}
	
	private void updateProperties() {
		mainVC.contextPut("id", taxonomyLevel.getKey());
		mainVC.contextPut("externalId", taxonomyLevel.getExternalId());
		mainVC.contextPut("path", taxonomyLevel.getMaterializedPathIdentifiers());
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(actionButton == source) {
			doOpenActions(ureq);
		} else if(source == segmentView) {
			if(event instanceof SegmentViewEvent) {
				SegmentViewEvent sve = (SegmentViewEvent)event;
				String segmentCName = sve.getComponentName();
				Component clickedLink = mainVC.getComponent(segmentCName);
				if (clickedLink == metadataLink) {
					doOpenMetadata(ureq);
				} else if (clickedLink == competencesLink){
					doOpenCompetences(ureq);
				} else if (clickedLink == relationsLink){
					doOpenRelations(ureq);
				}
			}
		}
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(metadataCtrl == source) {
			if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				taxonomyLevel = metadataCtrl.getTaxonomyLevel();
				updateProperties();
				fireEvent(ureq, event);
			} else if(event == Event.CANCELLED_EVENT) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		} else if(confirmDeleteDialog == source) {
			if (DialogBoxUIFactory.isOkEvent(event) || DialogBoxUIFactory.isYesEvent(event)) {
				doDelete(ureq);
			}
		} else if(createTaxonomyLevelCtrl == source) {
			if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				fireEvent(ureq, new NewTaxonomyLevelEvent(createTaxonomyLevelCtrl.getTaxonomyLevel()));
			}
			cmc.deactivate();
			cleanUp();
		} else if(cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(createTaxonomyLevelCtrl);
		removeAsListenerAndDispose(actionsCalloutCtrl);
		removeAsListenerAndDispose(actionsCtrl);
		removeAsListenerAndDispose(cmc);
		createTaxonomyLevelCtrl = null;
		actionsCalloutCtrl = null;
		actionsCtrl = null;
		cmc = null;
	}
	
	private void doOpenActions(UserRequest ureq) {
		actionsCtrl = new ActionsController(ureq, getWindowControl());
		listenTo(actionsCtrl);
		actionsCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
				actionsCtrl.getInitialComponent(), actionButton.getDispatchID(), "", true, "");
		listenTo(actionsCalloutCtrl);
		actionsCalloutCtrl.activate();
	}
	
	private void doConfirmDelete(UserRequest ureq) {
		if(taxonomyService.canDeleteTaxonomyLevel(taxonomyLevel)) {
			String title = translate("confirmation.delete.level.title");
			String text = translate("confirmation.delete.level", new String[] { StringHelper.escapeHtml(taxonomyLevel.getDisplayName()) });
			confirmDeleteDialog = activateOkCancelDialog(ureq, title, text, confirmDeleteDialog);
		} else {
			showWarning("warning.delete.level");
		}
	}
	
	private void doDelete(UserRequest ureq) {
		if(taxonomyService.deleteTaxonomyLevel(taxonomyLevel)) {
			dbInstance.commit();//commit before sending event
			fireEvent(ureq, new DeleteTaxonomyLevelEvent());
			showInfo("confirm.deleted.level", new String[] { StringHelper.escapeHtml(taxonomyLevel.getDisplayName()) });
		}
	}
	
	private void doMove() {
		//TODO taxonomy
		showWarning("not.implemented");
	}
	
	private void doCreateTaxonomyLevel(UserRequest ureq) {
		if(createTaxonomyLevelCtrl != null) return;
		
		taxonomyLevel = taxonomyService.getTaxonomyLevel(taxonomyLevel);
		Taxonomy taxonomy = taxonomyLevel.getTaxonomy();
		createTaxonomyLevelCtrl = new EditTaxonomyLevelController(ureq, getWindowControl(), taxonomyLevel, taxonomy);
		listenTo(createTaxonomyLevelCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), "close", createTaxonomyLevelCtrl.getInitialComponent(), true, translate("add.taxonomy.level"));
		listenTo(cmc);
		cmc.activate();
	}

	private void doOpenMetadata(UserRequest ureq) {
		if(metadataCtrl == null) {
			metadataCtrl = new EditTaxonomyLevelController(ureq, getWindowControl(), taxonomyLevel);
			listenTo(metadataCtrl);
		}
		mainVC.put("segmentCmp", metadataCtrl.getInitialComponent());
	}
	
	private void doOpenCompetences(UserRequest ureq) {
		if(competencesCtrl == null) {
			competencesCtrl = new TaxonomyLevelCompetenceController(ureq, getWindowControl(), taxonomyLevel);
			listenTo(competencesCtrl);
		}
		mainVC.put("segmentCmp", competencesCtrl.getInitialComponent());
	}
	
	private void doOpenRelations(UserRequest ureq) {
		if(relationsCtrl == null) {
			relationsCtrl = new TaxonomyLevelRelationsController(ureq, getWindowControl());
			listenTo(relationsCtrl);
		}
		mainVC.put("segmentCmp", relationsCtrl.getInitialComponent());
	}
	
	private class ActionsController extends BasicController {

		private final VelocityContainer toolVC;
		private Link moveLink, newLink, deleteLink;

		public ActionsController(UserRequest ureq, WindowControl wControl) {
			super(ureq, wControl);
			
			toolVC = createVelocityContainer("level_actions");
			if(!TaxonomyLevelManagedFlag.isManaged(taxonomyLevel, TaxonomyLevelManagedFlag.move)) {
				moveLink = addLink("move.taxonomy.level", "o_icon_move");
			}
			newLink = addLink("add.taxonomy.level.under", "o_icon_taxonomy_levels");
			if(!TaxonomyLevelManagedFlag.isManaged(taxonomyLevel, TaxonomyLevelManagedFlag.delete)) {
				deleteLink = addLink("delete", "o_icon_delete_item");
			}
			putInitialPanel(toolVC);
		}
		
		private Link addLink(String name, String iconCss) {
			Link link = LinkFactory.createLink(name, name, getTranslator(), toolVC, this, Link.LINK);
			toolVC.put(name, link);
			link.setIconLeftCSS("o_icon " + iconCss);
			return link;
		}

		@Override
		protected void doDispose() {
			//
		}
		
		@Override
		protected void event(UserRequest ureq, Component source, Event event) {
			if(moveLink == source) {
				close();
				doMove();
			} else if(newLink == source) {
				close();
				doCreateTaxonomyLevel(ureq);
			} else if(deleteLink == source) {
				close();
				doConfirmDelete(ureq);
			}
		}
		
		private void close() {
			actionsCalloutCtrl.deactivate();
			cleanUp();
		}
	}
}