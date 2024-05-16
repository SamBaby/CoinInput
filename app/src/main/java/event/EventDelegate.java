package event;

import java.util.Vector;

/**
 * EventDelegate for implementing observer pattern. event delegate provides add,
 * remove invoke, clear operations for registry customized event handlers.
 *
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public class EventDelegate<T extends INotification> {
    Vector<T> invocationList = new Vector<>();

	/**
	 * test whether no listener
	 * 
	 * @return true if listener is empty
	 */
    public synchronized boolean isEmpty() {
    	return invocationList.isEmpty();
    }
    
	/**
	 * get all invocation list. DO NOT DIRECTLY USE IT, USE addDelegate and invoke
	 * instead.
	 * 
	 * @return invocation list
	 */
    public synchronized  Vector<T> getInvocationList(){
        return invocationList;
    }

	/**
	 * register EventHandler to a event.
	 * 
	 * @param t EventHandler
	 * @return assigned event handler
	 */
    public synchronized T addDelegate(T t){
        invocationList.add(t);
        return t;
    }

	/**
	 * remove EventHandler from a event.
	 * 
	 * @param t event handler
	 * @return true if remove successfully.
	 */
    public synchronized boolean removeDelegate(T t){
        return invocationList.remove(t);
    }

	/**
	 * clear all EventHandler
	 */
    public synchronized void clear(){
		invocationList = new Vector<>();
    }

	/**
	 * broadcast/notify/trigger all registered EventHandler.
	 * 
	 * @param sender Sender.
	 * @param args   Argument for event.
	 */
	@SuppressWarnings("unchecked")
	public void invoke(Object sender, INotificationEventArgs args) {
        Vector<T> clone = getInvocationList();
        for(int i=0; i<clone.size(); ++i){
            T t = clone.get(i);
            t.perform(sender,args);
        }
    }
	/**
	 * broadcast/notify/trigger all registered EventHandler.
	 * 
	 * @param sender Sender.
	 * @param args   Arguments for event.
	 */
    public void invoke(Object sender,Object... args){
        INotificationEventArgs arglist = new INotificationEventArgs();
        arglist.object = args;
        this.invoke(sender,arglist);
    }
    public <T1> void invoke(Object sender, T1 arg1 ) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg1<T1>(arg1));
    }
    public <T1,T2> void invoke(Object sender, T1 arg1, T2 arg2 ) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg2<T1,T2>(arg1,arg2));
    }
    public <T1,T2,T3> void invoke(Object sender, T1 arg1, T2 arg2, T3 arg3 ) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg3<T1,T2,T3>(arg1,arg2,arg3));
    }
    public <T1,T2,T3,T4> void invoke(Object sender, T1 arg1, T2 arg2, T3 arg3, T4 arg4 ) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg4<T1,T2,T3,T4>(arg1,arg2,arg3,arg4));
    }
    public <T1,T2,T3,T4,T5> void invoke(Object sender, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5 ) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg5<T1,T2,T3,T4,T5>(arg1,arg2,arg3,arg4,arg5));
    }
    public <T1,T2,T3,T4,T5,T6> void invoke(Object sender, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5 , T6 arg6) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg6<T1,T2,T3,T4,T5,T6>(arg1,arg2,arg3,arg4,arg5,arg6));
    }
    public <T1,T2,T3,T4,T5,T6,T7> void invoke(Object sender, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5 , T6 arg6, T7 arg7) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg7<T1,T2,T3,T4,T5,T6,T7>(arg1,arg2,arg3,arg4,arg5,arg6,arg7));
    }
    public <T1,T2,T3,T4,T5,T6,T7,T8> void invoke(Object sender, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5 , T6 arg6, T7 arg7, T8 arg8) {
        this.invoke(sender, new INotificationEventArgs.INotificationEventArg8<T1,T2,T3,T4,T5,T6,T7,T8>(arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8));
    }
}