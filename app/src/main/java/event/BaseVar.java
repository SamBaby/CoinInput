package event;

/**
 * base var type which provide raw event handler for chaining every changes.
 * this class is aimed at providing non-generic template to notify/invoke event
 * handler within generic templates at once.
 */
public class BaseVar {
	public boolean hasValue;
	public boolean changed;
	public boolean notifyOnlyOnChanged;

	@SuppressWarnings("rawtypes")
	public EventDelegate<INotification> onChanged = new EventDelegate<>();
	public void notifyChanged() {
		onChanged.invoke(this, this);
	}
}
