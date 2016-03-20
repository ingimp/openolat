/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.commons.calendar;

import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.olat.commons.calendar.model.CalendarFileInfos;
import org.olat.commons.calendar.model.CalendarUserConfiguration;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.i18n.I18nManager;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.ResourceLoader;
import net.fortuna.ical4j.util.Strings;


/**
 * Description:<BR>
 * Servlet that serves the ical document.
 * <P>
 * Initial Date:  June 1, 2008
 *
 * @author Udit Sajjanhar
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class ICalServlet extends HttpServlet {

	private static final long serialVersionUID = -155266285395912535L;
	private static final OLog log = Tracing.createLoggerFor(ICalServlet.class);
	
	private static final int TTL_HOURS = 6;
	private static final int cacheAge = 60 * 60 * TTL_HOURS;//6 Hours
	private static final ConcurrentMap<String,VTimeZone> outlookVTimeZones = new ConcurrentHashMap<>();
	
	/** collection of iCal feed prefixs **/
	public static final String[] SUPPORTED_PREFIX = {
			CalendarManager.ICAL_PREFIX_AGGREGATED,
			CalendarManager.ICAL_PREFIX_PERSONAL,
			CalendarManager.ICAL_PREFIX_COURSE,
			CalendarManager.ICAL_PREFIX_GROUP
	};

	/**
	 * Default constructor.
	 */
	public ICalServlet() {
		//
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Tracing.setUreq(req);
		try {
			super.service(req, resp);
		} finally {
			//consume the userrequest.
			Tracing.setUreq(null);
			I18nManager.remove18nInfoFromThread();
			DBFactory.getInstance().commitAndCloseSession();
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException {
		String requestUrl = request.getPathInfo();
		try {
			if (log.isDebug()) {
				log.debug("doGet pathInfo=" + requestUrl);
			}
			if ((requestUrl == null) || (requestUrl.equals(""))) {
				return; // error
			}

			getIcalDocument(requestUrl, request, response);
		} catch (ValidationException e) {
			log.warn("Validation Error when generate iCal stream for path::" + request.getPathInfo(), e);
			response.sendError(HttpServletResponse.SC_CONFLICT, requestUrl);
		} catch (IOException e) {
			log.warn("IOException Error when generate iCal stream for path::" + request.getPathInfo(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, requestUrl);
		} catch (Exception e) {
			log.warn("Unknown Error in icalservlet", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, requestUrl);
		}
	}
  
	/**
	 * Reads in the appropriate ics file, depending upon the pathInfo:<br>
	 * <ul>
	 * 	<li>/aggregated/<config key>/AUTH_TOKEN.ics</li>
	 *  <li>/user/<user_name>/AUTH_TOKEN.ics</li>
	 *  <li>/group/<user_name>/AUTH_TOKEN/<group_id>.ics</li>
	 *  <li>/course/<user_name>/AUTH_TOKEN/<course_unique_id>.ics</li>
	 * </ul>
	 * 
	 * @param pathInfo
	 * @return Calendar
	 */
	private void getIcalDocument(String requestUrl, HttpServletRequest request, HttpServletResponse response)
	throws ValidationException, IOException {
		// get the individual path tokens
		String pathInfo;
		int icsIndex = requestUrl.indexOf(".ics");
		if(icsIndex > 0) {
			pathInfo = requestUrl.substring(0, icsIndex);
		} else {
			pathInfo = requestUrl;
		}
		
		String[] pathInfoTokens = pathInfo.split("/");
		if(pathInfoTokens.length < 4) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, requestUrl);
			return;
		}
		
		String calendarType = pathInfoTokens[1];
		String userName = pathInfoTokens[2];
		String authToken = pathInfoTokens[3];

		String calendarID;
		if(CalendarManager.TYPE_COURSE.equals(calendarType) || CalendarManager.TYPE_GROUP.equals(calendarType)) {
			if(pathInfoTokens.length < 5) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, requestUrl);
				return;
			}
			calendarID = pathInfoTokens[4];
		} else if(CalendarManager.TYPE_USER.equals(calendarType)) {
			if(pathInfoTokens.length < 5) {
				calendarID = userName;
			} else {
				calendarID = pathInfoTokens[4];
			}
		} else if(CalendarManager.TYPE_USER_AGGREGATED.equals(calendarType)) {
			calendarID = userName;
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, requestUrl);
			log.warn("Type not supported: " + pathInfo);
			return;
		}
		
		try {
			response.setCharacterEncoding("UTF-8");
			setCacheControl(response);
		} catch (Exception e) {
			e.printStackTrace();
		}

		CalendarManager calendarManager = CoreSpringFactory.getImpl(CalendarManager.class);
		if(CalendarManager.TYPE_USER_AGGREGATED.equals(calendarType)) {
			// check the authentication token
			CalendarUserConfiguration config = calendarManager.getCalendarUserConfiguration(Long.parseLong(userName));
			String savedToken = config.getToken();
			if (authToken == null || savedToken == null || !savedToken.equals(authToken)) {
				log.warn("Authenticity Check failed for the ical feed path: " + pathInfo);
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, requestUrl);
			} else {
				generateAggregatedCalendar(config.getIdentity(), request, response);
			}
		} else if (calendarManager.calendarExists(calendarType, calendarID)) {
			// check the authentication token
			String savedToken = calendarManager.getCalendarToken(calendarType, calendarID, userName);
			if (authToken == null || savedToken == null || !savedToken.equals(authToken)) {
				log.warn("Authenticity Check failed for the ical feed path: " + pathInfo);
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, requestUrl);
			} else {
				// read and return the calendar file
				Calendar calendar = calendarManager.readCalendar(calendarType, calendarID);
				DBFactory.getInstance().commitAndCloseSession();
				outputCalendar(calendar, request, response);
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, requestUrl);
		}
	}
	
	private void setCacheControl(HttpServletResponse httpResponse) {
		long expiry = new Date().getTime() + cacheAge * 1000;
	    httpResponse.setDateHeader("Expires", expiry);
	    httpResponse.setHeader("Cache-Control", "max-age="+ cacheAge);
	}
	
	private void outputCalendar(Calendar calendar, HttpServletRequest request, HttpServletResponse response)
	throws ValidationException, IOException {
		boolean outlook = isOutlook(request);
		updateUrlProperties(calendar);
		
		Writer out = response.getWriter();
		out.write(Calendar.BEGIN);
		out.write(':');
		out.write(Calendar.VCALENDAR);
		out.write(Strings.LINE_SEPARATOR);
		out.write(Version.VERSION_2_0.toString());
		
		boolean calScale = false;
		for (Iterator<?> propIter = calendar.getProperties().iterator(); propIter.hasNext();) {
			Object pobject = propIter.next();
			if(pobject instanceof Property) {
				Property property = (Property)pobject;
				if(Version.VERSION.equals(property.getName())) {
					//we force version 2.0
				} else if(Version.CALSCALE.equals(property.getName())) {
					out.write(property.toString());
					calScale = true;
				} else {
					out.write(property.toString());
				}
			}
		}
		
		if(!calScale) {
			out.write(CalScale.GREGORIAN.toString());
		}

		outputTTL(out);

		Set<String> timezoneIds = new HashSet<>();
		outputCalendarComponents(calendar, out, outlook, timezoneIds);
		if(outlook) {
			outputTimeZoneForOutlook(timezoneIds, out);
		}
		
		out.write(Calendar.END);
		out.write(':');
		out.write(Calendar.VCALENDAR);
	}
	
	/**
	 * Collect all the calendars, update the URL properties and the UUID.
	 * 
	 * @param identity
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void generateAggregatedCalendar(Identity identity, HttpServletRequest request, HttpServletResponse response) throws IOException {
		PersonalCalendarManager homeCalendarManager = CoreSpringFactory.getImpl(PersonalCalendarManager.class);
		if(identity == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else {
			List<CalendarFileInfos> iCalFiles = homeCalendarManager.getListOfCalendarsFiles(identity);
			DBFactory.getInstance().commitAndCloseSession();
			boolean outlook = isOutlook(request);
			
			Writer out = response.getWriter();
			out.write(Calendar.BEGIN);
			out.write(':');
			out.write(Calendar.VCALENDAR);
			out.write(Strings.LINE_SEPARATOR);
			out.write(Version.VERSION_2_0.toString());
			out.write(CalScale.GREGORIAN.toString());
			
			outputTTL(out);

			Set<String> timezoneIds = new HashSet<>();
			int numOfFiles = iCalFiles.size();
			for(int i=0; i<numOfFiles; i++) {
				outputCalendar(iCalFiles.get(i), out, outlook, timezoneIds);
			}
			if(outlook) {
				outputTimeZoneForOutlook(timezoneIds, out);
			}
			
			out.write(Calendar.END);
			out.write(':');
			out.write(Calendar.VCALENDAR);
		}
	}
	
	private boolean isOutlook(HttpServletRequest request) {
		String userAgent = request.getHeader("User-Agent");
		if(userAgent != null && userAgent.indexOf("Microsoft Outlook") >= 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Append TTL:<br>
	 * @see http://stackoverflow.com/questions/17152251/specifying-name-description-and-refresh-interval-in-ical-ics-format
	 * @see http://tools.ietf.org/html/draft-daboo-icalendar-extensions-06
	 * 
	 * @param out
	 * @throws IOException
	 */
	private void outputTTL(Writer out)
	throws IOException {
		out.write("X-PUBLISHED-TTL:PT" + TTL_HOURS + "H");
		out.write(Strings.LINE_SEPARATOR);
		out.write("REFRESH-INTERVAL;VALUE=DURATION:PT" + TTL_HOURS + "H");
		out.write(Strings.LINE_SEPARATOR);
	}
	
	private void outputTimeZoneForOutlook(Set<String> timezoneIds,  Writer out) {
		for(String timezoneId:timezoneIds) {
			if(StringHelper.containsNonWhitespace(timezoneId)) {
				try {
					VTimeZone vTimeZone = getOutlookVTimeZone(timezoneId);
					if(vTimeZone != null) {
						out.write(vTimeZone.toString());
					}
				} catch (IOException | ParserException e) {
					log.error("", e);
				}
			}
		}
	}
	
	private void outputCalendar(CalendarFileInfos fileInfos, Writer out, boolean outlook, Set<String> timezoneIds)
	throws IOException {
		try {
			CalendarManager calendarManager = CoreSpringFactory.getImpl(CalendarManager.class);
			Calendar calendar = calendarManager.readCalendar(fileInfos.getCalendarFile());
			updateUrlProperties(calendar);
			
			String prefix = fileInfos.getType() + "-" + fileInfos.getCalendarId() + "-";
			updateUUID(calendar, prefix);
			
			outputCalendarComponents(calendar, out, outlook, timezoneIds);
		} catch (IOException | OLATRuntimeException e) {
			log.error("", e);
		}
	}
	
	private void outputCalendarComponents(Calendar calendar, Writer out, boolean outlook, Set<String> timezoneIds)
	throws IOException {
		try {
			ComponentList events = calendar.getComponents();
			for (final Iterator<?> i = events.iterator(); i.hasNext();) {
				Object comp = i.next();
				String event = comp.toString();
				if (outlook && comp instanceof VEvent) {
					event = quoteTimeZone(event, (VEvent)comp, timezoneIds);
				}
				out.write(event);
			}
		} catch (IOException | OLATRuntimeException e) {
			log.error("", e);
		}
	}
	
	private String quoteTimeZone(String event, VEvent vEvent, Set<String> timezoneIds) {
		if(vEvent == null || vEvent.getStartDate().getTimeZone() == null
				|| vEvent.getStartDate().getTimeZone().getVTimeZone() == null) {
			return event;
		}

		String timezoneId = vEvent.getStartDate().getTimeZone().getID();
		timezoneIds.add(timezoneId);
		TzId tzId = (TzId)vEvent.getStartDate().getParameter(Parameter.TZID);
		String tzidReplacement = "TZID=\"" + timezoneId + "\"";	
		return event.replace(tzId.toString(), tzidReplacement);
	}
	
	private void updateUUID(Calendar calendar, String prefix) {
		for (Iterator<?> eventIter = calendar.getComponents().iterator(); eventIter.hasNext();) {
			Object comp = eventIter.next();
			if (comp instanceof VEvent) {
				VEvent event = (VEvent)comp;
				Uid uid = event.getUid();
				String newUid = prefix.concat(uid.getValue());
				uid.setValue(newUid);
			}
		}
	}
	
	private void updateUrlProperties(Calendar calendar) {
		for (Iterator<?> eventIter = calendar.getComponents().iterator(); eventIter.hasNext();) {
			Object comp = eventIter.next();
			if (comp instanceof VEvent) {
				VEvent event = (VEvent)comp;
				
				PropertyList ooLinkProperties = event.getProperties(CalendarManager.ICAL_X_OLAT_LINK);
				if(ooLinkProperties.isEmpty()) {
					continue;
				}
				
				Url currentUrl = event.getUrl();
				if(currentUrl != null) {
					continue;
				}
				
				for (Iterator<?> iter = ooLinkProperties.iterator(); iter.hasNext();) {
					XProperty linkProperty = (XProperty) iter.next();
					if (linkProperty != null) {
						String encodedLink = linkProperty.getValue();
						StringTokenizer st = new StringTokenizer(encodedLink, "§", false);
						if (st.countTokens() >= 4) {
							st.nextToken();//provider
							st.nextToken();//id
							st.nextToken();//displayname
							
							String uri = st.nextToken();
							try {
								Url urlProperty = new Url();
								urlProperty.setValue(uri);
								event.getProperties().add(urlProperty);
								break;
							} catch (URISyntaxException e) {
								log.error("Invalid URL:" + uri);
							}
						}
					}
				}
			}
		}
	}
	
	/**
     * Load the VTimeZone for Outlook. ical4j use a static map to reuse the TimeZone objects, we need to load
     * and save our specialized TimeZone in a separate map.
     */
    private VTimeZone getOutlookVTimeZone(final String id) throws IOException, ParserException {
    	return outlookVTimeZones.computeIfAbsent(id, (timeZoneId) -> {
        	try {
				URL resource = ResourceLoader.getResource("zoneinfo-outlook/" + id + ".ics");
				CalendarBuilder builder = new CalendarBuilder();
				Calendar calendar = builder.build(resource.openStream());
				VTimeZone vTimeZone = (VTimeZone)calendar.getComponent(Component.VTIMEZONE);
				return vTimeZone;
			} catch (Exception e) {
				log.error("", e);
				return null;
			}
    	});
    }
}