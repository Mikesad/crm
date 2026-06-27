package com.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 合同产品明细 (append-only,无 is_deleted)
 *
 * <p>字段说明见 {@code crm_full.sql} 中 {@code crm_contract_product} 表。</p>
 *
 * <p>折扣字段：9.5 折存 9.50；后端按 {@code sales_price = standard_price * discount / 10} 反推。</p>
 */
@Data
@TableName("crm_contract_product")
public class CrmContractProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 合同 ID */
    private Long contractId;

    /** 产品 ID */
    private Long productId;

    /** 数量 */
    private Integer count;

    /** 合同签订时产品的标准售价(快照,不受产品改价影响) */
    private BigDecimal standardPrice;

    /** 实际销售成交价(= standardPrice * discount / 10) */
    private BigDecimal salesPrice;

    /** 折扣:如 9.5 折记为 9.50 */
    private BigDecimal discount;
}
