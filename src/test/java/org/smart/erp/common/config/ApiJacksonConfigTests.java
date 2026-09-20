package org.smart.erp.common.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.smart.erp.common.result.Result;
import org.smart.erp.eip.dto.SystemNotificationPublishDTO;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.vo.UserDetailVo;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiJacksonConfigTests {

    @Test
    void serializesSnowflakeIdsAsStringsInUserListResponse() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(ApiJacksonConfig.class)
                .run(context -> {
                    JsonMapper mapper = context.getBean(JsonMapper.class);
                    UserDetailVo user = new UserDetailVo();
                    user.setId(2092121037575299074L);
                    user.setDeptId(2091921679337762818L);
                    user.setRoleIds(List.of(2092121037575299075L));
                    Page<UserDetailVo> page = new Page<>(1, 10);
                    page.setRecords(List.of(user));

                    String json = mapper.writeValueAsString(Result.success(page));

                    assertThat(json).contains("\"id\":\"2092121037575299074\"");
                    assertThat(json).contains("\"deptId\":\"2091921679337762818\"");
                    assertThat(json).contains("\"roleIds\":[\"2092121037575299075\"]");
                });
    }

    @Test
    void preservesHttpDateFormatAndAcceptsBothRequestFormats() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(ApiJacksonConfig.class)
                .run(context -> {
                    JsonMapper mapper = context.getBean(JsonMapper.class);
                    LocalDateTime expected = LocalDateTime.of(2026, 9, 20, 14, 30, 45);

                    assertThat(mapper.writeValueAsString(Map.of("time", expected)))
                            .contains("\"time\":\"2026-09-20 14:30:45\"");
                    assertThat(mapper.readValue("\"2026-09-20 14:30:45\"", LocalDateTime.class))
                            .isEqualTo(expected);
                    assertThat(mapper.readValue("\"2026-09-20T14:30:45\"", LocalDateTime.class))
                            .isEqualTo(expected);
                });
    }

    @Test
    void deserializesLombokBuilderDtoWithJackson3() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(ApiJacksonConfig.class)
                .run(context -> {
                    JsonMapper mapper = context.getBean(JsonMapper.class);
                    SystemNotificationPublishDTO dto = mapper.readValue(
                            "{\"templateCode\":\"notice\"}", SystemNotificationPublishDTO.class);

                    assertThat(dto.getTemplateCode()).isEqualTo("notice");
                    assertThat(mapper.writeValueAsString(UserStatus.NORMAL)).isEqualTo("1");
                });
    }
}
