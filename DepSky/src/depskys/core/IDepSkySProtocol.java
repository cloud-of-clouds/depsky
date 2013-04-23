package depskys.core;

import depskys.clouds.CloudReply;

/**
 * Interface for depskys read/write protocol
 * @author bruno
 */
public interface IDepSkySProtocol {

    int getClientId();

    byte[] read(DepSkySDataUnit reg) throws Exception;

    byte[] write(DepSkySDataUnit reg, byte[] value) throws Exception;

    void dataReceived(CloudReply reply);

    boolean sendingParallelRequests();
}
