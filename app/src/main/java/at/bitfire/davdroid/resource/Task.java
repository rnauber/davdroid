/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.davdroid.resource;

import android.util.Log;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.PercentComplete;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.SimpleHostInfo;
import net.fortuna.ical4j.util.UidGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import at.bitfire.davdroid.Constants;
import at.bitfire.davdroid.syncadapter.DavSyncAdapter;
import lombok.Getter;
import lombok.Setter;

public class Task extends Resource {
	private final static String TAG = "davdroid.Task";

	@Getter @Setter DateTime createdAt;
	@Getter @Setter DateTime lastModified;

	@Getter @Setter	String summary, location, description, url;
	@Getter @Setter int priority;
	@Getter @Setter Clazz classification;
	@Getter @Setter Status status;

	@Getter @Setter DtStart dtStart;
	@Getter @Setter Due due;
	@Getter @Setter Duration duration;
	@Getter @Setter Completed completedAt;
	@Getter @Setter Integer percentComplete;


	public Task(String name, String ETag) {
		super(name, ETag);
	}

	public Task(long localId, String name, String ETag)
	{
		super(localId, name, ETag);
	}

	@Override
	public void initialize() {
		UidGenerator generator = new UidGenerator(new SimpleHostInfo(DavSyncAdapter.getAndroidID()), String.valueOf(android.os.Process.myPid()));
		uid = generator.generateUid().getValue();
		name = uid + ".ics";
	}


	@Override
	public void parseEntity(InputStream entity, AssetDownloader downloader) throws IOException, InvalidResourceException {
		net.fortuna.ical4j.model.Calendar ical;
		try {
			CalendarBuilder builder = new CalendarBuilder();
			ical = builder.build(entity);

			if (ical == null)
				throw new InvalidResourceException("No iCalendar found");
		} catch (ParserException e) {
			throw new InvalidResourceException(e);
		}

		ComponentList notes = ical.getComponents(Component.VTODO);
		if (notes == null || notes.isEmpty())
			throw new InvalidResourceException("No VTODO found");
		VToDo todo = (VToDo)notes.get(0);

		if (todo.getUid() != null)
			uid = todo.getUid().getValue();

		if (todo.getCreated() != null)
			createdAt = todo.getCreated().getDateTime();
		if (todo.getLastModified() != null)
			lastModified = todo.getLastModified().getDateTime();

		if (todo.getSummary() != null)
			summary = todo.getSummary().getValue();
		if (todo.getLocation() != null)
			location = todo.getLocation().getValue();
		if (todo.getDescription() != null)
			description = todo.getDescription().getValue();
		if (todo.getUrl() != null)
			url = todo.getUrl().getValue();

		priority = (todo.getPriority() != null) ? todo.getPriority().getLevel() : 0;
		if (todo.getClassification() != null)
			classification = todo.getClassification();
		if (todo.getStatus() != null)
			status = todo.getStatus();

		if (todo.getDue() != null)
			due = todo.getDue();
		if (todo.getDuration() != null)
			duration = todo.getDuration();
		if (todo.getStartDate() != null)
			dtStart = todo.getStartDate();
		if (todo.getDateCompleted() != null)
			completedAt = todo.getDateCompleted();
		if (todo.getPercentComplete() != null)
			percentComplete = todo.getPercentComplete().getPercentage();
	}


	@Override
	public String getMimeType() {
		return "text/calendar";
	}

	@Override
	public ByteArrayOutputStream toEntity() throws IOException {
		final net.fortuna.ical4j.model.Calendar ical = new net.fortuna.ical4j.model.Calendar();
		ical.getProperties().add(Version.VERSION_2_0);
		ical.getProperties().add(Constants.ICAL_PRODID);

		final VToDo todo = new VToDo();
		ical.getComponents().add(todo);
		final PropertyList props = todo.getProperties();

		if (uid != null)
			props.add(new Uid(uid));

		if (createdAt != null)
			props.add(new Created(createdAt));
		if (lastModified != null)
			props.add(new LastModified(lastModified));

		if (summary != null)
			props.add(new Summary(summary));
		if (location != null)
			props.add(new Location(location));
		if (description != null)
			props.add(new Description(description));
		if (url != null)
			try {
				props.add(new Url(new URI(url)));
			} catch (URISyntaxException e) {
				Log.e(TAG, "Ignoring invalid task URL: " + url, e);
			}
		if (priority != 0)
			props.add(new Priority(priority));
		if (classification != null)
			props.add(classification);
		if (status != null)
			props.add(status);

		if (due != null)
			props.add(due);
		if (duration != null)
			props.add(duration);
		if (dtStart != null)
			props.add(dtStart);
		if (completedAt != null)
			props.add(completedAt);
		if (percentComplete != null)
			props.add(new PercentComplete(percentComplete));

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
