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
package org.olat.ims.qti21.ui.assessment;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.fileresource.FileResourceManager;
import org.olat.ims.qti21.AssessmentItemSession;
import org.olat.ims.qti21.AssessmentSessionAuditLogger;
import org.olat.ims.qti21.AssessmentTestHelper;
import org.olat.ims.qti21.AssessmentTestSession;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.model.ParentPartItemRefs;
import org.olat.ims.qti21.model.xml.QtiNodesExtractor;
import org.olat.ims.qti21.ui.ResourcesMapper;
import org.olat.ims.qti21.ui.assessment.event.NextAssessmentItemEvent;
import org.olat.ims.qti21.ui.assessment.model.AssessmentItemCorrection;
import org.olat.ims.qti21.ui.assessment.model.AssessmentItemListEntry;
import org.olat.modules.assessment.Role;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ed.ph.jqtiplus.node.test.AssessmentTest;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentTest;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNodeKey;
import uk.ac.ed.ph.jqtiplus.state.TestSessionState;

/**
 * 
 * Initial date: 23 févr. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CorrectionIdentityAssessmentItemController extends FormBasicController {
	
	private FormLink saveNextQuestionButton;
	private FormLink saveBackOverviewButton;

	private final String mapperUri;
	private final URI assessmentObjectUri;
	private final ResourcesMapper resourcesMapper;
	
	private CorrectionOverviewModel model;
	private final RepositoryEntry testEntry;
	private AssessmentItemCorrection itemCorrection;
	private final AssessmentItemListEntry assessmentEntry;
	private final ResolvedAssessmentTest resolvedAssessmentTest;
	private final List<? extends AssessmentItemListEntry> assessmentEntryList;
	private Map<Long, File> submissionDirectoryMaps = new HashMap<>();
	
	private CorrectionIdentityInteractionsController identityInteractionsCtrl;

	@Autowired
	private QTI21Service qtiService;
	
	public CorrectionIdentityAssessmentItemController(UserRequest ureq, WindowControl wControl,
			RepositoryEntry testEntry, ResolvedAssessmentTest resolvedAssessmentTest,
			AssessmentItemCorrection itemCorrection, AssessmentItemListEntry assessmentEntry,
			List<? extends AssessmentItemListEntry> assessmentEntryList, CorrectionOverviewModel model) {
		super(ureq, wControl, "correction_identity_assessment_item");

		FileResourceManager frm = FileResourceManager.getInstance();
		File fUnzippedDirRoot = frm.unzipFileResource(testEntry.getOlatResource());
		assessmentObjectUri = qtiService.createAssessmentTestUri(fUnzippedDirRoot);
		
		this.model = model;
		this.testEntry = testEntry;
		this.itemCorrection = itemCorrection;
		this.assessmentEntry = assessmentEntry;
		this.assessmentEntryList = assessmentEntryList;
		this.resolvedAssessmentTest = resolvedAssessmentTest;
		
		resourcesMapper = new ResourcesMapper(assessmentObjectUri, submissionDirectoryMaps);
		mapperUri = registerCacheableMapper(null, "QTI21CorrectionsResources::" + testEntry.getKey(), resourcesMapper);
		
		initForm(ureq);	
	}
	
	public AssessmentItemListEntry getAssessmentItemSession() {
		return assessmentEntry;
	}
	
	public List<? extends AssessmentItemListEntry> getAssessmentEntryList() {
		return assessmentEntryList;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			layoutCont.contextPut("label", assessmentEntry.getLabel());
			layoutCont.contextPut("labelCssClass", assessmentEntry.getLabelCssClass());
			if(StringHelper.containsNonWhitespace(assessmentEntry.getTitle())) {
				layoutCont.contextPut("title", assessmentEntry.getTitle());
			}
			if(StringHelper.containsNonWhitespace(assessmentEntry.getTitleCssClass())) {
				layoutCont.contextPut("titleCssClass", assessmentEntry.getTitleCssClass());
			}
		}

		identityInteractionsCtrl = new CorrectionIdentityInteractionsController(ureq, getWindowControl(), 
				testEntry, resolvedAssessmentTest, itemCorrection, submissionDirectoryMaps, mapperUri,
				mainForm);
		listenTo(identityInteractionsCtrl);
		formLayout.add("interactions", identityInteractionsCtrl.getInitialFormItem());
		
		uifactory.addFormCancelButton("cancel", formLayout, ureq, getWindowControl());
		uifactory.addFormSubmitButton("save", formLayout);
		saveNextQuestionButton = uifactory.addFormLink("save.next", formLayout, Link.BUTTON);
		saveBackOverviewButton = uifactory.addFormLink("save.back", formLayout, Link.BUTTON);
	}
	
	protected void updateNext(boolean nextEnable) {
		saveNextQuestionButton.setVisible(nextEnable);
		saveBackOverviewButton.setVisible(!nextEnable);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		doSave();
		fireEvent(ureq, Event.CHANGED_EVENT);
		identityInteractionsCtrl.updateStatus();
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(saveNextQuestionButton == source) {
			doSave();
			fireEvent(ureq, Event.CHANGED_EVENT);
			fireEvent(ureq, new NextAssessmentItemEvent());
		} else if(saveBackOverviewButton == source) {
			doSave();
			fireEvent(ureq, Event.CHANGED_EVENT);
			fireEvent(ureq, Event.BACK_EVENT);
		} else {
			super.formInnerEvent(ureq, source, event);
		}
	}

	private void doSave() {
		TestSessionState testSessionState = itemCorrection.getTestSessionState();
		AssessmentTestSession candidateSession = itemCorrection.getTestSession();
		try(AssessmentSessionAuditLogger candidateAuditLogger = qtiService.getAssessmentSessionAuditLogger(candidateSession, false)) {
			TestPlanNodeKey testPlanNodeKey = itemCorrection.getItemNode().getKey();
			String stringuifiedIdentifier = testPlanNodeKey.getIdentifier().toString();
			
			ParentPartItemRefs parentParts = AssessmentTestHelper
					.getParentSection(testPlanNodeKey, testSessionState, resolvedAssessmentTest);
			AssessmentItemSession itemSession = qtiService
					.getOrCreateAssessmentItemSession(candidateSession, parentParts, stringuifiedIdentifier);

			itemSession.setManualScore(identityInteractionsCtrl.getManualScore());
			itemSession.setCoachComment(identityInteractionsCtrl.getComment());
			itemSession.setToReview(identityInteractionsCtrl.isToReview());
			
			itemSession = qtiService.updateAssessmentItemSession(itemSession);
			itemCorrection.setItemSession(itemSession);
			
			candidateAuditLogger.logCorrection(candidateSession, itemSession, getIdentity());
			
			candidateSession = qtiService.recalculateAssessmentTestSessionScores(candidateSession.getKey());
			itemCorrection.setTestSession(candidateSession);
			model.updateLastSession(itemCorrection.getAssessedIdentity(), candidateSession);
			
			if(model.getCourseNode() != null && model.getCourseEnvironment() != null) {
				doUpdateCourseNode(candidateSession, model.getCourseNode(), model.getCourseEnvironment());
			}
		} catch(IOException e) {
			logError("", e);
		}
	}
	
	private void doUpdateCourseNode(AssessmentTestSession testSession, IQTESTCourseNode courseNode, CourseEnvironment courseEnv) {
		if(testSession == null) return;
		
		AssessmentTest assessmentTest = model.getResolvedAssessmentTest().getRootNodeLookup().extractIfSuccessful();
		Double cutValue = QtiNodesExtractor.extractCutValue(assessmentTest);

		UserCourseEnvironment assessedUserCourseEnv = AssessmentHelper
				.createAndInitUserCourseEnvironment(testSession.getIdentity(), courseEnv);
		ScoreEvaluation scoreEval = courseNode.getUserScoreEvaluation(assessedUserCourseEnv);
		
		BigDecimal finalScore = testSession.getFinalScore();
		Float score = finalScore == null ? null : finalScore.floatValue();
		Boolean passed = scoreEval.getPassed();
		if(testSession.getManualScore() != null && finalScore != null && cutValue != null) {
			boolean calculated = finalScore.compareTo(BigDecimal.valueOf(cutValue.doubleValue())) >= 0;
			passed = Boolean.valueOf(calculated);
		}
		
		ScoreEvaluation manualScoreEval = new ScoreEvaluation(score, passed,
				scoreEval.getAssessmentStatus(), scoreEval.getUserVisible(), scoreEval.getFullyAssessed(),
				scoreEval.getCurrentRunCompletion(), scoreEval.getCurrentRunStatus(), testSession.getKey());
		courseNode.updateUserScoreEvaluation(manualScoreEval, assessedUserCourseEnv, getIdentity(), false, Role.coach);
	}
}
