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
package org.olat.user.propertyhandlers;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.id.User;
import org.olat.core.util.StringHelper;

/**
 * <h3>Description:</h3>
 * The phne property provides a user property that contains a valid phone number. 
 * <p>
 * Initial Date: 27.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */
public class PhonePropertyHandler extends Generic127CharTextPropertyHandler {
	
	// Regexp to define valid phone numbers
	private static final Pattern VALID_PHONE_PATTERN_IP = Pattern.compile( "[0-9/\\-+' ]+" );
	
	/* (non-Javadoc)
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#addFormItem(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean, org.olat.core.gui.components.form.flexible.FormItemContainer)
	 */
	@Override
	public FormItem addFormItem(Locale locale, User user, String usageIdentifyer, boolean isAdministrativeUser,
			FormItemContainer formItemContainer) {
		org.olat.core.gui.components.form.flexible.elements.TextElement textElement = (org.olat.core.gui.components.form.flexible.elements.TextElement)super.addFormItem(locale, user, usageIdentifyer, isAdministrativeUser, formItemContainer);
		textElement.setExampleKey("form.example.phone", null);
		return textElement;
	}

	/**
	 * @see org.olat.user.AbstractUserPropertyHandler#getUserPropertyAsHTML(org.olat.core.id.User, java.util.Locale)
	 */
	@Override
	public String getUserPropertyAsHTML(User user, Locale locale) {
		String phonenr = getUserProperty(user, locale);
		if (StringHelper.containsNonWhitespace(phonenr)) {
			StringBuffer sb = new StringBuffer();
			sb.append("<a href=\"callto:");
			sb.append(phonenr);
			sb.append("\" class=\"b_link_call\">");
			sb.append(phonenr);
			sb.append("</a>");
			return sb.toString();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#isValid(org.olat.core.gui.components.form.flexible.FormItem, java.util.Map)
	 */
	@Override
	public boolean isValid(FormItem formItem, Map<String,String> formContext) {
		// check parent rules first: check if mandatory and empty
		if (! super.isValid(formItem, formContext)) {
			return false;
		} 
		
		org.olat.core.gui.components.form.flexible.elements.TextElement textElement = (org.olat.core.gui.components.form.flexible.elements.TextElement) formItem;
		String value = textElement.getValue();
		
		if (StringHelper.containsNonWhitespace(value)) {
			// check phone address syntax
			if (!VALID_PHONE_PATTERN_IP.matcher(value).matches()) {
				formItem.setErrorKey(i18nFormElementLabelKey() + ".error.valid", null);
				return false;
			}
		}
		// everthing ok
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.olat.user.propertyhandlers.Generic127CharTextPropertyHandler#isValidValue(java.lang.String, org.olat.core.gui.components.form.ValidationError, java.util.Locale)
	 */
	@Override
	public boolean isValidValue(String value, ValidationError validationError, Locale locale) {
		if ( ! super.isValidValue(value, validationError, locale)) return false;
		
		if (StringHelper.containsNonWhitespace(value)) {			
			// check phone address syntax
			if ( ! VALID_PHONE_PATTERN_IP.matcher(value).matches()) {
				validationError.setErrorKey(i18nFormElementLabelKey()+ ".error.valid");
				return false;
			}
		}
		return true;
	}
	
}


