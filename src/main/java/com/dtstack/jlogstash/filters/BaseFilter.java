/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dtstack.jlogstash.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtstack.jlogstash.render.FreeMarkerRender;
import com.dtstack.jlogstash.render.TemplateRender;


/**
 * 
 * Reason: TODO ADD REASON(可选)
 * Date: 2016年8月31日 下午1:26:50
 * Company: www.dtstack.com
 * @author sishu.yss
 *
 */
public abstract class BaseFilter implements Cloneable, java.io.Serializable{

	private static final long serialVersionUID = -6525215605315577598L;

	private static final Logger logger = LoggerFactory.getLogger(BaseFilter.class);

	protected Map config;
	protected List<TemplateRender> IF;
	protected TemplateRender render;
	protected String tagOnFailure;
	protected ArrayList<String> removeFields;

	public BaseFilter(Map config) {
		this.config = config;

		if (this.config.containsKey("if")) {
			IF = new ArrayList<TemplateRender>();
			for (String c : (List<String>) this.config.get("if")) {
				try {
					IF.add(new FreeMarkerRender(c, c));
				} catch (IOException e) {
					logger.error(e.getMessage());
					System.exit(1);
				}
			}
		} else {
			IF = null;
		}

		if (config.containsKey("tagOnFailure")) {
			this.tagOnFailure = (String) config.get("tagOnFailure");
		} else {
			this.tagOnFailure = null;
		}
	}

	public abstract void prepare();

	public Map process(Map event) {
		if(event != null && event.size() > 0){
			boolean succuess = true;
			if (this.IF != null) {
				for (TemplateRender render : this.IF) {
					if (!render.render(event).equals("true")) {
						succuess = false;
						break;
					}
				}
			}
			if (succuess == true) {
				try{
					event = this.filter(event);
					this.postProcess(event,true);
				}catch(Exception e){
					this.postProcess(event,false);
				}
			}
		}
		return event;
	}

	protected abstract Map filter(Map event) ;

	public void postProcess(Map event, boolean ifsuccess) {
		if (ifsuccess == false) {
			if (this.tagOnFailure == null) {
				return;
			}
			if (!event.containsKey("tags")) {
				event.put("tags",
						new ArrayList<String>(Arrays.asList(this.tagOnFailure)));
			} else {
				Object tags = event.get("tags");
				if (tags.getClass() == ArrayList.class
						&& ((ArrayList) tags).indexOf(this.tagOnFailure) == -1) {
					((ArrayList) tags).add(this.tagOnFailure);
				}
			}
		}
	}
	
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
