package uz.script.wincrm.transport.mapper;

import org.springframework.stereotype.Component;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrder;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.transport.TransportDelivery;
import uz.script.wincrm.transport.TransportDriver;
import uz.script.wincrm.transport.TransportWorker;
import uz.script.wincrm.transport.TransportWorkerSalary;
import uz.script.wincrm.transport.response.DeliveryResponse;
import uz.script.wincrm.transport.response.TransportDriverResponse;
import uz.script.wincrm.transport.response.TransportWorkerResponse;
import uz.script.wincrm.transport.response.WorkerSalaryResponse;
import uz.script.wincrm.users.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class TransportMapper {

    public TransportDriverResponse toResponse(TransportDriver driver) {
        return TransportDriverResponse.builder()
                .id(driver.getId())
                .fullName(driver.getFullName())
                .phone(driver.getPhone())
                .carModel(driver.getCarModel())
                .carNumber(driver.getCarNumber())
                .maxWeightKg(driver.getMaxWeightKg())
                .note(driver.getNote())
                .status(driver.getStatus())
                .createdAt(driver.getCreatedAt())
                .build();
    }

    public TransportWorkerResponse toResponse(TransportWorker worker) {
        return TransportWorkerResponse.builder()
                .id(worker.getId())
                .fullName(worker.getFullName())
                .phone(worker.getPhone())
                .note(worker.getNote())
                .status(worker.getStatus())
                .createdAt(worker.getCreatedAt())
                .build();
    }

    public DeliveryResponse toResponse(TransportDelivery delivery) {
        SaleOrder order = delivery.getSaleOrder();
        Client client = order != null ? order.getClient() : null;
        TransportDriver driver = delivery.getDriver();

        List<DeliveryResponse.WorkerRef> workers = delivery.getWorkers() == null ? List.of()
                : delivery.getWorkers().stream()
                .sorted(Comparator.comparing(TransportWorker::getId))
                .map(w -> DeliveryResponse.WorkerRef.builder()
                        .id(w.getId())
                        .fullName(w.getFullName())
                        .phone(w.getPhone())
                        .build())
                .toList();

        List<DeliveryResponse.Item> items = order == null || order.getSaleOrderItems() == null ? List.of()
                : order.getSaleOrderItems().stream()
                .sorted(Comparator.comparing(SaleOrderItem::getId))
                .map(this::toItem)
                .toList();

        BigDecimal orderWeightKg = items.stream()
                .map(DeliveryResponse.Item::getWeightKg)
                .filter(w -> w != null && w.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DeliveryResponse.builder()
                .id(delivery.getId())
                .deliveryStatus(delivery.getDeliveryStatus())
                .saleOrderId(order != null ? order.getId() : null)
                .saleOrderStatus(order != null ? order.getSalesOrderStatus() : null)
                .orderDate(order != null ? order.getOrderDate() : null)
                .plannedDeliveryDate(order != null ? order.getPlannedDeliveryDate() : null)
                .orderTotalSum(order != null ? order.getTotalSum() : null)
                .orderWeightKg(orderWeightKg.signum() > 0 ? orderWeightKg : null)
                .sellerFullName(order != null ? userName(order.getUser()) : null)
                .orderComment(order != null ? order.getComment() : null)
                .clientId(client != null ? client.getId() : null)
                .clientFullName(client != null ? client.getFullName() : null)
                .clientPhone(client != null ? client.getPhone() : null)
                .address(delivery.getAddress())
                .driverId(driver != null ? driver.getId() : null)
                .driverFullName(driver != null ? driver.getFullName() : null)
                .driverPhone(driver != null ? driver.getPhone() : null)
                .carModel(driver != null ? driver.getCarModel() : null)
                .carNumber(driver != null ? driver.getCarNumber() : null)
                .driverMaxWeightKg(driver != null ? driver.getMaxWeightKg() : null)
                .workers(workers)
                .note(delivery.getNote())
                .sentAt(delivery.getSentAt())
                .acceptedAt(delivery.getAcceptedAt())
                .acceptedByName(userName(delivery.getAcceptedBy()))
                .departedAt(delivery.getDepartedAt())
                .arrivedAt(delivery.getArrivedAt())
                .confirmedAt(delivery.getConfirmedAt())
                .confirmedByName(userName(delivery.getConfirmedBy()))
                .cancelledAt(delivery.getCancelledAt())
                .salaryPercent(delivery.getSalaryPercent())
                .salaryTotal(delivery.getSalaryTotal())
                .items(items)
                .build();
    }

    public WorkerSalaryResponse toResponse(TransportWorkerSalary salary) {
        TransportWorker worker = salary.getWorker();
        TransportDelivery delivery = salary.getDelivery();
        SaleOrder order = delivery != null ? delivery.getSaleOrder() : null;
        Client client = order != null ? order.getClient() : null;
        return WorkerSalaryResponse.builder()
                .id(salary.getId())
                .deliveryId(delivery != null ? delivery.getId() : null)
                .saleOrderId(salary.getSaleOrderId())
                .clientFullName(client != null ? client.getFullName() : null)
                .workerId(worker != null ? worker.getId() : null)
                .workerFullName(worker != null ? worker.getFullName() : null)
                .orderTotalSnapshot(salary.getOrderTotalSnapshot())
                .sourceCurrency(salary.getSourceCurrency())
                .sourceAmount(salary.getSourceAmount())
                .exchangeRate(salary.getExchangeRate())
                .percentSnapshot(salary.getPercentSnapshot())
                .workersCount(salary.getWorkersCount())
                .amount(salary.getAmount())
                .salaryStatus(salary.getSalaryStatus())
                .earnedAt(salary.getEarnedAt())
                .periodYear(salary.getPeriodYear())
                .periodMonth(salary.getPeriodMonth())
                .decidedAt(salary.getDecidedAt())
                .decidedByName(userName(salary.getDecidedBy()))
                .comment(salary.getComment())
                .build();
    }

    private DeliveryResponse.Item toItem(SaleOrderItem item) {
        Goods goods = item.getGoods();
        return DeliveryResponse.Item.builder()
                .id(item.getId())
                .goodsName(goods != null ? goods.getName() : null)
                .goodsType(goods != null && goods.getType() != null ? goods.getType().name() : null)
                .count(item.getCount())
                .width(item.getWidth())
                .height(item.getHeight())
                .weightKg(itemWeightKg(item))
                .build();
    }

    /** WINDOW: vazn = kv.m (count) × 1 m² vazni */
    public BigDecimal orderWeightKg(SaleOrder order) {
        if (order == null || order.getSaleOrderItems() == null) {
            return BigDecimal.ZERO;
        }
        return order.getSaleOrderItems().stream()
                .map(this::itemWeightKg)
                .filter(w -> w != null && w.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal itemWeightKg(SaleOrderItem item) {
        Goods goods = item.getGoods();
        if (goods == null || goods.getType() != Type.WINDOW) {
            return null;
        }
        BigDecimal kvm = item.getCount();
        BigDecimal per = goods.getWeightPerSqm();
        if (kvm == null || per == null || kvm.signum() <= 0 || per.signum() <= 0) {
            return null;
        }
        return kvm.multiply(per).setScale(2, RoundingMode.HALF_UP);
    }

    private String userName(User user) {
        if (user == null) {
            return null;
        }
        return user.getFullName() != null ? user.getFullName() : user.getUsername();
    }
}
