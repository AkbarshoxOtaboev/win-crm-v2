package uz.script.wincrm.goods;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;
import uz.script.wincrm.currency.Currency;
import uz.script.wincrm.goods.enums.Type;
import uz.script.wincrm.sale.SaleOrderItem;
import uz.script.wincrm.sale.SaleOrderWaste;
import uz.script.wincrm.stock.Stock;
import uz.script.wincrm.stock.StockHistory;
import uz.script.wincrm.filial.FilialScopedEntity;
import uz.script.wincrm.utils.TableName;
import uz.script.wincrm.warehouse.WarehouseOrderItem;

import java.math.BigDecimal;
import java.util.List;

@Entity
@SQLRestriction("status <> 'DELETED'")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = TableName.GOODS)
@Getter
@Setter
@SuperBuilder
public class Goods extends FilialScopedEntity {
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_group_id", nullable = false)
    private GoodsGroup goodsGroup;

    /** Kesishda list o‘rniga shu kategoriya tanlanadi. Ixtiyoriy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private GoodsCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_type_id", nullable = false)
    private UnitType unitType;

    @Enumerated(EnumType.STRING)
    private Type type;

    /** Oxirgi kirim narxi (so'mda); mahsulot yaratilganda 0, ombor kirimida yangilanadi. */
    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private BigDecimal priceCost = BigDecimal.ZERO;

    /** Oxirgi kirimda belgilangan sotish narxi (priceCurrency'da); mahsulot yaratilganda 0. */
    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private BigDecimal priceSelling = BigDecimal.ZERO;

    /** priceSelling valyutasi; priceCost doim so'mda (ombor tannarxi). */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_currency", nullable = false, length = 3)
    @ColumnDefault("'UZS'")
    @Builder.Default
    private Currency priceCurrency = Currency.UZS;


    private String barcode;

    /** Sotuvchi bera oladigan maksimal chegirma (%); null — cheklanmagan. */
    @Column(name = "max_discount_percent", precision = 5, scale = 2)
    private BigDecimal maxDiscountPercent;

    /**
     * Ombordagi minimal qoldiq (ixtiyoriy).
     * PRODUCT — o‘lchov birligida; WINDOW — kv.m da.
     * null — chegaralanmagan; jami qoldiq shu qiymatdan kam yoki teng bo‘lsa «kam qolgan» hisoblanadi.
     */
    @Column(name = "min_quantity", precision = 19, scale = 4)
    private BigDecimal minQuantity;

    /**
     * Oyna (WINDOW) uchun minimal qoldiq donada (ixtiyoriy).
     * null — dona bo‘yicha chegara yo‘q.
     */
    @Column(name = "min_piece_quantity", precision = 19, scale = 4)
    private BigDecimal minPieceQuantity;

    private String photo;

    /** Oyna (WINDOW) uchun default eni (santimetr) */
    private BigDecimal width;

    /** Oyna (WINDOW) uchun default bo‘yi (santimetr) */
    private BigDecimal height;

    /** Oyna (WINDOW) qalinligi (millimetr), masalan 4 / 6 / 8 */
    private BigDecimal thickness;

    /** Oyna (WINDOW) uchun 1 kv.m vazni (kg) */
    @Column(name = "weight_per_sqm")
    private BigDecimal weightPerSqm;

    @OneToMany(mappedBy = "goods")
    private List<WarehouseOrderItem> warehouseOrderItems;

    @OneToMany(mappedBy = "goods")
    private List<SaleOrderItem> saleOrderItems;

    /**
     * Ushbu material bo'yicha barcha buyurtmalarda qayd etilgan ortib qolgan (waste) yozuvlar.
     */
    @OneToMany(mappedBy = "goods")
    private List<SaleOrderWaste> saleOrderWastes;

    @OneToMany(mappedBy = "goods")
    private List<Stock> stocks;

    @OneToMany(mappedBy = "goods")
    private List<StockHistory> stockHistories;

}