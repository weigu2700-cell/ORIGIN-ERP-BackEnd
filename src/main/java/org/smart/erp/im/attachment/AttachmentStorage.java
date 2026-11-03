package org.smart.erp.im.attachment;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

public interface AttachmentStorage {

    record SignedPut(String url, Map<String, String> headers, Instant expiresAt) {
    }

    record Head(String version, int size) {
    }

    SignedPut sign(String key, int size, String mime, String sha256, Instant deadline);

    Head head(String key);

    InputStream open(String key, String version, String range);

    /** 仅保留消息引用的固定版本；keepVersion=null时删除该随机key的全部版本。 */
    void cleanup(String key, String keepVersion);

}
