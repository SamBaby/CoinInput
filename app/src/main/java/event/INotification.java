package event;

/**
 * 
 * Interface to registry EventDelegate
 *
 * @param <T>
 */
public interface INotification<T extends INotificationEventArgs> {
    void perform(Object from, T args);
}