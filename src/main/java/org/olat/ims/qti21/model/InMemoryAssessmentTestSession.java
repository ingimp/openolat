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
package org.olat.ims.qti21.model;

import java.math.BigDecimal;
import java.util.Date;

import org.olat.ims.qti21.AssessmentTestSession;

/**
 * 
 * Initial date: 12.02.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class InMemoryAssessmentTestSession implements AssessmentTestSession {

	private Long key;
	private Date creationDate;
	private Date lastModified;
    private String storage;
    private Date finishTime;
    private Date terminationTime;
    private Long duration;
    private Boolean passed;
    private BigDecimal score;
    private boolean exploded;
    
    public InMemoryAssessmentTestSession() {
    	key = -1l;
    	creationDate = new Date();
    	lastModified = creationDate;
    }

    @Override
	public Long getKey() {
		return key;
	}

    @Override
	public Date getCreationDate() {
		return creationDate;
	}

    @Override
	public Date getLastModified() {
		return lastModified;
	}

    @Override
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	@Override
	public boolean isExploded() {
		return exploded;
	}

	public void setExploded(boolean exploded) {
		this.exploded = exploded;
	}

	@Override
	public String getStorage() {
		return storage;
	}

	public void setStorage(String storage) {
		this.storage = storage;
	}

	@Override
	public Date getFinishTime() {
		return finishTime;
	}

	@Override
	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}

	@Override
	public Date getTerminationTime() {
		return terminationTime;
	}

	@Override
	public void setTerminationTime(Date terminationTime) {
		this.terminationTime = terminationTime;
	}

	@Override
	public Boolean getPassed() {
		return passed;
	}

	@Override
	public void setPassed(Boolean passed) {
		this.passed = passed;
	}

	@Override
	public BigDecimal getScore() {
		return score;
	}

	@Override
	public void setScore(BigDecimal score) {
		this.score = score;
	}

	@Override
	public Long getDuration() {
		return duration;
	}

	@Override
	public void setDuration(Long duration) {
		this.duration = duration;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return (this == obj);
	}
}