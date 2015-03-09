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
package org.olat.login.oauth.ui;

import org.olat.core.commons.fullWebApp.BaseFullWebappController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.login.DmzBFWCParts;
import org.olat.login.oauth.model.OAuthRegistration;

/**
 * 
 * Initial date: 04.11.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class OAuthRegistrationCreator implements ControllerCreator {
	
	@Override
	public Controller createController(UserRequest ureq, WindowControl wControl) {
		DmzBFWCParts parts = new DmzBFWCParts();
		parts.setContentControllerCreator(new InternalCreator());
		return new BaseFullWebappController(ureq, parts);
	}
	
	private static class InternalCreator implements ControllerCreator {
		@Override
		public Controller createController(UserRequest ureq, WindowControl wControl) {
			OAuthRegistration registration = (OAuthRegistration)ureq.getHttpReq()
					.getSession().getAttribute("oauthRegistration");
			return new OAuthRegistrationController(ureq, wControl, registration);
		}
	}
}
