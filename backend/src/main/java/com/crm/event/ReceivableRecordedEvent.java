package com.crm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 回款成功事件
 *
 * <p>财务录入回款记录后由 {@code ReceivableService.create()} 异步发送，
 * {@code ReceivableEventListener} 监听并联动更新回款计划 / 合同状态。</p>
 *
 * <p>采用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 模式：
 * receivable 事务先提交，事件监听器在事务外处理 plan/contract 状态，
 * 避免监听器异常回滚 receivable 写入。</p>
 */
@Getter
public class ReceivableRecordedEvent extends ApplicationEvent {

    /** 合同 ID */
    private final Long contractId;

    /** 回款记录 ID */
    private final Long receivableId;

    /** 计划外回款时为 null */
    private final Long planId;

    public ReceivableRecordedEvent(Object source, Long contractId, Long receivableId, Long planId) {
        super(source);
        this.contractId = contractId;
        this.receivableId = receivableId;
        this.planId = planId;
    }
}
