package com.futures.matching.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * Disruptor 事件工厂。
 * <p>
 * 使用 Objenesis 预分配事件对象，避免运行期内存分配，降低 GC 压力。
 * </p>
 */
public class OrderEventFactory implements EventFactory<OrderEvent> {

    @Override
    public OrderEvent newInstance() {
        return new OrderEvent();
    }
}
