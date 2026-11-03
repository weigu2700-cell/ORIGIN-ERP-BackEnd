package org.smart.erp.im;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("im")
@org.springframework.validation.annotation.Validated
public class ImProperties {

    private boolean enabled;

    @jakarta.validation.constraints.Min(3)
    @jakarta.validation.constraints.Max(200)
    private int maxGroupMembers = 200;

    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(50_000_000)
    private int maxFileBytes = 50_000_000;

    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(5)
    private int maxConnectionsPerUser = 5;

    @jakarta.validation.constraints.Min(1)
    private int maxPendingUploadsPerUser = 3;

    @jakarta.validation.constraints.Min(1)
    private int sendsPerSecond = 5;

    @jakarta.validation.constraints.Min(1)
    private long storageQuotaBytes = 20_000_000_000L;

    private List<String> allowedOrigins = List.of();

    @jakarta.validation.constraints.NotEmpty
    private List<@jakarta.validation.constraints.Pattern(
            regexp = "jpg|jpeg|png|webp|pdf|docx|xlsx|pptx|txt|csv|zip") String> allowedExtensions = List.of("jpg",
                    "jpeg", "png", "webp", "pdf", "docx", "xlsx", "pptx", "txt", "csv", "zip");

    private Storage storage = new Storage();

    @jakarta.validation.Valid
    private Scanner scanner = new Scanner();

    public int groupLimit() {
        return Math.clamp(maxGroupMembers, 3, 200);
    }

    public int fileLimit() {
        return Math.clamp(maxFileBytes, 1, 50_000_000);
    }

    @Getter
    @Setter
    public static class Storage {

        private boolean enabled;

        private String endpoint;

        private String region = "us-east-1";

        private String bucket;

        private String accessKey;

        private String secretKey;

        private boolean pathStyle = true;

    }

    @Getter
    @Setter
    public static class Scanner {

        private String host;

        @jakarta.validation.constraints.Min(1)
        @jakarta.validation.constraints.Max(65535)
        private int port = 3310;

        @jakarta.validation.constraints.Min(100)
        @jakarta.validation.constraints.Max(60000)
        private int timeoutMillis = 30_000;

    }

}
