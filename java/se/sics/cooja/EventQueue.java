/*
 * Copyright (c) 2008, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * $Id: EventQueue.java,v 1.6 2009/05/26 13:36:36 fros4943 Exp $
 */

package se.sics.cooja;

import java.util.ArrayList;

/**
 * @author Joakim Eriksson (ported to COOJA by Fredrik Osterlind)
 */
public class EventQueue {

  private TimeEvent first;
  private int eventCount = 0;

  /* For scheduling events from outside simulation thread effectively */
  private boolean hasUnsortedEvents = false;
  private ArrayList<TimeEvent> unsortedEvents = new ArrayList<TimeEvent>();

  public EventQueue() {
  }

  private synchronized void sortEvents() {
    hasUnsortedEvents = false;

    for (TimeEvent e: unsortedEvents) {
      addEvent(e);
    }
    unsortedEvents.clear();
  }

  /**
   * May be called from outside simulation thread.
   *
   * @param event Event
   * @param time Time
   */
  public synchronized void addEventUnsorted(TimeEvent event, long time) {
    /* Make sure this event is not executed before being resorted (readded) */
    if (event.queue != null) {
      event.remove();
    }
    event.time = time;
    unsortedEvents.add(event);
    hasUnsortedEvents = true;
  }

  /**
   * Should only be called from simulation thread!
   *
   * @param event Event
   * @param time Time
   */
  public void addEvent(TimeEvent event, long time) {
    event.time = time;
    addEvent(event);
  }

  /**
   * Should only be called from simulation thread!
   *
   * @param event Event
   */
  public void addEvent(TimeEvent event) {
    if (event.queue != null) {
      removeFromQueue(event);
    }

    if (first == null) {
      first = event;
    } else {
      TimeEvent pos = first;
      TimeEvent lastPos = first;
      while (pos != null && pos.time <= event.time) {
        lastPos = pos;
        pos = pos.nextEvent;
      }
      // Here pos will be the first TE after event
      // and lastPos the first before
      if (pos == first) {
        // Before all other
        event.nextEvent = pos;
        first = event;
      } else {
        event.nextEvent = pos;
        lastPos.nextEvent = event;
      }
    }
    event.removed = false;
    event.queue = this;
    eventCount++;
  }

  /**
   * Should only be called from simulation thread!
   *
   * @param event Event
   * @return True if event was removed
   */
  private boolean removeFromQueue(TimeEvent event) {
    TimeEvent pos = first;
    TimeEvent lastPos = first;

    while (pos != null && pos != event) {
      lastPos = pos;
      pos = pos.nextEvent;
    }
    if (pos == null) {
      return false;
    }
    // pos == event!
    if (pos == first) {
      // remove it from first pos.
      first = pos.nextEvent;
    } else {
      // else link prev to next...
      lastPos.nextEvent = pos.nextEvent;
    }
    // unlink
    pos.nextEvent = null;

    event.queue = null;
    eventCount--;
    return true;
  }

  public void removeAll() {
    TimeEvent event = popFirst();
    while (event != null) {
      event = popFirst();
    }
  }

  /**
   * Should only be called from simulation thread!
   *
   * @return Event
   */
  public TimeEvent popFirst() {
    if (hasUnsortedEvents) {
      sortEvents();
    }

    TimeEvent tmp = first;
    if (tmp == null) {
      return null;
    }

    first = tmp.nextEvent;
    // Unlink.
    tmp.nextEvent = null;

    // No longer scheduled!
    tmp.queue = null;
    eventCount--;

    if (tmp.removed) {
      /* pop and return another event instead */
      return popFirst();
    }
    return tmp;
  }

  public TimeEvent peekFirst() {
    if (hasUnsortedEvents) {
      sortEvents();
    }

    return first;
  }

  public String toString() {
    return "EventQueue with " + (eventCount+unsortedEvents.size()) + " events";
  }
}
