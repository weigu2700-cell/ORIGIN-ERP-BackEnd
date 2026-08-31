package org.smart.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "org.smart.erp.system.mapper",
        "org.smart.erp.master.mapper",
        "org.smart.erp.inventory.mapper",
        "org.smart.erp.sales.mapper"
})
public class SmartErpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartErpServerApplication.class, args);
    }

}
