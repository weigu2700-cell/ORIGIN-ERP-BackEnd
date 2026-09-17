package org.smart.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.smart.erp.inventory.entity.MaterialStock;

@Mapper
public interface MaterialStockMapper extends BaseMapper<MaterialStock> {

    /** 原子增加库存；依赖 inv_material_stock.uk_material_warehouse_id 处理首次并发入库。 */
    @Insert("INSERT INTO inv_material_stock "
            + "(id, warehouse_id, material_id, on_hand, reserved, version, create_time, update_time) "
            + "VALUES (#{stock.id}, #{stock.warehouseId}, #{stock.materialId}, #{stock.onHand}, "
            + "#{stock.reserved}, #{stock.version}, #{stock.createTime}, #{stock.updateTime}) "
            + "ON DUPLICATE KEY UPDATE "
            + "on_hand = on_hand + VALUES(on_hand), "
            + "update_time = VALUES(update_time), version = version + 1")
    int inboundAtomic(@Param("stock") MaterialStock stock);
}
