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
package org.olat.commons.calendar.ui.events;

import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;

/**
 * 
 * Initial date: 09.04.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CalendarGUIResizeEvent extends FormEvent {

	private static final long serialVersionUID = -5910033103406371704L;
	public static final String CMD_RESIZE = "resizecalevent";
	
	private Boolean allDay;
	private Long minuteDelta;
	private KalendarEvent event;
	private KalendarRenderWrapper calendarWrapper;
	
	public CalendarGUIResizeEvent(FormItem item, KalendarEvent event, KalendarRenderWrapper calendarWrapper,
			Long minuteDelta, Boolean allDay) {
		super(CMD_RESIZE, item);
		this.allDay = allDay;
		this.minuteDelta = minuteDelta;
		this.event = event;
		this.calendarWrapper = calendarWrapper;
	}

	public Boolean getAllDay() {
		return allDay;
	}

	public Long getMinuteDelta() {
		return minuteDelta;
	}

	public KalendarEvent getKalendarEvent() {
		return event;
	}

	public KalendarRenderWrapper getKalendarRenderWrapper() {
		return calendarWrapper;
	}
}
