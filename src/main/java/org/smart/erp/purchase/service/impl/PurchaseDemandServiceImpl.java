package org.smart.erp.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.purchase.dto.PurchaseDemandAddDto;
import org.smart.erp.purchase.dto.PurchaseDemandPageDto;
import org.smart.erp.purchase.entity.PurchaseDemand;
import org.smart.erp.purchase.enums.PurchaseDemandStatus;
import org.smart.erp.purchase.mapper.PurchaseDemandMapper;
import org.smart.erp.purchase.service.PurchaseDemandService;
import org.smart.erp.purchase.vo.PurchaseDemandVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class PurchaseDemandServiceImpl
        extends ServiceImpl<PurchaseDemandMapper, PurchaseDemand>
        implements PurchaseDemandService {

    private final PurchaseDemandMapper purchaseDemandMapper;
    private final BusinessNoGenerator businessNoGenerator;

    public PurchaseDemandServiceImpl(
            PurchaseDemandMapper purchaseDemandMapper,
            BusinessNoGenerator businessNoGenerator
    )
    {
        this.purchaseDemandMapper = purchaseDemandMapper;
        this.businessNoGenerator = businessNoGenerator;
    }

    /**
     * 检查参数是否为空，为空则抛出业务异常
     * @param value 参数值
     * @param message 异常消息
     * @param <T> 参数类型
     */
    private static <T> void require(T value, String message) {
        Optional.ofNullable(value).orElseThrow(() -> new BusinessException(400, message));
    }

    /**
     * 获取采购需求，如果不存在则抛出业务异常
     * @param id 采购需求ID
     * @return PurchaseDemand 采购需求对象
     */
    private PurchaseDemand getDemandOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException(400, "采购需求ID不能为空");
        }
        PurchaseDemand demand = purchaseDemandMapper.selectById(id);
        if (demand == null) {
            throw new BusinessException(404, "采购需求不存在");
        }
        return demand;
    }
    /**
     * 将采购需求转换为VO对象
     * @param demands 采购需求列表
     * @return 转换后的VO对象列表
     */
    private List<PurchaseDemandVo> toVoList(List<PurchaseDemand> demands) {
        return demands.stream().map(d -> {
            PurchaseDemandVo vo = new PurchaseDemandVo();
            BeanUtils.copyProperties(d, vo);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseDemand addPurchaseDemand(PurchaseDemandAddDto dto) {
        require(dto.getMaterialId(), "物料ID不能为空");
        require(dto.getPurchaseQuantity(), "采购数量不能为空");
        require(dto.getSourceNo(), "来源单号不能为空");
        require(dto.getSourceType(), "来源类型不能为空");

        PurchaseDemand purchaseDemand = new PurchaseDemand();
        BeanUtils.copyProperties(dto, purchaseDemand);
        purchaseDemand.setPurchaseDemandNo(
                businessNoGenerator.generateNo(
                        "erp:sequence:purchase-demand:",
                        "PR"
                )
        );
        purchaseDemand.setStatus(PurchaseDemandStatus.DRAFT);
        purchaseDemandMapper.insert(purchaseDemand);
        return purchaseDemand;
    }

    @Override
    public Page<PurchaseDemandVo> pagePurchaseDemand(PurchaseDemandPageDto dto) {
        int pageNum = dto.getPageNum() == null ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null ? 10 : dto.getPageSize();

        LambdaQueryWrapper<PurchaseDemand> qw = new LambdaQueryWrapper<PurchaseDemand>()
                .eq(dto.getMaterialId() != null, PurchaseDemand::getMaterialId, dto.getMaterialId())
                .eq(dto.getSourceType() != null, PurchaseDemand::getSourceType, dto.getSourceType())
                .eq(dto.getStatus() != null, PurchaseDemand::getStatus, dto.getStatus())
                .like(StringUtils.hasText(dto.getSourceNo()), PurchaseDemand::getSourceNo, dto.getSourceNo())
                .orderByDesc(PurchaseDemand::getCreateTime);

        Page<PurchaseDemand> page = purchaseDemandMapper.selectPage(new Page<>(pageNum, pageSize), qw);

        Page<PurchaseDemandVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVoList(page.getRecords()));
        return voPage;
    }

    @Override
    public PurchaseDemandVo detailPurchaseDemand(Long id) {
        PurchaseDemand demand = getDemandOrThrow(id);
        return toVoList(List.of(demand)).getFirst();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePurchaseDemand(Long id) {
        PurchaseDemand demand = getDemandOrThrow(id);
        if (demand.getStatus() != PurchaseDemandStatus.DRAFT) {
            throw new BusinessException(400, "采购需求状态不为草稿，无法审批");
        }
        demand.setStatus(PurchaseDemandStatus.APPROVED);
        purchaseDemandMapper.updateById(demand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePurchaseDemand(Long id) {
        PurchaseDemand demand = getDemandOrThrow(id);
        if (demand.getStatus() == PurchaseDemandStatus.CLOSED) {
            return;
        }
        if (demand.getStatus() != PurchaseDemandStatus.APPROVED) {
            throw new BusinessException(400, "仅已审批采购需求可关闭");
        }
        demand.setStatus(PurchaseDemandStatus.CLOSED);
        purchaseDemandMapper.updateById(demand);
    }


}
