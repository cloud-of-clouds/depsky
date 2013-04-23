package depskys.clouds;

public interface ICloudDataManager {

    void processMetadata(CloudReply metadataReply);

    void checkDataIntegrity(CloudReply valuedataReply);

    void writeNewMetadata(CloudReply reply);
}
