package ve.ucv.ciens.ccg.nxtcam.robotcontrol;

import java.util.LinkedList;
import java.util.Queue;

import ve.ucv.ciens.ccg.networkdata.MotorEvent;

/**
 * <p>A simple monitor class that encapsulates a queue.</p>
 * <p>As it name says it stores motor events to be forwarded to the NXT robot.</p>
 * <p>This class implements the singleton design pattern.<p> 
 * 
 * @author Miguel Angel Astor Romero
 */
public class MotorEventQueue {
	/**
	 * The event queue implemented as a linked list.
	 */
	private Queue<MotorEvent> motorEvents;

	private MotorEventQueue(){
		motorEvents = new LinkedList<MotorEvent>();
	}

	private static class SingletonHolder{
		public static final MotorEventQueue instance = new MotorEventQueue();
	}

	/**
	 * Return the singleton instance of this class.
	 * @return The singleton instance.
	 */
	public static MotorEventQueue getInstance(){
		return SingletonHolder.instance;
	}

	/**
	 * <p>Get the first event on the queue.</p>
	 * <p> If there are no events to return this method blocks until some thread calls the addEvent() method.</p>
	 * @return The event at the front of the queue.
	 */
	public synchronized MotorEvent getNextEvent(){
		while(motorEvents.size() == 0){
			try{ wait(); }catch(InterruptedException ie){ }
		}
		return motorEvents.poll();
	}

	/**
	 * <p>Adds an event to the back of the queue.</p>
	 * @param event The event to add.
	 */
	public synchronized void addEvent(MotorEvent event){
		motorEvents.add(event);
		notifyAll();
	}
}
