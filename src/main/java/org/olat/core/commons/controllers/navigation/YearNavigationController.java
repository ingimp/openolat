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
package org.olat.core.commons.controllers.navigation;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.StackedPanel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.Translator;

/**
 * Displays a navigation tool for Dated objects sorted by month. Like this:
 * 
 * <pre>
 * &lt;&lt; 2009 &gt;&gt;
 * August (3)
 * July (12)
 * June (29)
 * ...
 * </pre>
 * 
 * Fires: NavigationEvent
 * <P>
 * Initial Date: Aug 12, 2009 <br>
 * 
 * @author gwassmann
 */
public class YearNavigationController extends BasicController {

	private YearNavigationModel model;
	private Link next, previous, yearLink;
	private VelocityContainer mainVC;
	private StackedPanel mainPanel;
	private List<Link> monthLinks;
	private List<? extends Dated> allObjects;
	private boolean showAll = true;

	/**
	 * Constructor based on a list of <code>Dated</code> objects.
	 * 
	 * @param ureq
	 * @param control
	 * @param fallBackTranslator
	 * @param datedObjects
	 */
	public YearNavigationController(UserRequest ureq, WindowControl control, Translator fallBackTranslator, List<? extends Dated> datedObjects) {
		super(ureq, control, fallBackTranslator);
		setDatedObjects(datedObjects);
	}

	/**
	 * Creates the year and month links to be displayed
	 */
	private void createLinks() {
		Year year = model.getCurrentYear();
		if (year != null) {
			yearLink = LinkFactory.createLink("yearLink", mainVC, this);
			yearLink.setCustomEnabledLinkCSS("o_year");
			yearLink.setCustomDisplayText(year.getName());
			yearLink.setUserObject(year);
			mainVC.contextPut("year", year);
			// Reestablish month links
			monthLinks = new ArrayList<Link>();
			for (Month month : year.getMonths()) {
				Link monthLink = LinkFactory.createLink("month_" + month.getName(), mainVC, this);
				monthLink.setCustomEnabledLinkCSS("o_month");
				monthLink.setCustomDisplayText(model.getMonthName(month));
				monthLink.setUserObject(month);
				monthLinks.add(monthLink);
			}
			// enable/disable the navigation links
			next.setEnabled(model.hasNext());
			previous.setEnabled(model.hasPrevious());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	protected void doDispose() {
	// nothing so far
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component,
	 *      org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == next) {
			model.next();
			createLinks();
			Year year = (Year) yearLink.getUserObject();
			Event navEvent = new NavigationEvent(year.getItems());
			fireEvent(ureq, navEvent);
			yearLink.setCustomEnabledLinkCSS("o_year o_selected");

		} else if (source == previous) {
			model.previous();
			createLinks();
			Year year = (Year) yearLink.getUserObject();
			Event navEvent = new NavigationEvent(year.getItems());
			fireEvent(ureq, navEvent);
			yearLink.setCustomEnabledLinkCSS("o_year o_selected");

		} else if (source == yearLink) {
			// Click on year toggles between year filter and show all filter
			if (showAll) {
				Year year = (Year) yearLink.getUserObject();
				Event navEvent = new NavigationEvent(year.getItems());
				fireEvent(ureq, navEvent);
				// update GUI
				yearLink.setCustomEnabledLinkCSS("o_year o_selected");
				for (Link monthLink : monthLinks) {
					monthLink.setCustomEnabledLinkCSS("o_month");
				}
				showAll = false;				
			} else {
				Event navEvent = new NavigationEvent(allObjects);
				fireEvent(ureq, navEvent);
				// update GUI
				yearLink.setCustomEnabledLinkCSS("o_year");
				showAll = true;
			}

		} else if (monthLinks.contains(source)) {
			Link monthLink = (Link) source;
			Month month = (Month) monthLink.getUserObject();
			Event navEvent = new NavigationEvent(month.getItems());
			fireEvent(ureq, navEvent);
			// update GUI
			yearLink.setCustomEnabledLinkCSS("o_year");
			for (Link link : monthLinks) {
				link.setCustomEnabledLinkCSS("o_month");
			}
			monthLink.setCustomEnabledLinkCSS("o_month o_selected");
		}
	}

	/**
	 * Method to re-initialize the year navigation with other dated objects. The
	 * model, links etc are all discarded.
	 * 
	 * @param datedObjects the new objects for the navigation
	 */
	public void setDatedObjects(List<? extends Dated> datedObjects) {
		// Create new main view, next and previous navigation even when it
		// exists. This method is called from the constructor but also when the
		// datamodel changes. It is bet to redo everything to not have stale
		// links in the velcity page.
		mainVC = createVelocityContainer("yearnavigation");
		next = LinkFactory.createCustomLink("navi.forward", "navi.forward", null, Link.NONTRANSLATED, mainVC, this);
		next.setIconLeftCSS("o_icon o_icon_next_page");
		next.setTooltip(translate("navi.forward"));
		//
		previous = LinkFactory.createCustomLink("navi.backward", "navi.backward", null, Link.NONTRANSLATED, mainVC, this);
		previous.setIconLeftCSS("o_icon o_icon_previous_page");
		previous.setTooltip(translate("navi.backward"));
		//
		if (mainPanel == null) {
			// first time
			mainPanel = this.putInitialPanel(mainVC);
		} else {
			// updating existing view
			mainPanel.setContent(mainVC);
		}
		// Create new model model
		model = new YearNavigationModel(datedObjects, getLocale());
		allObjects = datedObjects;
		showAll = true;
		//
		createLinks();
	}

	/**
	 * Adds the item to the model
	 * 
	 * @param item
	 */
	public void add(Dated item) {
		model.add(item);
		createLinks();
	}

	/**
	 * Removes the item from the model
	 * 
	 * @param item
	 */
	public void remove(Dated item) {
		model.remove(item);
		mainVC.setDirty(true);
	}

}
