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
package org.olat.ims.qti21.ui.editor;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.Util;
import org.olat.ims.qti21.QTI21Constants;
import org.olat.ims.qti21.model.xml.AssessmentTestBuilder;
import org.olat.ims.qti21.ui.AssessmentTestDisplayController;
import org.olat.ims.qti21.ui.editor.events.AssessmentTestEvent;

import uk.ac.ed.ph.jqtiplus.node.test.AssessmentTest;

/**
 * 
 * Initial date: 22.05.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentTestEditorController extends BasicController {

	private final TabbedPane tabbedPane;
	private final VelocityContainer mainVC;
	
	private AssessmentTestOptionsEditorController optionsCtrl;
	private AssessmentTestFeedbackEditorController feedbackCtrl;
	
	private final AssessmentTest assessmentTest;
	private final AssessmentTestBuilder testBuilder;
	
	public AssessmentTestEditorController(UserRequest ureq, WindowControl wControl,
			AssessmentTest assessmentTest) {
		super(ureq, wControl, Util.createPackageTranslator(AssessmentTestDisplayController.class, ureq.getLocale()));
		this.assessmentTest = assessmentTest;
		testBuilder = new AssessmentTestBuilder(assessmentTest);
		
		mainVC = createVelocityContainer("assessment_test_editor");
		tabbedPane = new TabbedPane("testTabs", getLocale());
		tabbedPane.addListener(this);
		mainVC.put("tabbedpane", tabbedPane);
		
		initTestEditor(ureq);
		putInitialPanel(mainVC);
	}
	
	@Override
	protected void doDispose() {
		//
	}
	
	private void initTestEditor(UserRequest ureq) {
		if(QTI21Constants.TOOLNAME.equals(assessmentTest.getToolName())) {
			optionsCtrl = new AssessmentTestOptionsEditorController(ureq, getWindowControl(), assessmentTest, testBuilder);
			listenTo(optionsCtrl);
			feedbackCtrl = new AssessmentTestFeedbackEditorController(ureq, getWindowControl(), testBuilder);
			listenTo(feedbackCtrl);
			
			tabbedPane.addTab(translate("assessment.test.config"), optionsCtrl.getInitialComponent());
			tabbedPane.addTab(translate("form.feedback"), feedbackCtrl.getInitialComponent());
		} else {
			Controller ctrl = new UnkownTestEditorController(ureq, getWindowControl());
			listenTo(ctrl);
			tabbedPane.addTab("Unkown", ctrl.getInitialComponent());
		}
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(optionsCtrl == source || feedbackCtrl == source) {
			if(AssessmentTestEvent.ASSESSMENT_TEST_CHANGED_EVENT.equals(event)) {
				testBuilder.build();
				fireEvent(ureq, event);
			}
		}
	}
}
