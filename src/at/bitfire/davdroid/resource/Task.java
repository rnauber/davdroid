/*******************************************************************************
 * Copyright (c) 2014 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package at.bitfire.davdroid.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;
import lombok.Setter;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.SimpleHostInfo;
import net.fortuna.ical4j.util.UidGenerator;
import android.util.Log;
import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.syncadapter.DavSyncAdapter;


/**
 * Represents a task. Locally, this is a task in the Mirakel
 * content provider; remote, this is a VTODO entry in a CalDAV calendar.
 */
public class Task extends Resource {
	private static final String TAG = "davdroid.Task";
	
	public final static String MIME_TYPE = "text/calendar";
	
	@Getter @Setter private String title, description;
	
	
	public Task(String name, String ETag) {
		super(name, ETag);
	}
	
	public Task(long localID, String name, String ETag) {
		super(localID, name, ETag);
	}

	
	@Override
	public void initialize() {
		generateUID();
		name = uid.replace("@", "_") + ".ics";
	}
	
	protected void generateUID() {
		UidGenerator generator = new UidGenerator(new SimpleHostInfo(DavSyncAdapter.getAndroidID()), String.valueOf(android.os.Process.myPid()));
		uid = generator.generateUid().getValue();
	}


	@Override
	public void parseEntity(InputStream entity) throws IOException, InvalidResourceException {
		net.fortuna.ical4j.model.Calendar ical;
		try {
			CalendarBuilder builder = new CalendarBuilder();
			ical = builder.build(entity);

			if (ical == null)
				throw new InvalidResourceException("No iCalendar found");
		} catch (ParserException e) {
			throw new InvalidResourceException(e);
		}
		
		// we're only interested in VTODOs
		ComponentList todos = ical.getComponents(Component.VTODO);
		if (todos == null || todos.isEmpty())
			throw new InvalidResourceException("No VTODO found");
		VToDo todo = (VToDo)todos.get(0);
		
		if (todo.getUid() != null)
			uid = todo.getUid().getValue();
		else {
			Log.w(TAG, "Received VTODO without UID, generating new one");
			generateUID();
		}
		
		if (todo.getSummary() != null)
			title = todo.getSummary().getValue();
		if (todo.getDescription() != null)
			description = todo.getDescription().getValue();
	}

	@Override
	public ByteArrayOutputStream toEntity() throws IOException {
		net.fortuna.ical4j.model.Calendar ical = new net.fortuna.ical4j.model.Calendar();
		ical.getProperties().add(Version.VERSION_2_0);
		ical.getProperties().add(new ProdId("-//bitfire web engineering//DAVdroid " + Constants.APP_VERSION + "//EN"));
		
		VToDo todo = new VToDo();
		PropertyList props = todo.getProperties();
		
		if (uid != null)
			props.add(new Uid(uid));
		
		if (title != null)
			props.add(new Summary(title));
		if (description != null)
			props.add(new Description(description));

		CalendarOutputter output = new CalendarOutputter(false);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			output.output(ical, os);
		} catch (ValidationException e) {
			Log.e(TAG, "Generated invalid iCalendar");
		}
		return os;
	}
}
