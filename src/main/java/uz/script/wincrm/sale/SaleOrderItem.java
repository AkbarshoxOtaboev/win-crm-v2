package uz.script.wincrm.sale;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.clients.Client;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.goods.Goods;
import uz.script.wincrm.goods.GoodsCategory;
import uz.script.wincrm.users.User;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.warehouse.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Table(name = TableName.SALE_ORDER_ITEMS)
@SQLRestriction("status <> 'DELETED'")
public class SaleOrderItem extends FilialScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id")
    private SaleOrder saleOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id")
    private Goods goods;

    @Column(nullable = false)
    private BigDecimal priceCost;

    @Column(nullable = false)
    private BigDecimal priceSelling;

    @Column(precision = 38, scale = 4)
    private BigDecimal width;

    @Column(precision = 38, scale = 4)
    private BigDecimal height;

    @Column(nullable = false, precision = 38, scale = 4)
    private BigDecimal count;

    /** Buyurtmada tanlangan kesuv qalinligi, mm; null - sozlamadagi qiymat. */
    @Column(name = "cut_kerf_mm", precision = 6, scale = 2)
    private BigDecimal cutKerfMm;

    /** Bo'lakni 90° burish mumkinmi; null - mumkin. */
    @Column(name = "cut_allow_rotate")
    private Boolean cutAllowRotate;

    /** Foydali qoldiqning minimal tomoni, sm. Optimizatsiyada foydalanuvchi yozadi. */
    @Column(name = "cut_min_remnant_cm", precision = 8, scale = 2)
    private BigDecimal cutMinRemnantCm;

    /** Shu qatorni qaysi list turlaridan kesish mumkin. Vergul bilan: "12,15,18". */
    @Column(name = "cut_candidate_goods_ids", length = 1000)
    private String cutCandidateGoodsIds;

    /** Kesish kategoriyasi: optimizatsiya shu kategoriyadagi listlardan o‘lcham bo‘yicha tanlaydi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_category_id")
    private GoodsCategory cutCategory;

    @Column(nullable = false)
    private LocalDateTime arrivalDate;
}
