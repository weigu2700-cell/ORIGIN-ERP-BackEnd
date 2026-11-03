package org.smart.erp.im.attachment;

import org.apache.tika.Tika;
import org.smart.erp.im.ImException;
import org.smart.erp.im.ImProperties;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

@Component
public class AttachmentInspector {

    private final ImProperties properties;

    private final Tika tika = new Tika();

    private final java.util.concurrent.ScheduledExecutorService deadlines = java.util.concurrent.Executors
        .newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("im-scan-timeout").factory());

    private static final Map<String, Set<String>> TYPES = Map.ofEntries(Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")), Map.entry("png", Set.of("image/png")),
            Map.entry("webp", Set.of("image/webp")), Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("csv", Set.of("text/plain", "text/csv", "application/csv")),
            Map.entry("zip", Set.of("application/zip")));

    public AttachmentInspector(ImProperties properties) {
        this.properties = properties;
    }

    public void requireScanner() {
        if (properties.getScanner().getHost() == null || properties.getScanner().getHost().isBlank())
            throw new ImException(503, "STORAGE_UNAVAILABLE", "尚未配置附件扫描服务");
    }

    public String extension(String name) {
        if (name == null || name.isBlank() || name.length() > 255
                || name.chars().anyMatch(c -> c < 32 || c == 127 || c == '/' || c == '\\'))
            throw ImException.invalid("文件名不合法");
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!TYPES.containsKey(ext) || !properties.getAllowedExtensions().contains(ext))
            throw new ImException(415, "FILE_TYPE_NOT_ALLOWED", "不支持此文件类型");
        return ext;
    }

    public String inspect(InputStream source, String name, int expectedSize, String expectedHash) throws IOException {
        requireScanner();
        String ext = extension(name);
        Path file = Files.createTempFile("origin-im-scan-", ".tmp");
        try {
            var digest = sha256();
            int total = 0;
            try (var output = Files.newOutputStream(file); source) {
                byte[] bytes = new byte[65536];
                int length;
                while ((length = source.read(bytes)) != -1) {
                    total = Math.addExact(total, length);
                    if (total > expectedSize || total > properties.fileLimit())
                        throw rejected("SIZE_MISMATCH");
                    digest.update(bytes, 0, length);
                    output.write(bytes, 0, length);
                }
            }
            if (total != expectedSize || !HexFormat.of().formatHex(digest.digest()).equals(expectedHash))
                throw rejected("CHECKSUM_MISMATCH");
            String detected;
            // 不把临时文件的 .tmp 后缀当作 MIME 证据；让 Tika 按容器内容识别 OOXML。
            try (var input = org.apache.tika.io.TikaInputStream.get(file)) {
                detected = tika.getDetector().detect(input, new org.apache.tika.metadata.Metadata()).toString();
            }
            if (!TYPES.get(ext).contains(detected))
                throw rejected("TYPE_MISMATCH");
            if (Set.of("zip", "docx", "xlsx", "pptx").contains(ext))
                inspectZip(file);
            scan(file);
            return "csv".equals(ext) ? "text/csv" : detected;
        }
        finally {
            Files.deleteIfExists(file);
        }
    }

    private void inspectZip(Path path) throws IOException {
        long expanded = 0, start = System.nanoTime();
        int count = 0;
        try (var zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            byte[] bytes = new byte[65536];
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory())
                    continue;
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (++count > 1000 || name.contains("vbaproject")
                        || name.matches(".*\\.(exe|dll|com|bat|cmd|ps1|sh|js|vbs|jar|docm|xlsm|pptm|zip|7z|rar)$"))
                    throw rejected("UNSAFE_ARCHIVE");
                try (var input = new BufferedInputStream(zip.getInputStream(entry))) {
                    input.mark(8);
                    byte[] signature = input.readNBytes(4);
                    input.reset();
                    // V1拒绝嵌套压缩包，避免递归解压与加密内层绕过检查。
                    if (signature.length == 4 && signature[0] == 'P' && signature[1] == 'K' && signature[2] == 3
                            && signature[3] == 4)
                        throw rejected("NESTED_ARCHIVE");
                    int length;
                    while ((length = input.read(bytes)) != -1) {
                        expanded += length;
                        if (expanded > 100_000_000 || System.nanoTime() - start > 10_000_000_000L)
                            throw rejected("ARCHIVE_LIMIT");
                    }
                }
            }
        }
        catch (java.util.zip.ZipException e) {
            throw rejected("UNREADABLE_ARCHIVE");
        }
    }

    /** 采用 ClamAV 官方 INSTREAM 协议传字节；病毒识别和规则更新交给独立扫描服务。 */
    private void scan(Path file) throws IOException {
        var config = properties.getScanner();
        try (var socket = new Socket()) {
            var deadline = deadlines.schedule(() -> {
                try {
                    socket.close();
                }
                catch (IOException ignored) {
                }
            }, config.getTimeoutMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            try {
                socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getTimeoutMillis());
                socket.setSoTimeout(config.getTimeoutMillis());
                var output = new DataOutputStream(socket.getOutputStream());
                output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
                try (var input = Files.newInputStream(file)) {
                    byte[] bytes = new byte[65536];
                    int length;
                    while ((length = input.read(bytes)) != -1) {
                        output.writeInt(length);
                        output.write(bytes, 0, length);
                    }
                }
                output.writeInt(0);
                output.flush();
                var response = new ByteArrayOutputStream();
                int value;
                while (response.size() < 1024 && (value = socket.getInputStream().read()) != -1 && value != 0)
                    response.write(value);
                String result = response.toString(StandardCharsets.UTF_8);
                if (result.endsWith(" FOUND"))
                    throw rejected("MALWARE_DETECTED");
                if (!result.equals("stream: OK"))
                    throw new IOException("扫描未完成");
            }
            finally {
                deadline.cancel(false);
            }
        }
    }

    @jakarta.annotation.PreDestroy
    public void close() {
        deadlines.shutdownNow();
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ImException rejected(String reason) {
        return new ImException(415, reason, "附件校验未通过");
    }

}
