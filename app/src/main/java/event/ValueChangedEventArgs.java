package event;

/**
 * a value changed event argument for Var<T>.valueChanged Event.
 *
 * @param <T> data type.
 */
public class ValueChangedEventArgs<T extends Object> extends INotificationEventArgs{
	public T originValue;
	public T newValue;
	/// reject the change?
	public boolean cancel = false;
	@SuppressWarnings("unchecked")
	public T getValue() {
		return (T)this.object[0]; 
	}
	public ValueChangedEventArgs(T value) {
		this.object = new Object[] {value};
		this.newValue=value;
	}
	public ValueChangedEventArgs(T value, T originalValue) {
		this.object = new Object[] {value,originalValue};
		this.newValue=value;
		this.originValue=originalValue;
	}
}
