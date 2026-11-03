package org.smart.erp.im.attachment;

import jakarta.annotation.PreDestroy;
import org.smart.erp.im.ImException;
import org.smart.erp.im.ImProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.*;

@Component
public class S3AttachmentStorage implements AttachmentStorage {

    private final ImProperties properties;

    private volatile S3Client client;

    private S3Presigner presigner;

    public S3AttachmentStorage(ImProperties properties) {
        this.properties = properties;
    }

    private synchronized void initialize() {
        try {
            configure();
        }
        catch (ImException e) {
            throw e;
        }
        catch (RuntimeException e) {
            if (client == null && presigner != null) {
                presigner.close();
                presigner = null;
            }
            throw unavailable();
        }
    }

    private void configure() {
        if (client != null)
            return;
        var p = properties.getStorage();
        if (!p.isEnabled() || p.getBucket() == null || p.getBucket().isBlank())
            throw unavailable();
        AwsCredentialsProvider credentials = p.getAccessKey() == null || p.getAccessKey().isBlank()
                ? DefaultCredentialsProvider.builder().build()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(p.getAccessKey(), p.getSecretKey()));
        var service = S3Configuration.builder().pathStyleAccessEnabled(p.isPathStyle()).build();
        var builder = S3Client.builder()
            .region(Region.of(p.getRegion()))
            .credentialsProvider(credentials)
            .serviceConfiguration(service)
            .httpClientBuilder(UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(5))
                .socketTimeout(Duration.ofSeconds(30)))
            .overrideConfiguration(
                    c -> c.apiCallTimeout(Duration.ofSeconds(40)).apiCallAttemptTimeout(Duration.ofSeconds(35)));
        var signer = S3Presigner.builder()
            .region(Region.of(p.getRegion()))
            .credentialsProvider(credentials)
            .serviceConfiguration(service);
        if (p.getEndpoint() != null && !p.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(p.getEndpoint()));
            signer.endpointOverride(URI.create(p.getEndpoint()));
        }
        presigner = signer.build();
        client = builder.build();
    }

    private String bucket() {
        return properties.getStorage().getBucket();
    }

    @Override
    public SignedPut sign(String key, int size, String mime, String sha256, java.time.Instant deadline) {
        initialize();
        try {
            if (client.getBucketVersioning(b -> b.bucket(bucket())).status() != BucketVersioningStatus.ENABLED)
                throw unavailable();
            Duration ttl = Duration.between(java.time.Instant.now(), deadline);
            if (ttl.isNegative() || ttl.isZero())
                throw new ImException(410, "UPLOAD_EXPIRED", "上传预约已过期");
            var request = PutObjectRequest.builder()
                .bucket(bucket())
                .key(key)
                .contentLength((long) size)
                .contentType(mime)
                .checksumSHA256(Base64.getEncoder().encodeToString(HexFormat.of().parseHex(sha256)))
                .build();
            var signed = presigner.presignPutObject(
                    PutObjectPresignRequest.builder().signatureDuration(ttl).putObjectRequest(request).build());
            var headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
            signed.signedHeaders().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("host"))
                    headers.put(name, String.join(",", values));
            });
            // 长度必须进入签名；否则 PUT 不能在入站阶段约束体积。
            if (!headers.containsKey("content-length"))
                throw unavailable();
            return new SignedPut(signed.url().toString(), headers, signed.expiration());
        }
        catch (software.amazon.awssdk.core.exception.SdkException e) {
            throw unavailable();
        }
    }

    @Override
    public Head head(String key) {
        initialize();
        try {
            var head = client.headObject(b -> b.bucket(bucket()).key(key));
            if (head.versionId() == null || "null".equals(head.versionId()))
                throw unavailable();
            if (head.contentLength() > properties.fileLimit())
                throw new ImException(413, "FILE_TOO_LARGE", "文件超过允许大小");
            return new Head(head.versionId(), Math.toIntExact(head.contentLength()));
        }
        catch (NoSuchKeyException e) {
            throw ImException.conflict("UPLOAD_NOT_COMPLETE", "文件尚未上传完成");
        }
        catch (S3Exception e) {
            if (e.statusCode() == 404)
                throw ImException.conflict("UPLOAD_NOT_COMPLETE", "文件尚未上传完成");
            throw unavailable();
        }
        catch (software.amazon.awssdk.core.exception.SdkException e) {
            throw unavailable();
        }
    }

    @Override
    public InputStream open(String key, String version, String range) {
        initialize();
        try {
            return client.getObject(
                    GetObjectRequest.builder().bucket(bucket()).key(key).versionId(version).range(range).build());
        }
        catch (software.amazon.awssdk.core.exception.SdkException e) {
            throw unavailable();
        }
    }

    @Override
    public void cleanup(String key, String keepVersion) {
        initialize();
        // SDK 负责分页，不用只列最新版本的 ListObjects，避免遗漏旧PUT产生的版本。
        for (var page : client.listObjectVersionsPaginator(b -> b.bucket(bucket()).prefix(key))) {
            for (var version : page.versions())
                if (key.equals(version.key()) && !Objects.equals(keepVersion, version.versionId()))
                    client.deleteObject(b -> b.bucket(bucket()).key(key).versionId(version.versionId()));
            for (var marker : page.deleteMarkers())
                if (key.equals(marker.key()))
                    client.deleteObject(b -> b.bucket(bucket()).key(key).versionId(marker.versionId()));
        }
    }

    private ImException unavailable() {
        return new ImException(503, "STORAGE_UNAVAILABLE", "附件存储不可用或未启用版本控制");
    }

    @PreDestroy
    public void close() {
        if (client != null)
            client.close();
        if (presigner != null)
            presigner.close();
    }

}
