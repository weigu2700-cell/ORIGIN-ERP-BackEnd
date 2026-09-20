package org.smart.erp.ai.tool;

import lombok.RequiredArgsConstructor;
import org.smart.erp.ai.dto.toolResult.PageToolResult;
import org.smart.erp.eip.dto.NotificationPageDto;
import org.smart.erp.eip.service.NotificationInboxService;
import org.smart.erp.eip.vo.NotificationVo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationQueryTool {

    private final NotificationInboxService notificationInboxService;

    @Tool(name = "query_my_notifications", description = "分页查询当前登录用户的通知。可按是否已读筛选；只读操作，不接受用户ID，始终只返回当前用户的通知。")
    public PageToolResult<NotificationVo> queryMyNotifications(
            @ToolParam(required = false, description = "是否已读；true表示已读，false表示未读，不传表示全部") Boolean isRead,
            @ToolParam(required = false, description = "页码，从1开始，默认1") Integer pageNum,
            @ToolParam(required = false, description = "每页条数，默认10，最大100") Integer pageSize,
            ToolContext toolContext) {
        NotificationPageDto dto = new NotificationPageDto();
        dto.setIsRead(isRead);
        dto.setPageNum(ToolExecutionSupport.normalizePage(pageNum));
        dto.setPageSize(ToolExecutionSupport.normalizePageSize(pageSize));
        return ToolExecutionSupport.withSecurityContext(toolContext,
                () -> PageToolResult.from(notificationInboxService.page(dto)));
    }

}
