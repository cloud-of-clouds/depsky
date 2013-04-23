package depskys.clouds;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

/**
 *
 * @author koras
 */
public class CloudRepliesControlSet {

    /**
     * This semaphore has no permits, and its purpose is to lock a thread waiting for relies.
     * After, when n - f replies are received, the locked thread is unlocked.
     */
    public Semaphore waitReplies = new Semaphore(0);
    public CopyOnWriteArrayList<CloudReply> replies;
    public int sequence;

    public CloudRepliesControlSet(int n, int sequence) {
        this.sequence = sequence;
        this.replies = new CopyOnWriteArrayList<CloudReply>();
    }
}
