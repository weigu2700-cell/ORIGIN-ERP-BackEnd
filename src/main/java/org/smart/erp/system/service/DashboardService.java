package org.smart.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.smart.erp.system.entity.Dashboard;

public interface DashboardService extends IService<Dashboard> {
    Dashboard getDashboard();
}
